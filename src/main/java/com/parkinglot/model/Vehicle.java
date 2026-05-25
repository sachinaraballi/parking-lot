package com.parkinglot.model;

import java.util.UUID;

public class Vehicle {
    private String id;
    private String licensePlate;
    private VehicleType type;

    public Vehicle() {}

    public Vehicle(String licensePlate, VehicleType type) {
        this.id = UUID.randomUUID().toString();
        this.licensePlate = licensePlate.toUpperCase();
        this.type = type;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getLicensePlate() { return licensePlate; }
    public void setLicensePlate(String licensePlate) { this.licensePlate = licensePlate; }

    public VehicleType getType() { return type; }
    public void setType(VehicleType type) { this.type = type; }
}
