package com.parkinglot.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ParkingFloor {
    private String id;
    private int floorNumber;
    private List<ParkingSpot> spots;

    public ParkingFloor() {
        this.spots = new ArrayList<>();
    }

    public ParkingFloor(int floorNumber) {
        this.id = UUID.randomUUID().toString();
        this.floorNumber = floorNumber;
        this.spots = new ArrayList<>();
    }

    /**
     * Returns the smallest suitable spot to maximize space utilization
     * (fits a motorcycle in SMALL before using a LARGE spot).
     */
    public Optional<ParkingSpot> findAvailableSpot(VehicleType vehicleType) {
        return spots.stream()
            .filter(spot -> !spot.isOccupied() && spot.canFitVehicle(vehicleType))
            .min(Comparator.comparingInt(spot -> spot.getType().ordinal()));
    }

    public long countAvailableSpots() {
        return spots.stream().filter(s -> !s.isOccupied()).count();
    }

    public long countOccupiedSpots() {
        return spots.stream().filter(ParkingSpot::isOccupied).count();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public int getFloorNumber() { return floorNumber; }
    public void setFloorNumber(int floorNumber) { this.floorNumber = floorNumber; }

    public List<ParkingSpot> getSpots() { return spots; }
    public void setSpots(List<ParkingSpot> spots) { this.spots = spots; }
}
