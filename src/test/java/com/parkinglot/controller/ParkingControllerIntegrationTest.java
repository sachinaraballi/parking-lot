package com.parkinglot.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkinglot.dto.request.ParkVehicleRequest;
import com.parkinglot.model.VehicleType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.File;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "parking.data.file=data/test_parking_lot.json")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Parking Controller Integration Tests")
class ParkingControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // shared ticket ID captured across ordered tests
    static String ticketId;

    @BeforeAll
    static void cleanTestFile() {
        new File("data/test_parking_lot.json").delete();
    }

    // ── GET /api/parking/status ─────────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("GET /status → 200 with success=true and totalSpots=30")
    void getStatus_returns200() throws Exception {
        mockMvc.perform(get("/api/parking/status"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.totalSpots").value(30))
            .andExpect(jsonPath("$.data.floors", hasSize(3)));
    }

    // ── GET /api/parking/pricing ────────────────────────────────────────────

    @Test
    @Order(2)
    @DisplayName("GET /pricing → 200 with all three vehicle types")
    void getPricing_returnsAllTypes() throws Exception {
        mockMvc.perform(get("/api/parking/pricing"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.MOTORCYCLE").exists())
            .andExpect(jsonPath("$.data.CAR").exists())
            .andExpect(jsonPath("$.data.TRUCK").exists());
    }

    // ── POST /api/parking/entry ─────────────────────────────────────────────

    @Test
    @Order(3)
    @DisplayName("POST /entry → 201 with a valid ticket for a CAR")
    void parkVehicle_car_returns201() throws Exception {
        ParkVehicleRequest req = new ParkVehicleRequest();
        req.setLicensePlate("TEST-CAR-001");
        req.setVehicleType(VehicleType.CAR);

        MvcResult result = mockMvc.perform(post("/api/parking/entry")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id", startsWith("TKT-")))
            .andExpect(jsonPath("$.data.status").value("ACTIVE"))
            .andExpect(jsonPath("$.data.vehicle.licensePlate").value("TEST-CAR-001"))
            .andReturn();

        String body = result.getResponse().getContentAsString();
        ticketId = objectMapper.readTree(body).at("/data/id").asText();
    }

    @Test
    @Order(4)
    @DisplayName("POST /entry → 201 for MOTORCYCLE")
    void parkVehicle_motorcycle_returns201() throws Exception {
        ParkVehicleRequest req = new ParkVehicleRequest();
        req.setLicensePlate("TEST-MOTO-001");
        req.setVehicleType(VehicleType.MOTORCYCLE);

        mockMvc.perform(post("/api/parking/entry")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.spotType").value("SMALL"));
    }

    @Test
    @Order(5)
    @DisplayName("POST /entry with duplicate plate → 400")
    void parkVehicle_duplicate_returns400() throws Exception {
        ParkVehicleRequest req = new ParkVehicleRequest();
        req.setLicensePlate("TEST-CAR-001");
        req.setVehicleType(VehicleType.CAR);

        mockMvc.perform(post("/api/parking/entry")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message", containsString("already parked")));
    }

    @Test
    @Order(6)
    @DisplayName("POST /entry with missing licensePlate → 400 validation error")
    void parkVehicle_missingField_returns400() throws Exception {
        String body = """
            { "vehicleType": "CAR" }
            """;

        mockMvc.perform(post("/api/parking/entry")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @Order(7)
    @DisplayName("POST /entry with invalid vehicleType → 400")
    void parkVehicle_invalidVehicleType_returns400() throws Exception {
        String body = """
            { "licensePlate": "XYZ999", "vehicleType": "BICYCLE" }
            """;

        mockMvc.perform(post("/api/parking/entry")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest());
    }

    // ── GET /api/parking/ticket/:id ─────────────────────────────────────────

    @Test
    @Order(8)
    @DisplayName("GET /ticket/:id → 200 with correct ticket data")
    void getTicket_existingId_returns200() throws Exception {
        mockMvc.perform(get("/api/parking/ticket/{id}", ticketId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(ticketId))
            .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    @Order(9)
    @DisplayName("GET /ticket/:id with unknown ID → 404")
    void getTicket_unknownId_returns404() throws Exception {
        mockMvc.perform(get("/api/parking/ticket/TKT-NOSUCHID"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.success").value(false));
    }

    // ── GET /api/parking/active-tickets ────────────────────────────────────

    @Test
    @Order(10)
    @DisplayName("GET /active-tickets → 200 with non-empty list")
    void getActiveTickets_returns200() throws Exception {
        mockMvc.perform(get("/api/parking/active-tickets"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data", not(empty())));
    }

    // ── POST /api/parking/exit/:id ──────────────────────────────────────────

    @Test
    @Order(11)
    @DisplayName("POST /exit/:id → 200 with payment info")
    void exitVehicle_returns200WithPayment() throws Exception {
        mockMvc.perform(post("/api/parking/exit/{id}", ticketId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.status").value("PAID"))
            .andExpect(jsonPath("$.data.amountDue", startsWith("$")))
            .andExpect(jsonPath("$.data.durationMinutes").isNumber());
    }

    @Test
    @Order(12)
    @DisplayName("POST /exit/:id again (already paid) → 400")
    void exitVehicle_alreadyPaid_returns400() throws Exception {
        mockMvc.perform(post("/api/parking/exit/{id}", ticketId))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @Order(13)
    @DisplayName("POST /exit with unknown ticket ID → 404")
    void exitVehicle_unknownId_returns404() throws Exception {
        mockMvc.perform(post("/api/parking/exit/TKT-NOSUCHID"))
            .andExpect(status().isNotFound());
    }

    // ── Admin endpoints ─────────────────────────────────────────────────────

    @Test
    @Order(14)
    @DisplayName("GET /admin/parking-lot → 200 with full data")
    void adminGetParkingLot_returns200() throws Exception {
        mockMvc.perform(get("/api/admin/parking-lot"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").exists())
            .andExpect(jsonPath("$.data.floors").isArray());
    }

    @Test
    @Order(15)
    @DisplayName("POST /admin/floors → 201 creates new floor")
    void adminAddFloor_returns201() throws Exception {
        String body = """
            { "floorNumber": 10 }
            """;

        mockMvc.perform(post("/api/admin/floors")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.floorNumber").value(10))
            .andExpect(jsonPath("$.data.id").exists());
    }

    @Test
    @Order(16)
    @DisplayName("POST /admin/floors with duplicate number → 400")
    void adminAddFloor_duplicate_returns400() throws Exception {
        String body = """
            { "floorNumber": 1 }
            """;

        mockMvc.perform(post("/api/admin/floors")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest());
    }
}
