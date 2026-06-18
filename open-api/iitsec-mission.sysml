package MissionArchitecture {
  import ScalarValues::*;

  metadata def Map {
    attribute key;
    attribute value;
  }

  metadata def List {
    attribute of;
  }

  metadata def DevsAtomicModel;
  metadata def DevsCoupledModel;
  metadata def Imported;

  package SimDomain {
    item def Location {
      attribute latitude: Real;
      attribute longitude: Real;
      attribute altitude: Real;
    }

    item def Velocity {
      attribute speedKph: Real;
      attribute headingDegrees: Real;
      attribute pitchDegrees: Real;
    }

    item def DegreesOrientation {
      attribute headingDegrees: Real;
      attribute pitchDegrees: Real;
      attribute rollDegrees: Real;
    }

    item def Message {
      attribute senderId: String;
      attribute receiverId: String;
      attribute networkId: String;
    }

    enum def TaskStatus {
      AWAITING,
      EXECUTING,
      COMPLETE,
      CANCELLED
    }

    item def Task specializes Message {
      attribute uid: String;
      attribute assignedSystemId: String;
      attribute taskStatus: TaskStatus;
    }

    item def PrimitiveAction {
      attribute status: TaskStatus;
    }

    item def LocatedAction specializes PrimitiveAction {
      attribute location: Location[0..*];
    }

    item def AirReconTask specializes Task {
      attribute reconObjective: Location[0..*];
      attribute ingressRoute: LocatedAction;
      attribute egressRoute: LocatedAction;
      attribute orbitRoute: LocatedAction;
    }

    item def MoveTask specializes Task {
      attribute route: LocatedAction;
    }

    item def SystemStatus {
      attribute systemId: String;
      attribute name: String;
      attribute fullName: String;
      attribute milStd2525Symbol: String;
      attribute location: Location;
      attribute timeSeconds: Integer;
    }

    item def AutonomousSystemReport specializes SystemStatus {
      attribute batteryPercent: Real;
    }

    item def CommonOperationalPicture {
      attribute systemId: String;
      attribute knownEntities: SystemStatus[0..*] #Map(key = String, value = SystemStatus);
    }

    enum def BatteryOperatingState {
      NORMAL,
      OVERCURRENT,
      OVERTEMP,
      COLD_DERATE,
      LOW,
      CRITICAL
    }

    item def BatteryStatus {
      attribute chargePercent: Real;
      attribute availablePowerWatts: Real;
      attribute batteryState: BatteryOperatingState;
      attribute energyRemainingWattHours: Real;
      attribute estimatedTempC: Real;
      attribute capacityDerateFactor: Real;
      attribute thermalWarning: Boolean;
    }

    item def EnvironmentConditions {
      attribute airTemperatureC: Real;
    }
    
    item def PowerRequest {
      attribute requestedPowerWatts: Real;
      attribute airTemperatureC: Real;
    }

    enum def UavOperatingMode {
      OFF,
      TAKEOFF,
      CLIMB,
      CRUISE,
      LOITER,
      DESCENT,
      LAND,
      DESTROYED
    }

    enum def BatteryPackConfig {
      S3,
      S4,
      S6
    }

    enum def BatteryPackConfig {
      S3,
      S4,
      S6
    }

    package InformationDomain {
      item def RadioType {
        attribute name: String;
        attribute powerWatts: Real;
        attribute operatingFrequency: Real;
        attribute bandwidthHz: Real;
        attribute antennaGainDb: Real;
        attribute receiverSensitivityDbm: Real;
      }

      item def JammingActivity {
        attribute jammerLocation: Location;
        attribute jammerPower: Real;
        attribute jammerFrequency: Real;
      }

      // Registry entry describing a participant's radio and last-known state
      item def RadioInfo {
        attribute systemId: String;
        attribute radio: RadioType;
        attribute lastKnownLocation: Location;
        attribute lastSeenTimeSeconds: Integer;
      }

      // Observation of pairwise radio connectivity
      item def RadioConnectivity {
        attribute senderId: String;
        attribute receiverId: String;
        attribute linkMarginDbm: Real;
        attribute isConnected: Boolean;
        attribute timeSeconds: Integer;
      }
    }

  }

  #Imported
  package cloud::simlytics::c2sim {
    type McaC2SimClient;
    type C2SimScenario;
  }


  package Sim {
    package MissionModel {
      import MissionArchitecture::SimDomain::*;
      import MissionArchitecture::Sim::MissionModel::MissionPackage::*;
      import MissionArchitecture::Sim::MissionModel::InformationEnvironment::*;

      #DevsCoupledModel
      part def MissionModel {
        in port messageIntoMissionModel: Message;
        out port reportsFromMissionModel: SystemStatus;

        public part missionPackage: MissionPackage;
        public part informationEnvironment: InformationEnvironment;

        connect messageIntoMissionModel to informationEnvironment.messageIntoInformationEnvironment;
        connect informationEnvironment.messageForMissionPackage to missionPackage.ordersForMissionPackage;
        connect missionPackage.statusFromMissionPackage to reportsFromMissionModel;
        connect missionPackage.statusFromMissionPackage to informationEnvironment.systemStatusToInformationEnvironment;
      }

      package MissionPackage {
        import MissionArchitecture::SimDomain::*;
        import MissionArchitecture::Sim::MissionModel::MissionPackage::CoupledMissionSystem::*;
        import MissionArchitecture::cloud::simlytics::c2sim::*;

        #DevsCoupledModel
        part def MissionPackage {
          in port ordersForMissionPackage: Message;
          out port statusFromMissionPackage: SystemStatus;

          public part platforms: Platform[0..*];
          public part robots: CoupledMissionSystem[0..*];
          public part externalPlatforms: ExternalPlatform[0..*];
          public part missionPackageEngine: MissionPackageEngine;
          public part c2SimServer: C2SimAdapter;
          public part bifrostAdapter: BifrostAdapter;
          public part missionSystemObserver: MissionSystemObserver;

          // Orders route into the MissionPackageEngine which mediates and forwards statuses
          connect ordersForMissionPackage to missionPackageEngine.receiveOrder;

          // Platforms and external adapters send status into the engine
          connect platforms.sendStatus to missionPackageEngine.receiveMissionSystemStatus;
          connect externalPlatforms.sendStatus to missionPackageEngine.receiveMissionSystemStatus;
          connect robots.coupledSendStatus to missionPackageEngine.receiveMissionSystemStatus;

          // MissionPackageEngine emits the unified status to the MissionPackage output and observer
          connect missionPackageEngine.sendStatus to statusFromMissionPackage;
          connect missionPackageEngine.sendStatus to missionSystemObserver.missionSystemObservation;

          // C2SimAdapter connects to the MissionPackageEngine for orders and status
          connect missionPackageEngine.sendStatus to c2SimServer.reportsToC2Sim;
          connect c2SimServer.statusFromC2Sim to missionPackageEngine.receiveExternalStatus;
          connect c2SimServer.ordersFromC2Sim to missionPackageEngine.receiveOrder;

          // BifrostAdapter connects to the MissionPackageEngine for status
          connect missionPackageEngine.sendStatus to bifrostAdapter.reportsToBifrost;
          connect bifrostAdapter.statusFromBifrost to missionPackageEngine.receiveExternalStatus;

          // Engine emits routed orders to child parts/adapters
          connect missionPackageEngine.sendOrder to platforms.receiveOrder;
          connect missionPackageEngine.sendOrder to robots.coupledReceiveOrder;
          connect missionPackageEngine.sendOrder to c2SimServer.ordersToC2Sim;
          connect missionPackageEngine.sendOrder to bifrostAdapter.ordersToBifrost;

          // Engine emits status updates to child parts/adapters
          connect missionPackageEngine.updateExternalPlatformStatus to externalPlatforms.updateStatus;
        }

        package CoupledMissionSystem {
          import MissionArchitecture::SimDomain::*;

          #DevsAtomicModel
          abstract part def MissionSystem {
            constant attribute id: String;
            constant attribute name: String;
            constant attribute fullName: String;
            constant attribute milStd2525Symbol: String;
            constant attribute speedKph: Real;
            attribute velocity: Velocity;
            attribute degreesOrientation: DegreesOrientation;
            attribute location: Location;
            attribute assignedTasks: Task[0..*];
            in port receiveOrder: Message;
            out port sendStatus: SystemStatus;
          }

          #DevsAtomicModel
          part def AutonomousSystem specializes MissionSystem {
            attribute operatingMode: UavOperatingMode;
            attribute requiredPowerWatts: Real;
            attribute batteryChargePercent: Real;
            constant attribute statusUpdateIntervalSeconds: Integer;
            out port requestedPowerWatts: PowerRequest;
            in port batteryStatusIn: BatteryStatus;
          }

          #DevsAtomicModel
          part def Battery {
            constant attribute energyCapacityWattHours: Real;
            constant attribute batteryUpdateIntervalSeconds: Integer;
            constant attribute packConfig: BatteryPackConfig;
            constant attribute capacityAmpHours: Real;
            attribute energyRemainingWattHours: Real;
            attribute continuousEnergyDrawWatts: Real;
            attribute batteryState: BatteryOperatingState;
            attribute internalTemperatureC: Real;
            in port powerRequestWatts: PowerRequest;
            out port batteryStatusOut: BatteryStatus;
          }

          #DevsCoupledModel
          part def CoupledMissionSystem {
            in port coupledReceiveOrder: Task;
            out port coupledSendStatus: SystemStatus;

            public part uavSystem: AutonomousSystem;
            public part uavBattery: Battery;

            connect uavSystem.requestedPowerWatts to uavBattery.powerRequestWatts;
            connect uavBattery.batteryStatusOut to uavSystem.batteryStatusIn;
            connect coupledReceiveOrder to uavSystem.receiveOrder;
            connect uavSystem.sendStatus to coupledSendStatus;
          }
        }


        #DevsAtomicModel
        part def ExternalPlatform specializes MissionSystem {
          in port updateStatus: SystemStatus;
        }

        #DevsAtomicModel
        part def Platform specializes MissionSystem {
        }

        #DevsAtomicModel
        part def MissionSystemObserver {
          in port missionSystemObservation: SystemStatus;
        }

        #DevsAtomicModel
        part def MissionPackageEngine {
          constant attribute c2simScenario: C2SimScenario;
          // Receives orders for the MissionPackage
          in port receiveOrder: Message;
          // Emits orders routed to external adapters or platforms
          out port sendOrder: Message;
          // Receives status reports from all platforms/mission systems
          in port receiveMissionSystemStatus: SystemStatus;
          in port receiveExternalStatus: SystemStatus;
          out port sendStatus: SystemStatus;
          out port updateExternalPlatformStatus: SystemStatus;
        }

        #DevsAtomicModel
        part def C2SimAdapter {
          constant attribute c2simClientUrl: String;
          constant attribute c2simUpdateIntervalSeconds: Integer;
          in port ordersToC2Sim: Message;
          in port reportsToC2Sim: SystemStatus;
          out port statusFromC2Sim: SystemStatus;
          out port ordersFromC2Sim: Message;
        }

        #DevsAtomicModel
        part def BifrostAdapter {
          in port ordersToBifrost: Message;
          in port reportsToBifrost: SystemStatus;
          out port statusFromBifrost: SystemStatus;
        }
      }

      package InformationEnvironment {
        import MissionArchitecture::SimDomain::*;
        import MissionArchitecture::SimDomain::InformationDomain::*;
        import MissionArchitecture::Sim::MissionModel::InformationEnvironment::NetworkModel::*;

        #DevsCoupledModel
        part def InformationEnvironment {
          in port messageIntoInformationEnvironment: Message;
          in port systemStatusToInformationEnvironment: SystemStatus;
          out port messageForMissionPackage: Message;

          public part networkModel: NetworkModel;

          connect systemStatusToInformationEnvironment to networkModel.statusToNetworkModel;
          connect messageIntoInformationEnvironment to networkModel.messageIntoNetworkModel;
          connect networkModel.messageFromNetworkModel to messageForMissionPackage;
        }

        package NetworkModel {
          import MissionArchitecture::SimDomain::*;

          #DevsAtomicModel
          part def CommunicationsNetwork {
            in port messageIntoCommsNetwork: Message;
            in port statusToCommsNetwork: SystemStatus;
            in port jammingActivityToCommsNetwork: JammingActivity;
            out port messageFromCommsNetwork: Message;
            out port radioConnectivityObservation: RadioConnectivity;
            
              // Registry of radios as state attribute (map keyed by systemId)
              attribute knownRadios: RadioInfo[0..*] #Map(key = String, value = RadioInfo);
              // Registry of active jammers
              attribute activeJammers: JammingActivity[0..*] #Map(key = String, value = JammingActivity);
              // Initial known radios from properties
              constant attribute initialKnownRadios: RadioInfo[0..*] #Map(key = String, value = RadioInfo);
              constant attribute connectivityUpdateFrequencySeconds: Integer;
          }

          #DevsAtomicModel
          part def Jammer {
            in port statusToJammer: SystemStatus;
            out port jammerInitiatesJammingActivity: JammingActivity;
            constant attribute jammingPowerWatts: Real;
            constant attribute jammingFrequency: Real;
            constant attribute jammingIntervalSeconds: Integer;
            attribute currentLocation: Location;
          }

          #DevsAtomicModel
          part def NetworkConnectivityObserver {
            in port connectivityObservation: RadioConnectivity;
          }

          #DevsCoupledModel
          part def NetworkModel {
            in port messageIntoNetworkModel: Message;
            in port statusToNetworkModel: SystemStatus;
            out port messageFromNetworkModel: Message;

            public part commsNetwork: CommunicationsNetwork;
            public part jammer: Jammer;
            public part networkConnectivityObserver: NetworkConnectivityObserver;

            connect messageIntoNetworkModel to commsNetwork.messageIntoCommsNetwork;
            connect statusToNetworkModel to commsNetwork.statusToCommsNetwork;
            connect statusToNetworkModel to jammer.statusToJammer;
            connect commsNetwork.messageFromCommsNetwork to messageFromNetworkModel;
            connect commsNetwork.radioConnectivityObservation to networkConnectivityObserver.connectivityObservation;
            connect jammer.jammerInitiatesJammingActivity to commsNetwork.jammingActivityToCommsNetwork;
          }
        }
      }
    }

    package SimFrame {
      import MissionArchitecture::SimDomain::*;
      import MissionArchitecture::Sim::MissionModel::*;

      #DevsAtomicModel
      part def OrderGenerator {
        out port ordersOut: Message;
      }

      #DevsAtomicModel
      part def SimObserver {
        attribute systemMap: SystemStatus[0..*] #Map(key = String, value = SystemStatus);
        in port systemStatusIn: SystemStatus;
      }

      #DevsCoupledModel
      part def SimFrame {
        public part orderGenerator: OrderGenerator;
        public part missionModel: MissionModel;
        public part simObserver: SimObserver;

        connect orderGenerator.ordersOut to missionModel.messageIntoMissionModel;
        connect missionModel.reportsFromMissionModel to simObserver.systemStatusIn;
      }
    }
  }
}