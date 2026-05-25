package com.parkinglot.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.parkinglot.model.ParkingLotData;
import com.parkinglot.model.SpotType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ParkingLotRepository Tests")
class ParkingLotRepositoryTest {

    @TempDir
    Path tempDir;

    private ParkingLotRepository repository;
    private String testFilePath;

    @BeforeEach
    void setUp() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        testFilePath = tempDir.resolve("test_parking_lot.json").toString();
        repository = new ParkingLotRepository(mapper, testFilePath);
    }

    @Test
    @DisplayName("load() auto-seeds a default lot when file does not exist")
    void load_createsDefaultLot_whenFileAbsent() {
        ParkingLotData lot = repository.load();

        assertThat(lot).isNotNull();
        assertThat(lot.getId()).isEqualTo("PL-001");
        assertThat(lot.getName()).isEqualTo("Central Parking");
        assertThat(lot.getFloors()).hasSize(3);
    }

    @Test
    @DisplayName("load() creates the JSON file on disk after seeding")
    void load_writesFileToDisk_afterSeeding() {
        repository.load();
        assertThat(new File(testFilePath)).exists();
    }

    @Test
    @DisplayName("Default lot has 30 total spots (5+5, 5+5, 10)")
    void load_defaultLot_has30Spots() {
        ParkingLotData lot = repository.load();
        assertThat(lot.getTotalSpots()).isEqualTo(30);
    }

    @Test
    @DisplayName("Default lot floor 1 contains SMALL and MEDIUM spots")
    void load_floor1_hasSmallAndMediumSpots() {
        ParkingLotData lot = repository.load();
        var floor1 = lot.getFloors().get(0);

        long small = floor1.getSpots().stream().filter(s -> s.getType() == SpotType.SMALL).count();
        long medium = floor1.getSpots().stream().filter(s -> s.getType() == SpotType.MEDIUM).count();

        assertThat(small).isEqualTo(5);
        assertThat(medium).isEqualTo(5);
    }

    @Test
    @DisplayName("save() then load() round-trips data correctly")
    void saveAndLoad_roundTrip() {
        ParkingLotData original = repository.load();
        original.setName("Updated Name");
        repository.save(original);

        ParkingLotData reloaded = repository.load();
        assertThat(reloaded.getName()).isEqualTo("Updated Name");
        assertThat(reloaded.getFloors()).hasSize(3);
    }

    @Test
    @DisplayName("All spots are available on fresh load")
    void load_allSpotsAvailable_initially() {
        ParkingLotData lot = repository.load();
        assertThat(lot.getAvailableSpots()).isEqualTo(lot.getTotalSpots());
        assertThat(lot.getOccupiedSpots()).isEqualTo(0);
    }
}
