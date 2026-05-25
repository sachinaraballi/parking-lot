package com.parkinglot.dto.request;

import com.parkinglot.model.SpotType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class AddSpotRequest {

    @NotBlank(message = "spotNumber is required")
    private String spotNumber;

    @NotNull(message = "spotType is required: SMALL, MEDIUM, or LARGE")
    private SpotType spotType;

    public String getSpotNumber() { return spotNumber; }
    public void setSpotNumber(String spotNumber) { this.spotNumber = spotNumber; }

    public SpotType getSpotType() { return spotType; }
    public void setSpotType(SpotType spotType) { this.spotType = spotType; }
}
