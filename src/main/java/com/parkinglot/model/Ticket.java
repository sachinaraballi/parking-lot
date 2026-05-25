package com.parkinglot.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class Ticket {
    private String id;
    private Vehicle vehicle;
    private String spotId;
    private String floorId;
    private int floorNumber;
    private String spotNumber;
    private SpotType spotType;
    private LocalDateTime entryTime;
    private LocalDateTime exitTime;
    private TicketStatus status;
    private Double amount;

    public Ticket() {}

    public Ticket(Vehicle vehicle, String spotId, String floorId,
                  int floorNumber, String spotNumber, SpotType spotType) {
        this.id = "TKT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        this.vehicle = vehicle;
        this.spotId = spotId;
        this.floorId = floorId;
        this.floorNumber = floorNumber;
        this.spotNumber = spotNumber;
        this.spotType = spotType;
        this.entryTime = LocalDateTime.now();
        this.status = TicketStatus.ACTIVE;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Vehicle getVehicle() { return vehicle; }
    public void setVehicle(Vehicle vehicle) { this.vehicle = vehicle; }

    public String getSpotId() { return spotId; }
    public void setSpotId(String spotId) { this.spotId = spotId; }

    public String getFloorId() { return floorId; }
    public void setFloorId(String floorId) { this.floorId = floorId; }

    public int getFloorNumber() { return floorNumber; }
    public void setFloorNumber(int floorNumber) { this.floorNumber = floorNumber; }

    public String getSpotNumber() { return spotNumber; }
    public void setSpotNumber(String spotNumber) { this.spotNumber = spotNumber; }

    public SpotType getSpotType() { return spotType; }
    public void setSpotType(SpotType spotType) { this.spotType = spotType; }

    public LocalDateTime getEntryTime() { return entryTime; }
    public void setEntryTime(LocalDateTime entryTime) { this.entryTime = entryTime; }

    public LocalDateTime getExitTime() { return exitTime; }
    public void setExitTime(LocalDateTime exitTime) { this.exitTime = exitTime; }

    public TicketStatus getStatus() { return status; }
    public void setStatus(TicketStatus status) { this.status = status; }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }
}
