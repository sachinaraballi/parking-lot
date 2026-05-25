package com.parkinglot.strategy;

public class MotorcyclePricingStrategy implements PricingStrategy {

    private static final double HOURLY_RATE = 1.00;

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
        return "$1.00/hour (min 1 hour)";
    }
}
