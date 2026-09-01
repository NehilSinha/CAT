package com.SRTS.CAT.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EquipmentAlert {
    private String equipmentId;
    private String equipmentCode;
    private boolean overdue;
    private boolean upcomingReturn;
    private boolean underutilized;
    private boolean overheating;
    private boolean lowFuel;
    private boolean seatbeltViolation;
    private boolean idleAnomaly;
    private boolean unassignedUse;
    private double utilization;
}
