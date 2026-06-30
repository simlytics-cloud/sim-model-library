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

    abstract item def MessagePayload {
        attribute payloadId: String;
    }

    item def Message {
      attribute senderId: String;
      attribute receiverId: String;
      attribute messageId: String;
      attribute networkId: String;
      attribute messageType: String;
      attribute payload: MessagePayload;
    }

    enum def TaskStatus {
      AWAITING,
      EXECUTING,
      COMPLETE,
      CANCELLED
    }

    item def Task specializes MessagePayload {
      attribute taskUid: String;
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
        enum def JammerCommandEnum {
            START_JAMMING,
            STOP_JAMMING
        }

        item def JammerCommand {
          attribute jammerId: String;
          attribute networkId: String;
          attribute jammerCommand: JammerCommandEnum;
        }

        item def NetworkConnectivity {
            attribute nodes: String[0..*];
            attribute matrix: Boolean[0..*] #Map(key = String, value = map(key = String, value = Boolean));
        }

        item def CommunicationsSystemInit {
            attribute platformId: String;
            attribute transmitPower: Real;
            attribute location: Location;
        }

        item def CommunicationsNetworkInit {
            attribute networkId: String;
            attribute radios: CommunicationsSystemInit[1..*];
            attribute jammers: CommunicationsSystemInit[0..*];
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
      import MissionArchitecture::SimDomain::InformationDomain::*;
      import MissionArchitecture::Sim::MissionModel::MissionPackage::*;
      import MissionArchitecture::Sim::MissionModel::InformationEnvironment::*;

      #DevsCoupledModel
      part def MissionModel {
        in port messageIntoMissionModel: Message;
        in port jammerCommandIntoMissionModel: JammerCommand;
        out port reportsFromMissionModel: SystemStatus;

        public part missionPackage: MissionPackage;
        public part informationEnvironment: InformationEnvironment;

        connect messageIntoMissionModel to informationEnvironment.messageIntoInformationEnvironment;
        connect jammerCommandIntoMissionModel to informationEnvironment.jammerCommandToInformationEnvironment;
        connect informationEnvironment.messageForMissionPackage to missionPackage.ordersForMissionPackage;
        connect informationEnvironment.networkConnectivityForMissionPackage to missionPackage.networkConnectivityToMissionPackage;
        connect missionPackage.statusFromMissionPackage to reportsFromMissionModel;
        connect missionPackage.statusFromMissionPackage to informationEnvironment.systemStatusToInformationEnvironment;
      }

      package MissionPackage {
        import MissionArchitecture::SimDomain::*;
        import MissionArchitecture::SimDomain::InformationDomain::*;
        import MissionArchitecture::Sim::MissionModel::MissionPackage::CoupledMissionSystem::*;
        import MissionArchitecture::cloud::simlytics::c2sim::*;

        #DevsCoupledModel
        part def MissionPackage {
          in port ordersForMissionPackage: Message;
          in port networkConnectivityToMissionPackage: NetworkConnectivity;
          out port statusFromMissionPackage: SystemStatus;
          out port jammerCommandFromMissionPackage: JammerCommand;

          public part platforms: Platform[0..*];
          public part robots: CoupledMissionSystem[0..*];
          public part externalPlatforms: ExternalPlatform[0..*];
          public part missionPackageEngine: MissionPackageEngine;
          public part c2SimServer: C2SimAdapter;
          public part bifrostAdapter: BifrostAdapter;
          public part missionSystemObserver: MissionSystemObserver;
          public part ravenUav: RavenMS4Me;

          // Orders route into the MissionPackageEngine which mediates and forwards statuses
          connect ordersForMissionPackage to missionPackageEngine.receiveOrder;

          // Platforms and external adapters send status into the engine
          connect platforms.sendStatus to missionPackageEngine.receiveMissionSystemStatus;
          connect externalPlatforms.sendStatus to missionPackageEngine.receiveMissionSystemStatus;
          connect robots.coupledSendStatus to missionPackageEngine.receiveMissionSystemStatus;
          connect ravenUav.sendStatus to missionPackageEngine.receiveMissionSystemStatus;

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
          connect missionPackageEngine.sendOrder to ravenUav.receiveOrder;

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
            in port coupledReceiveOrder: Message;
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
        part def RavenMS4Me specializes MissionSystem {
            constant attribute takeoffTimeSeconds: Real;
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
          in port jammerCommandToInformationEnvironment: JammerCommand;
          out port messageForMissionPackage: Message;
          out port networkConnectivityForMissionPackage: NetworkConnectivity;

          public part networkModel: NetworkModel;

          connect systemStatusToInformationEnvironment to networkModel.statusToNetworkModel;
          connect messageIntoInformationEnvironment to networkModel.messageIntoNetworkModel;
          connect jammerCommandToInformationEnvironment to networkModel.jammerCommandToNetworkModel;
          connect networkModel.messageFromNetworkModel to messageForMissionPackage;
          connect networkModel.networkConnectivityReport to networkConnectivityForMissionPackage;
        }

        package NetworkModel {
          import MissionArchitecture::SimDomain::*;
          import MissionArchitecture::SimDomain::InformationDomain::*;

          #DevsAtomicModel
          abstract part def AbstractCommunicationsNetwork {
            constant attribute communicationsNetworks: CommunicationsNetworkInit[1..*];
            in port messageIntoCommsNetwork: Message;
            in port statusToCommsNetwork: SystemStatus;
            in port jammerCommandToCommsNetwork: JammerCommand;
            out port messageFromCommsNetwork: Message;
          }
           #DevsAtomicModel
           part def PerfectCommunicationsNetwork specializes AbstractCommunicationsNetwork {
             out port networkConnectivityObservation: NetworkConnectivity;
           }

          #DevsAtomicModel
          part def CommunicationsNetwork specializes AbstractCommunicationsNetwork {
            out port networkConnectivityObservation: NetworkConnectivity;
          }

          #DevsAtomicModel
          part def NetworkConnectivityObserver {
            in port connectivityObservation: NetworkConnectivity;
          }

          #DevsCoupledModel
          part def NetworkModel {
            in port messageIntoNetworkModel: Message;
            in port statusToNetworkModel: SystemStatus;
            in port jammerCommandToNetworkModel: JammerCommand;
            out port messageFromNetworkModel: Message;
            out port networkConnectivityReport: NetworkConnectivity;

            public part commsNetwork: CommunicationsNetwork;
            public part networkConnectivityObserver: NetworkConnectivityObserver;

            connect messageIntoNetworkModel to commsNetwork.messageIntoCommsNetwork;
            connect statusToNetworkModel to commsNetwork.statusToCommsNetwork;
            connect jammerCommandToNetworkModel to commsNetwork.jammerCommandToCommsNetwork;
            connect commsNetwork.messageFromCommsNetwork to messageFromNetworkModel;
            connect commsNetwork.networkConnectivityObservation to networkConnectivityObserver.connectivityObservation;
            connect commsNetwork.networkConnectivityObservation to networkConnectivityReport;
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