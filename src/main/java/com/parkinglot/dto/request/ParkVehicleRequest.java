package com.parkinglot.dto.request;

import com.parkinglot.model.VehicleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ParkVehicleRequest {

    @NotBlank(message = "licensePlate is required")
    private String licensePlate;

    @NotNull(message = "vehicleType is required: MOTORCYCLE, CAR, or TRUCK")
    private VehicleType vehicleType;

    public String getLicensePlate() { return licensePlate; }
    public void setLicensePlate(String licensePlate) { this.licensePlate = licensePlate; }

    public VehicleType getVehicleType() { return vehicleType; }
    public void setVehicleType(VehicleType vehicleType) { this.vehicleType = vehicleType; }
}
