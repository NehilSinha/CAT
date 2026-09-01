package com.SRTS.CAT.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class EquipmentHistoryView {
    private String equipmentId;
    private String equipmentCode;
    private List<Integer> engineHoursHistory;
    private List<Integer> idleHoursHistory;
    private List<Integer> fuelLevelHistory;
    private double averageEngineHours;
    private double averageIdleHours;
    private double averageFuelLevel;
}
