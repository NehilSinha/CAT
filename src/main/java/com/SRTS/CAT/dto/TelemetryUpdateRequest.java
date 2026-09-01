package com.SRTS.CAT.dto;

import lombok.Data;

@Data
public class TelemetryUpdateRequest {
    private Double engineTemperature;
    private Integer fuelLevel;
    private Boolean seatbeltEngaged;
}
