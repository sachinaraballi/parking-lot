package com.parkinglot.strategy;

/**
 * Strategy interface for pricing — each vehicle type plugs in its own rate.
 * Open for extension (new vehicle types) without modifying existing strategies.
 */
public interface PricingStrategy {
    double calculate(long durationMinutes);
    double getHourlyRate();
    String getDescription();
}
