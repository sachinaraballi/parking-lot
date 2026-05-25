package com.parkinglot.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ParkingLotData {
    private String id;
    private String name;
    private String address;
    private List<ParkingFloor> floors;
    private List<Ticket> tickets;

    public ParkingLotData() {
        this.floors = new ArrayList<>();
        this.tickets = new ArrayList<>();
    }

    public Optional<ParkingFloor> findFloorWithAvailableSpot(VehicleType vehicleType) {
        return floors.stream()
            .filter(floor -> floor.findAvailableSpot(vehicleType).isPresent())
            .findFirst();
    }

    public Optional<Ticket> findTicketById(String ticketId) {
        return tickets.stream()
            .filter(t -> t.getId().equals(ticketId))
            .findFirst();
    }

    public Optional<ParkingSpot> findSpotById(String spotId) {
        return floors.stream()
            .flatMap(floor -> floor.getSpots().stream())
            .filter(spot -> spot.getId().equals(spotId))
            .findFirst();
    }

    public Optional<ParkingFloor> findFloorById(String floorId) {
        return floors.stream()
            .filter(floor -> floor.getId().equals(floorId))
            .findFirst();
    }

    @JsonIgnore
    public long getTotalSpots() {
        return floors.stream().mapToLong(f -> f.getSpots().size()).sum();
    }

    @JsonIgnore
    public long getAvailableSpots() {
        return floors.stream().mapToLong(ParkingFloor::countAvailableSpots).sum();
    }

    @JsonIgnore
    public long getOccupiedSpots() {
        return floors.stream().mapToLong(ParkingFloor::countOccupiedSpots).sum();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public List<ParkingFloor> getFloors() { return floors; }
    public void setFloors(List<ParkingFloor> floors) { this.floors = floors; }

    public List<Ticket> getTickets() { return tickets; }
    public void setTickets(List<Ticket> tickets) { this.tickets = tickets; }
}
