package com.parkinglot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.parkinglot.exception.NoSpotAvailableException;
import com.parkinglot.exception.ParkingException;
import com.parkinglot.exception.TicketNotFoundException;
import com.parkinglot.factory.PricingStrategyFactory;
import com.parkinglot.model.Ticket;
import com.parkinglot.model.TicketStatus;
import com.parkinglot.model.VehicleType;
import com.parkinglot.repository.ParkingLotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ParkingService Tests")
class ParkingServiceTest {

    @TempDir
    Path tempDir;

    private ParkingService service;

    @BeforeEach
    void setUp() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        String filePath = tempDir.resolve("parking.json").toString();
        ParkingLotRepository repo = new ParkingLotRepository(mapper, filePath);
        service = new ParkingService(repo, new PricingStrategyFactory());
    }

    // ── Park vehicle ────────────────────────────────────────────────────────

    @Test
    @DisplayName("Parking a car returns an ACTIVE ticket with correct vehicle info")
    void parkVehicle_car_returnsActiveTicket() {
        Ticket ticket = service.parkVehicle("MH12AB1234", VehicleType.CAR);

        assertThat(ticket.getId()).startsWith("TKT-");
        assertThat(ticket.getVehicle().getLicensePlate()).isEqualTo("MH12AB1234");
        assertThat(ticket.getVehicle().getType()).isEqualTo(VehicleType.CAR);
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.ACTIVE);
        assertThat(ticket.getEntryTime()).isNotNull();
        assertThat(ticket.getAmount()).isNull();
    }

    @Test
    @DisplayName("Parking a motorcycle assigns a SMALL spot first (smallest-fit rule)")
    void parkVehicle_motorcycle_assignsSmallestSuitableSpot() {
        Ticket ticket = service.parkVehicle("KA01EF5678", VehicleType.MOTORCYCLE);
        assertThat(ticket.getSpotType().name()).isEqualTo("SMALL");
    }

    @Test
    @DisplayName("Parking a truck assigns a LARGE spot")
    void parkVehicle_truck_assignsLargeSpot() {
        Ticket ticket = service.parkVehicle("DL03CD9999", VehicleType.TRUCK);
        assertThat(ticket.getSpotType().name()).isEqualTo("LARGE");
    }

    @Test
    @DisplayName("License plate is normalised to uppercase")
    void parkVehicle_licensePlate_isUppercased() {
        Ticket ticket = service.parkVehicle("mh12ab1234", VehicleType.CAR);
        assertThat(ticket.getVehicle().getLicensePlate()).isEqualTo("MH12AB1234");
    }

    @Test
    @DisplayName("Parking an already-parked vehicle throws ParkingException")
    void parkVehicle_duplicate_throwsParkingException() {
        service.parkVehicle("MH12AB1234", VehicleType.CAR);

        assertThatThrownBy(() -> service.parkVehicle("MH12AB1234", VehicleType.CAR))
            .isInstanceOf(ParkingException.class)
            .hasMessageContaining("already parked");
    }

    @Test
    @DisplayName("Multiple different vehicles can be parked simultaneously")
    void parkVehicle_multiple_allParked() {
        service.parkVehicle("AA0001", VehicleType.CAR);
        service.parkVehicle("AA0002", VehicleType.CAR);
        service.parkVehicle("AA0003", VehicleType.MOTORCYCLE);

        List<Ticket> active = service.getActiveTickets();
        assertThat(active).hasSize(3);
    }

    @Test
    @DisplayName("Parking reduces available spot count by 1")
    void parkVehicle_reducesAvailableSpots() {
        Map<String, Object> before = service.getParkingStatus();
        long availBefore = (long) before.get("availableSpots");

        service.parkVehicle("MH12AB1234", VehicleType.CAR);

        Map<String, Object> after = service.getParkingStatus();
        long availAfter = (long) after.get("availableSpots");

        assertThat(availAfter).isEqualTo(availBefore - 1);
    }

    // ── Exit vehicle ────────────────────────────────────────────────────────

    @Test
    @DisplayName("Exiting a vehicle marks ticket as PAID and returns amount")
    void exitVehicle_marksTicketPaid() {
        Ticket ticket = service.parkVehicle("MH12AB1234", VehicleType.CAR);
        Map<String, Object> result = service.exitVehicle(ticket.getId());

        assertThat(result.get("status")).isEqualTo("PAID");
        assertThat(result.get("amountDue").toString()).startsWith("$");
        assertThat(result.get("durationMinutes")).isNotNull();
    }

    @Test
    @DisplayName("Exiting frees the spot (available count returns to original)")
    void exitVehicle_freesSpot() {
        long totalBefore = (long) service.getParkingStatus().get("availableSpots");

        Ticket ticket = service.parkVehicle("MH12AB1234", VehicleType.CAR);
        service.exitVehicle(ticket.getId());

        long totalAfter = (long) service.getParkingStatus().get("availableSpots");
        assertThat(totalAfter).isEqualTo(totalBefore);
    }

    @Test
    @DisplayName("Exiting removes ticket from active list")
    void exitVehicle_removesFromActiveTickets() {
        Ticket ticket = service.parkVehicle("MH12AB1234", VehicleType.CAR);
        assertThat(service.getActiveTickets()).hasSize(1);

        service.exitVehicle(ticket.getId());
        assertThat(service.getActiveTickets()).isEmpty();
    }

    @Test
    @DisplayName("Exiting an already-paid ticket throws ParkingException")
    void exitVehicle_alreadyPaid_throwsException() {
        Ticket ticket = service.parkVehicle("MH12AB1234", VehicleType.CAR);
        service.exitVehicle(ticket.getId());

        assertThatThrownBy(() -> service.exitVehicle(ticket.getId()))
            .isInstanceOf(ParkingException.class)
            .hasMessageContaining("already been settled");
    }

    @Test
    @DisplayName("Exiting with an unknown ticket ID throws TicketNotFoundException")
    void exitVehicle_unknownId_throwsNotFoundException() {
        assertThatThrownBy(() -> service.exitVehicle("TKT-DOESNOTEXIST"))
            .isInstanceOf(TicketNotFoundException.class);
    }

    // ── Get ticket ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("getTicket returns the correct ticket by ID")
    void getTicket_returnsCorrectTicket() {
        Ticket parked = service.parkVehicle("MH12AB1234", VehicleType.CAR);
        Ticket fetched = service.getTicket(parked.getId());
        assertThat(fetched.getId()).isEqualTo(parked.getId());
    }

    @Test
    @DisplayName("getTicket with unknown ID throws TicketNotFoundException")
    void getTicket_unknownId_throwsException() {
        assertThatThrownBy(() -> service.getTicket("TKT-INVALID"))
            .isInstanceOf(TicketNotFoundException.class);
    }

    // ── Parking status ──────────────────────────────────────────────────────

    @Test
    @DisplayName("getParkingStatus returns lot name and 30 total spots")
    void getParkingStatus_containsExpectedFields() {
        Map<String, Object> status = service.getParkingStatus();

        assertThat(status).containsKey("name");
        assertThat(status.get("totalSpots")).isEqualTo(30L);
        assertThat(status).containsKey("floors");
        assertThat(status).containsKey("occupancyRate");
    }

    @Test
    @DisplayName("Fresh lot has 0% occupancy rate")
    void getParkingStatus_freshLot_zeroOccupancy() {
        Map<String, Object> status = service.getParkingStatus();
        assertThat(status.get("occupancyRate")).isEqualTo("0.0%");
    }

    // ── Admin operations ────────────────────────────────────────────────────

    @Test
    @DisplayName("addFloor creates a new floor with correct number")
    void addFloor_createsFloorSuccessfully() {
        var floor = service.addFloor(4);
        assertThat(floor.getFloorNumber()).isEqualTo(4);
        assertThat(floor.getId()).isNotNull();
    }

    @Test
    @DisplayName("addFloor with duplicate number throws ParkingException")
    void addFloor_duplicate_throwsException() {
        assertThatThrownBy(() -> service.addFloor(1))
            .isInstanceOf(ParkingException.class)
            .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("addSpot adds a spot to the specified floor")
    void addSpot_addsSpotToFloor() {
        var floor = service.addFloor(4);
        var spot = service.addSpot(floor.getId(), "F4-L1", com.parkinglot.model.SpotType.LARGE);

        assertThat(spot.getSpotNumber()).isEqualTo("F4-L1");
        assertThat(spot.getType()).isEqualTo(com.parkinglot.model.SpotType.LARGE);
        assertThat(spot.isOccupied()).isFalse();
    }

    @Test
    @DisplayName("addSpot with unknown floorId throws ParkingException")
    void addSpot_unknownFloor_throwsException() {
        assertThatThrownBy(() -> service.addSpot("nonexistent-id", "S1", com.parkinglot.model.SpotType.SMALL))
            .isInstanceOf(ParkingException.class)
            .hasMessageContaining("Floor not found");
    }

    // ── No spot available ───────────────────────────────────────────────────

    @Test
    @DisplayName("Parking more cars than MEDIUM+LARGE spots available throws NoSpotAvailableException")
    void parkVehicle_noSpotLeft_throwsNoSpotAvailableException() {
        // Floor1: 5 MEDIUM, Floor2: 5 MEDIUM+5 LARGE, Floor3: 10 LARGE → 25 CAR-compatible spots
        for (int i = 1; i <= 25; i++) {
            service.parkVehicle("CAR" + i, VehicleType.CAR);
        }
        assertThatThrownBy(() -> service.parkVehicle("CAR26", VehicleType.CAR))
            .isInstanceOf(NoSpotAvailableException.class);
    }

    // ── Pricing info ────────────────────────────────────────────────────────

    @Test
    @DisplayName("getPricingInfo returns entries for all three vehicle types")
    void getPricingInfo_coversAllTypes() {
        Map<String, Object> pricing = service.getPricingInfo();
        assertThat(pricing).containsKeys("MOTORCYCLE", "CAR", "TRUCK");
    }
}
