package com.parkinglot.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class AddFloorRequest {

    @NotNull(message = "floorNumber is required")
    @Min(value = 1, message = "floorNumber must be >= 1")
    private Integer floorNumber;

    public Integer getFloorNumber() { return floorNumber; }
    public void setFloorNumber(Integer floorNumber) { this.floorNumber = floorNumber; }
}
