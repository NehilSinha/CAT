package com.SRTS.CAT.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class FleetSummary {
    private String clientId;
    private int totalRented;
    private int activelyUsed;
    private int underutilizedCount;
    private List<UnderutilizedEquipment> underutilizedEquipment;

    @Data
    @AllArgsConstructor
    public static class UnderutilizedEquipment {
        private String equipmentId;
        private String equipmentCode;
        private String reason;
    }
}
