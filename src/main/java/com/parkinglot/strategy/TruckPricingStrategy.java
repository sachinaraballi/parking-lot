package com.parkinglot.strategy;

public class TruckPricingStrategy implements PricingStrategy {

    private static final double HOURLY_RATE = 3.50;

    @Override
    public double calculate(long durationMinutes) {
        double hours = Math.ceil(durationMinutes / 60.0);
        return Math.max(1, hours) * HOURLY_RATE;
    }

    @Override
    public double getHourlyRate() {
        return HOURLY_RATE;
    }

    @Override
    public String getDescription() {
        return "$3.50/hour (min 1 hour)";
    }
}
