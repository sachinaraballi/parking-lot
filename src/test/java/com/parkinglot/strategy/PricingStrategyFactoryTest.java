package com.parkinglot.strategy;

import com.parkinglot.factory.PricingStrategyFactory;
import com.parkinglot.model.VehicleType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PricingStrategyFactory Tests")
class PricingStrategyFactoryTest {

    private PricingStrategyFactory factory;

    @BeforeEach
    void setUp() {
        factory = new PricingStrategyFactory();
    }

    @Test
    @DisplayName("Returns MotorcyclePricingStrategy for MOTORCYCLE")
    void getStrategy_motorcycle() {
        PricingStrategy strategy = factory.getStrategy(VehicleType.MOTORCYCLE);
        assertThat(strategy).isInstanceOf(MotorcyclePricingStrategy.class);
    }

    @Test
    @DisplayName("Returns CarPricingStrategy for CAR")
    void getStrategy_car() {
        PricingStrategy strategy = factory.getStrategy(VehicleType.CAR);
        assertThat(strategy).isInstanceOf(CarPricingStrategy.class);
    }

    @Test
    @DisplayName("Returns TruckPricingStrategy for TRUCK")
    void getStrategy_truck() {
        PricingStrategy strategy = factory.getStrategy(VehicleType.TRUCK);
        assertThat(strategy).isInstanceOf(TruckPricingStrategy.class);
    }

    @Test
    @DisplayName("getAllStrategies returns an entry for every VehicleType")
    void getAllStrategies_coversAllVehicleTypes() {
        assertThat(factory.getAllStrategies()).containsKeys(
            VehicleType.MOTORCYCLE, VehicleType.CAR, VehicleType.TRUCK
        );
    }
}
