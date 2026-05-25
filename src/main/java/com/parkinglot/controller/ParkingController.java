package com.parkinglot.controller;

import com.parkinglot.dto.request.ParkVehicleRequest;
import com.parkinglot.dto.response.ApiResponse;
import com.parkinglot.model.Ticket;
import com.parkinglot.service.ParkingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/parking")
public class ParkingController {

    private final ParkingService parkingService;

    public ParkingController(ParkingService parkingService) {
        this.parkingService = parkingService;
    }

    /** Park a vehicle and receive a ticket. */
    @PostMapping("/entry")
    public ResponseEntity<ApiResponse<Ticket>> parkVehicle(@Valid @RequestBody ParkVehicleRequest request) {
        Ticket ticket = parkingService.parkVehicle(request.getLicensePlate(), request.getVehicleType());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Vehicle parked successfully.", ticket));
    }

    /** Exit the parking lot — calculates fee and frees the spot. */
    @PostMapping("/exit/{ticketId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> exitVehicle(@PathVariable String ticketId) {
        Map<String, Object> result = parkingService.exitVehicle(ticketId);
        return ResponseEntity.ok(ApiResponse.success("Payment successful. Safe drive!", result));
    }

    /** Fetch a ticket by ID (active or paid). */
    @GetMapping("/ticket/{ticketId}")
    public ResponseEntity<ApiResponse<Ticket>> getTicket(@PathVariable String ticketId) {
        return ResponseEntity.ok(ApiResponse.success(parkingService.getTicket(ticketId)));
    }

    /** Overall parking lot occupancy and per-floor breakdown. */
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStatus() {
        return ResponseEntity.ok(ApiResponse.success(parkingService.getParkingStatus()));
    }

    /** All currently active (unpaid) tickets. */
    @GetMapping("/active-tickets")
    public ResponseEntity<ApiResponse<List<Ticket>>> getActiveTickets() {
        return ResponseEntity.ok(ApiResponse.success(parkingService.getActiveTickets()));
    }

    /** Current pricing rates per vehicle type. */
    @GetMapping("/pricing")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPricing() {
        return ResponseEntity.ok(ApiResponse.success(parkingService.getPricingInfo()));
    }
}
