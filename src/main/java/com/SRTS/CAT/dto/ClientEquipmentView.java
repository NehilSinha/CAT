package com.SRTS.CAT.dto;

import com.SRTS.CAT.entity.EquipmentStatus;
import com.SRTS.CAT.entity.EquipmentType;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
public class ClientEquipmentView {
    private String equipmentId;
    private String equipmentCode;
    private EquipmentType type;
    private EquipmentStatus status;
    private String currentLocation;
    private String lastOperatorId;
    private Integer engineHoursPerDay;
    private Integer idleHoursPerDay;
    private Integer operatingDays;
    private LocalDate checkOutDate;
    private LocalDate expectedReturnDate;
    private Double engineTemperature;
    private Integer fuelLevel;
    private Boolean seatbeltEngaged;
    private double utilization;
    private boolean overdue;
    private boolean overheating;
    private boolean lowFuel;
    private boolean seatbeltViolation;
    private boolean idleAnomaly;
    private boolean unassignedUse;
    private List<Integer> engineHoursHistory;
    private List<Integer> idleHoursHistory;
    private List<Integer> fuelLevelHistory;
    private double averageEngineHours;
    private double averageIdleHours;
    private double averageFuelLevel;
}
