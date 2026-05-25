package com.parkinglot.factory;

import com.parkinglot.model.VehicleType;
import com.parkinglot.strategy.CarPricingStrategy;
import com.parkinglot.strategy.MotorcyclePricingStrategy;
import com.parkinglot.strategy.PricingStrategy;
import com.parkinglot.strategy.TruckPricingStrategy;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Factory that maps vehicle types to their pricing strategies.
 * Adding a new vehicle type only requires adding an entry here
 * without changing any existing strategy class.
 */
@Component
public class PricingStrategyFactory {

    private final Map<VehicleType, PricingStrategy> strategies = Map.of(
        VehicleType.MOTORCYCLE, new MotorcyclePricingStrategy(),
        VehicleType.CAR, new CarPricingStrategy(),
        VehicleType.TRUCK, new TruckPricingStrategy()
    );

    public PricingStrategy getStrategy(VehicleType vehicleType) {
        PricingStrategy strategy = strategies.get(vehicleType);
        if (strategy == null) {
            throw new IllegalArgumentException("No pricing strategy for: " + vehicleType);
        }
        return strategy;
    }

    public Map<VehicleType, PricingStrategy> getAllStrategies() {
        return strategies;
    }
}
