package irpsystem {
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
  package irpmodel {
      import irpsystem::irpdomain::*;

      item def FacilityProps {
        constant attribute coordinate: Coordinate;
        constant attribute startingInventory: Real;
        constant attribute inventoryCost: Real;
      }

        #DevsAtomicModel
        part def Vehicle {
          constant attribute vehicleId: Integer;
          constant attribute capacity: Real;
          constant attribute manufacturerLocation: Coordinate;
          attribute dailyKmTraveled: Real;
          attribute deliveryRoute: DeliveryRoute;
          attribute location: Coordinate;
          constant attribute costPerKm: Real;
          constant attribute speedKmHr: Real;
          in port acceptDeliveryRoute: DeliveryRoute;
          out port dropDelivery: Delivery;
          out port dailyDeliveryCost: VehicleCost;
        }
        #DevsAtomicModel
        part def ImprovedVehicle specializes Vehicle {
        }
      #DevsAtomicModel
      abstract part def Facility {
        constant attribute facilityProperties: FacilityProps;
        attribute currentInventory: Real;
      }
      #DevsAtomicModel
      part def Manufacturer specializes Facility {
        constant attribute dailyProduction: Real;
        in port acceptDeliverySchedule: DeliverySchedule;
        in port acceptDelivery: Delivery;
        out port postDeliveryRoute: DeliveryRoute;
        out port dailyInventoryCost: InventoryCost;
      }
      #DevsAtomicModel
      part def Retailer specializes Facility {
        constant attribute retailerId: Integer;
        constant attribute minInventory: Real;
        constant attribute maxInventory: Real;
        constant attribute dailyConsumption: Real;
        in port receiveDelivery: Delivery;
        out port dailyInventoryCost: InventoryCost;
      }

      #DevsCoupledModel
      abstract part def InventoryRouting {
          in port receiveDeliverySchedule: DeliverySchedule;
          out port reportVehicleCost: VehicleCost;
          out port reportInventoryCost: InventoryCost;
      }

      #DevsCoupledModel part def BasicInventoryRouting specializes InventoryRouting {
        public part manufacturer: Manufacturer;
        public part vehicles: Vehicle[0..*];
        public part retailers: Retailer[0..*];
        connect receiveDeliverySchedule to manufacturer.acceptDeliverySchedule;
        connect manufacturer.postDeliveryRoute to vehicles.acceptDeliveryRoute;
        connect vehicles.dropDelivery to retailers.receiveDelivery;
        connect manufacturer.dailyInventoryCost to reportInventoryCost;
        connect retailers.dailyInventoryCost to reportInventoryCost;
        connect vehicles.dailyDeliveryCost to reportVehicleCost;
      }

      #DevsCoupledModel
      part def ImprovedInventoryRouting specializes InventoryRouting {
          public part improvedManufacturer: Manufacturer;
          public part improvedVehicles: ImprovedVehicle[0..*];
          public part improvedRetailers: Retailer[0..*];
          connect receiveDeliverySchedule to improvedManufacturer.acceptDeliverySchedule;
          connect improvedManufacturer.postDeliveryRoute to improvedVehicles.acceptDeliveryRoute;
          connect improvedVehicles.dropDelivery to improvedRetailers.receiveDelivery;
          connect improvedVehicles.dropDelivery to improvedManufacturer.acceptDelivery;
          connect improvedManufacturer.dailyInventoryCost to reportInventoryCost;
          connect improvedRetailers.dailyInventoryCost to reportInventoryCost;
          connect improvedVehicles.dailyDeliveryCost to reportVehicleCost;
      }

  }

  package ExperimentalFrame {
      import irpsystem::irpdomain::*;
      import irpsystem::irpmodel::*;

      #DevsAtomicModel part def DeliveryScheduleGenerator {
        attribute deliverySchedule: DeliverySchedule;
        out port postDeliverySchedule: DeliverySchedule;
      }
      #DevsAtomicModel
      part def Transducer {
          attribute vehicleCostByDayByVehicle: Real[0..*] #Map(key = Integer, value = map(key = Integer, value = Real));
          attribute inventoryCostByDayByRetailer: Real[0..1] #Map(key = Integer, value = map(key = Integer, value = Real));
          in port aggregateInventoryCost: InventoryCost;
          in port aggregateVehicleCost: VehicleCost;
      }


    #DevsCoupledModel part def BasicExperimentalFrame {
          public part generator: DeliveryScheduleGenerator;
          public part transducer: Transducer;
          public part basicInventoryRouting: BasicInventoryRouting;
          connect generator.postDeliverySchedule to basicInventoryRouting.receiveDeliverySchedule;
          connect basicInventoryRouting.reportVehicleCost to transducer.aggregateVehicleCost;
          connect basicInventoryRouting.reportInventoryCost to transducer.aggregateInventoryCost;
    }

    #DevsCoupledModel part def ImprovedExperimentalFrame {
      public part generator: DeliveryScheduleGenerator;
      public part transducer: Transducer;
      public part improvedInventoryRouting: ImprovedInventoryRouting;
      connect generator.postDeliverySchedule to improvedInventoryRouting.receiveDeliverySchedule;
      connect improvedInventoryRouting.reportVehicleCost to transducer.aggregateVehicleCost;
      connect improvedInventoryRouting.reportInventoryCost to transducer.aggregateInventoryCost;
    }

  }


  package irpdomain {
  
    item def Coordinate {
      attribute x: Real;
      attribute y: Real;
    }
  
    item def Delivery {
      attribute retailerId: Integer;
      attribute retailerLocation: Coordinate;
      attribute productAmount: Real;
    }
  
    item def DeliveryRoute {
      attribute vehicleId: Integer;
      attribute deliveries:Delivery[0..*];
    }
  
    item def DeliverySchedule {
      attribute deliveriesByDayByVehicle: DeliveryRoute[0..*]
        #Map(key = Integer, value = map(key = Integer, value = DeliveryRoute));
    }
  
    item def VehicleCost {
      constant attribute day: Integer;
      constant attribute vehicleId: Integer;
      constant attribute cost: Real;
    }
  
    item def InventoryCost {
      constant attribute day: Integer;
      constant attribute retailerId: Integer;
      constant attribute cost: Real;
    }
  
    enum def CostType {
      HIGH,
      LOW
    }
  }

}
