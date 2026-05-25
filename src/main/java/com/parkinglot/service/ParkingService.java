package com.parkinglot.service;

import com.parkinglot.exception.NoSpotAvailableException;
import com.parkinglot.exception.ParkingException;
import com.parkinglot.exception.TicketNotFoundException;
import com.parkinglot.factory.PricingStrategyFactory;
import com.parkinglot.model.ParkingFloor;
import com.parkinglot.model.ParkingLotData;
import com.parkinglot.model.ParkingSpot;
import com.parkinglot.model.SpotType;
import com.parkinglot.model.Ticket;
import com.parkinglot.model.TicketStatus;
import com.parkinglot.model.Vehicle;
import com.parkinglot.model.VehicleType;
import com.parkinglot.repository.ParkingLotRepository;
import com.parkinglot.strategy.PricingStrategy;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ParkingService {

    private final ParkingLotRepository repository;
    private final PricingStrategyFactory pricingStrategyFactory;

    public ParkingService(ParkingLotRepository repository, PricingStrategyFactory pricingStrategyFactory) {
        this.repository = repository;
        this.pricingStrategyFactory = pricingStrategyFactory;
    }

    public synchronized Ticket parkVehicle(String licensePlate, VehicleType vehicleType) {
        ParkingLotData lot = repository.load();

        boolean alreadyParked = lot.getTickets().stream()
            .anyMatch(t -> t.getVehicle().getLicensePlate().equalsIgnoreCase(licensePlate)
                          && t.getStatus() == TicketStatus.ACTIVE);
        if (alreadyParked) {
            throw new ParkingException("Vehicle " + licensePlate.toUpperCase() + " is already parked.");
        }

        ParkingFloor floor = lot.findFloorWithAvailableSpot(vehicleType)
            .orElseThrow(() -> new NoSpotAvailableException(
                "No available spot for vehicle type: " + vehicleType));

        ParkingSpot spot = floor.findAvailableSpot(vehicleType)
            .orElseThrow(() -> new NoSpotAvailableException("Spot disappeared during assignment."));

        Vehicle vehicle = new Vehicle(licensePlate, vehicleType);
        Ticket ticket = new Ticket(vehicle, spot.getId(), floor.getId(),
                                   floor.getFloorNumber(), spot.getSpotNumber(), spot.getType());

        spot.setOccupied(true);
        spot.setVehicleId(vehicle.getId());
        lot.getTickets().add(ticket);

        repository.save(lot);
        return ticket;
    }

    public synchronized Map<String, Object> exitVehicle(String ticketId) {
        ParkingLotData lot = repository.load();

        Ticket ticket = lot.findTicketById(ticketId)
            .orElseThrow(() -> new TicketNotFoundException("Ticket not found: " + ticketId));

        if (ticket.getStatus() == TicketStatus.PAID) {
            throw new ParkingException("Ticket " + ticketId + " has already been settled.");
        }

        LocalDateTime exitTime = LocalDateTime.now();
        long durationMinutes = Duration.between(ticket.getEntryTime(), exitTime).toMinutes();
        if (durationMinutes < 1) durationMinutes = 1;

        PricingStrategy strategy = pricingStrategyFactory.getStrategy(ticket.getVehicle().getType());
        double amount = strategy.calculate(durationMinutes);

        ticket.setExitTime(exitTime);
        ticket.setStatus(TicketStatus.PAID);
        ticket.setAmount(amount);

        lot.findSpotById(ticket.getSpotId()).ifPresent(spot -> {
            spot.setOccupied(false);
            spot.setVehicleId(null);
        });

        repository.save(lot);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ticketId", ticket.getId());
        result.put("licensePlate", ticket.getVehicle().getLicensePlate());
        result.put("vehicleType", ticket.getVehicle().getType());
        result.put("floor", ticket.getFloorNumber());
        result.put("spot", ticket.getSpotNumber());
        result.put("entryTime", ticket.getEntryTime());
        result.put("exitTime", exitTime);
        result.put("durationMinutes", durationMinutes);
        result.put("pricingRate", strategy.getDescription());
        result.put("amountDue", String.format("$%.2f", amount));
        result.put("status", "PAID");
        return result;
    }

    public Ticket getTicket(String ticketId) {
        return repository.load()
            .findTicketById(ticketId)
            .orElseThrow(() -> new TicketNotFoundException("Ticket not found: " + ticketId));
    }

    public List<Ticket> getActiveTickets() {
        return repository.load().getTickets().stream()
            .filter(t -> t.getStatus() == TicketStatus.ACTIVE)
            .toList();
    }

    public Map<String, Object> getParkingStatus() {
        ParkingLotData lot = repository.load();

        Map<String, Object> status = new LinkedHashMap<>();
        status.put("parkingLotId", lot.getId());
        status.put("name", lot.getName());
        status.put("address", lot.getAddress());
        status.put("totalSpots", lot.getTotalSpots());
        status.put("availableSpots", lot.getAvailableSpots());
        status.put("occupiedSpots", lot.getOccupiedSpots());
        status.put("occupancyRate",
            lot.getTotalSpots() == 0 ? "0%"
            : String.format("%.1f%%", (lot.getOccupiedSpots() * 100.0 / lot.getTotalSpots())));
        status.put("activeTickets",
            lot.getTickets().stream().filter(t -> t.getStatus() == TicketStatus.ACTIVE).count());

        List<Map<String, Object>> floorList = new ArrayList<>();
        for (ParkingFloor floor : lot.getFloors()) {
            Map<String, Object> f = new LinkedHashMap<>();
            f.put("floorId", floor.getId());
            f.put("floorNumber", floor.getFloorNumber());
            f.put("totalSpots", floor.getSpots().size());
            f.put("availableSpots", floor.countAvailableSpots());
            f.put("occupiedSpots", floor.countOccupiedSpots());

            Map<String, Long> byType = new LinkedHashMap<>();
            for (SpotType type : SpotType.values()) {
                long avail = floor.getSpots().stream()
                    .filter(s -> s.getType() == type && !s.isOccupied()).count();
                long total = floor.getSpots().stream()
                    .filter(s -> s.getType() == type).count();
                byType.put(type.name() + "_available", avail);
                byType.put(type.name() + "_total", total);
            }
            f.put("spotsByType", byType);
            floorList.add(f);
        }
        status.put("floors", floorList);
        return status;
    }

    public ParkingLotData getAllData() {
        return repository.load();
    }

    public ParkingFloor addFloor(int floorNumber) {
        ParkingLotData lot = repository.load();
        boolean exists = lot.getFloors().stream()
            .anyMatch(f -> f.getFloorNumber() == floorNumber);
        if (exists) {
            throw new ParkingException("Floor " + floorNumber + " already exists.");
        }
        ParkingFloor floor = new ParkingFloor(floorNumber);
        lot.getFloors().add(floor);
        lot.getFloors().sort(Comparator.comparingInt(ParkingFloor::getFloorNumber));
        repository.save(lot);
        return floor;
    }

    public ParkingSpot addSpot(String floorId, String spotNumber, SpotType spotType) {
        ParkingLotData lot = repository.load();
        ParkingFloor floor = lot.findFloorById(floorId)
            .orElseThrow(() -> new ParkingException("Floor not found: " + floorId));

        boolean spotExists = floor.getSpots().stream()
            .anyMatch(s -> s.getSpotNumber().equalsIgnoreCase(spotNumber));
        if (spotExists) {
            throw new ParkingException("Spot " + spotNumber + " already exists on this floor.");
        }

        ParkingSpot spot = new ParkingSpot(spotNumber, spotType);
        floor.getSpots().add(spot);
        repository.save(lot);
        return spot;
    }

    public Map<String, Object> getPricingInfo() {
        Map<String, Object> info = new LinkedHashMap<>();
        pricingStrategyFactory.getAllStrategies().forEach((type, strategy) ->
            info.put(type.name(), Map.of(
                "hourlyRate", strategy.getHourlyRate(),
                "description", strategy.getDescription()
            ))
        );
        return info;
    }
}
