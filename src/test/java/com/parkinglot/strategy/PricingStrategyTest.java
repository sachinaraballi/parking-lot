package com.parkinglot.strategy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Pricing Strategy Tests")
class PricingStrategyTest {

    // ── Motorcycle ($1/hr) ──────────────────────────────────────────────────

    @Test
    @DisplayName("Motorcycle: 30 min → billed as 1 hour = $1.00")
    void motorcycle_lessThanOneHour_chargesMinimum() {
        PricingStrategy strategy = new MotorcyclePricingStrategy();
        assertThat(strategy.calculate(30)).isEqualTo(1.00);
    }

    @Test
    @DisplayName("Motorcycle: exactly 60 min = $1.00")
    void motorcycle_exactlyOneHour() {
        assertThat(new MotorcyclePricingStrategy().calculate(60)).isEqualTo(1.00);
    }

    @Test
    @DisplayName("Motorcycle: 61 min → ceil to 2 hours = $2.00")
    void motorcycle_justOverOneHour() {
        assertThat(new MotorcyclePricingStrategy().calculate(61)).isEqualTo(2.00);
    }

    @ParameterizedTest(name = "{0} min → ${2}")
    @CsvSource({"30,MOTORCYCLE,1.0", "60,MOTORCYCLE,1.0", "90,MOTORCYCLE,2.0", "120,MOTORCYCLE,2.0", "121,MOTORCYCLE,3.0"})
    @DisplayName("Motorcycle: parameterised duration → amount")
    void motorcycle_parameterised(long minutes, String ignored, double expected) {
        assertThat(new MotorcyclePricingStrategy().calculate(minutes)).isEqualTo(expected);
    }

    // ── Car ($2/hr) ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("Car: 45 min → billed as 1 hour = $2.00")
    void car_lessThanOneHour_chargesMinimum() {
        assertThat(new CarPricingStrategy().calculate(45)).isEqualTo(2.00);
    }

    @Test
    @DisplayName("Car: 120 min = $4.00")
    void car_twoHours() {
        assertThat(new CarPricingStrategy().calculate(120)).isEqualTo(4.00);
    }

    @Test
    @DisplayName("Car: 135 min → ceil to 3 hours = $6.00")
    void car_twoHoursAndQuarter() {
        assertThat(new CarPricingStrategy().calculate(135)).isEqualTo(6.00);
    }

    // ── Truck ($3.50/hr) ─────────────────────────────────────────────────────

    @Test
    @DisplayName("Truck: 1 min → billed as 1 hour = $3.50")
    void truck_oneMinute_chargesMinimum() {
        assertThat(new TruckPricingStrategy().calculate(1)).isEqualTo(3.50);
    }

    @Test
    @DisplayName("Truck: 180 min = $10.50")
    void truck_threeHours() {
        assertThat(new TruckPricingStrategy().calculate(180)).isEqualTo(10.50);
    }

    // ── Hourly rates ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("Hourly rates are correct for each strategy")
    void hourlyRates_areCorrect() {
        assertThat(new MotorcyclePricingStrategy().getHourlyRate()).isEqualTo(1.00);
        assertThat(new CarPricingStrategy().getHourlyRate()).isEqualTo(2.00);
        assertThat(new TruckPricingStrategy().getHourlyRate()).isEqualTo(3.50);
    }
}
