package com.parkinglot.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkinglot.model.ParkingFloor;
import com.parkinglot.model.ParkingLotData;
import com.parkinglot.model.ParkingSpot;
import com.parkinglot.model.SpotType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.File;
import java.io.IOException;

/**
 * Singleton repository (Spring default) that owns all reads and writes
 * to the JSON file database. Synchronized to be safe under concurrent requests.
 */
@Repository
public class ParkingLotRepository {

    private final ObjectMapper objectMapper;
    private final String filePath;

    public ParkingLotRepository(ObjectMapper objectMapper,
                                 @Value("${parking.data.file:data/parking_lot.json}") String filePath) {
        this.objectMapper = objectMapper;
        this.filePath = filePath;
    }

    public synchronized ParkingLotData load() {
        File file = new File(filePath);
        if (!file.exists()) {
            ParkingLotData defaultLot = buildDefaultParkingLot();
            save(defaultLot);
            return defaultLot;
        }
        try {
            return objectMapper.readValue(file, ParkingLotData.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load parking lot data from: " + filePath, e);
        }
    }

    public synchronized void save(ParkingLotData data) {
        File file = new File(filePath);
        file.getParentFile().mkdirs();
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, data);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save parking lot data to: " + filePath, e);
        }
    }

    private ParkingLotData buildDefaultParkingLot() {
        ParkingLotData lot = new ParkingLotData();
        lot.setId("PL-001");
        lot.setName("Central Parking");
        lot.setAddress("123 Main Street, Downtown");

        // Floor 1: small & medium spots (suits motorcycles and cars)
        ParkingFloor floor1 = new ParkingFloor(1);
        addSpots(floor1, 5, SpotType.SMALL,  "F1-S");
        addSpots(floor1, 5, SpotType.MEDIUM, "F1-M");
        lot.getFloors().add(floor1);

        // Floor 2: medium & large spots (suits cars and trucks)
        ParkingFloor floor2 = new ParkingFloor(2);
        addSpots(floor2, 5, SpotType.MEDIUM, "F2-M");
        addSpots(floor2, 5, SpotType.LARGE,  "F2-L");
        lot.getFloors().add(floor2);

        // Floor 3: large spots only (suits trucks)
        ParkingFloor floor3 = new ParkingFloor(3);
        addSpots(floor3, 10, SpotType.LARGE, "F3-L");
        lot.getFloors().add(floor3);

        return lot;
    }

    private void addSpots(ParkingFloor floor, int count, SpotType type, String prefix) {
        for (int i = 1; i <= count; i++) {
            floor.getSpots().add(new ParkingSpot(prefix + i, type));
        }
    }
}
