package com.parkinglot.model;

import java.util.UUID;

public class ParkingSpot {
    private String id;
    private String spotNumber;
    private SpotType type;
    private boolean occupied;
    private String vehicleId;

    public ParkingSpot() {}

    public ParkingSpot(String spotNumber, SpotType type) {
        this.id = UUID.randomUUID().toString();
        this.spotNumber = spotNumber;
        this.type = type;
        this.occupied = false;
    }

    /**
     * Spot compatibility:
     *   MOTORCYCLE → SMALL, MEDIUM, LARGE
     *   CAR        → MEDIUM, LARGE
     *   TRUCK      → LARGE only
     */
    public boolean canFitVehicle(VehicleType vehicleType) {
        return switch (vehicleType) {
            case MOTORCYCLE -> true;
            case CAR -> type == SpotType.MEDIUM || type == SpotType.LARGE;
            case TRUCK -> type == SpotType.LARGE;
        };
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSpotNumber() { return spotNumber; }
    public void setSpotNumber(String spotNumber) { this.spotNumber = spotNumber; }

    public SpotType getType() { return type; }
    public void setType(SpotType type) { this.type = type; }

    public boolean isOccupied() { return occupied; }
    public void setOccupied(boolean occupied) { this.occupied = occupied; }

    public String getVehicleId() { return vehicleId; }
    public void setVehicleId(String vehicleId) { this.vehicleId = vehicleId; }
}
