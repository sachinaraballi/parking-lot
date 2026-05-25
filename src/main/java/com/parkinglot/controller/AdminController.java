package com.parkinglot.controller;

import com.parkinglot.dto.request.AddFloorRequest;
import com.parkinglot.dto.request.AddSpotRequest;
import com.parkinglot.dto.response.ApiResponse;
import com.parkinglot.model.ParkingFloor;
import com.parkinglot.model.ParkingLotData;
import com.parkinglot.model.ParkingSpot;
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

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final ParkingService parkingService;

    public AdminController(ParkingService parkingService) {
        this.parkingService = parkingService;
    }

    /** Full raw view of the parking lot (all floors, spots, tickets). */
    @GetMapping("/parking-lot")
    public ResponseEntity<ApiResponse<ParkingLotData>> getParkingLot() {
        return ResponseEntity.ok(ApiResponse.success(parkingService.getAllData()));
    }

    /** Add a new floor to the parking lot. */
    @PostMapping("/floors")
    public ResponseEntity<ApiResponse<ParkingFloor>> addFloor(@Valid @RequestBody AddFloorRequest request) {
        ParkingFloor floor = parkingService.addFloor(request.getFloorNumber());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Floor " + request.getFloorNumber() + " added.", floor));
    }

    /** Add a new parking spot to an existing floor. */
    @PostMapping("/floors/{floorId}/spots")
    public ResponseEntity<ApiResponse<ParkingSpot>> addSpot(
            @PathVariable String floorId,
            @Valid @RequestBody AddSpotRequest request) {
        ParkingSpot spot = parkingService.addSpot(floorId, request.getSpotNumber(), request.getSpotType());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Spot " + request.getSpotNumber() + " added.", spot));
    }
}
