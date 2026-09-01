package com.SRTS.CAT.service;

import com.SRTS.CAT.dto.EquipmentAlert;
import com.SRTS.CAT.entity.EquipmentEntry;
import com.SRTS.CAT.repo.EquipmentRepo;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AlertService {

    private final EquipmentRepo equipmentRepo;
    private final EquipmentCalculationService calculationService;

    public AlertService(EquipmentRepo equipmentRepo, EquipmentCalculationService calculationService) {
        this.equipmentRepo = equipmentRepo;
        this.calculationService = calculationService;
    }

    public List<EquipmentAlert> getAlerts() {
        return equipmentRepo.findAll().stream()
                .map(this::toAlert)
                .filter(alert -> alert.isOverdue() || alert.isUpcomingReturn() || alert.isUnderutilized()
                        || alert.isOverheating() || alert.isLowFuel() || alert.isSeatbeltViolation()
                        || alert.isIdleAnomaly() || alert.isUnassignedUse())
                .collect(Collectors.toList());
    }

    private EquipmentAlert toAlert(EquipmentEntry equipment) {
        return new EquipmentAlert(
                equipment.getId(),
                equipment.getEquipmentCode(),
                calculationService.isOverdue(equipment),
                calculationService.isUpcomingReturn(equipment),
                calculationService.isUnderutilized(equipment),
                calculationService.isOverheating(equipment),
                calculationService.isLowFuel(equipment),
                calculationService.isSeatbeltViolation(equipment),
                calculationService.isIdleHoursAnomaly(equipment),
                calculationService.isUnassignedUse(equipment),
                calculationService.calculateUtilization(equipment)
        );
    }
}
