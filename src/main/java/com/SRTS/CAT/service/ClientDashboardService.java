package com.SRTS.CAT.service;

import com.SRTS.CAT.dto.ClientEquipmentView;
import com.SRTS.CAT.dto.FleetSummary;
import com.SRTS.CAT.entity.EquipmentEntry;
import com.SRTS.CAT.repo.EquipmentRepo;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ClientDashboardService {

    private final EquipmentRepo equipmentRepo;
    private final EquipmentCalculationService calculationService;

    public ClientDashboardService(EquipmentRepo equipmentRepo, EquipmentCalculationService calculationService) {
        this.equipmentRepo = equipmentRepo;
        this.calculationService = calculationService;
    }

    public List<ClientEquipmentView> getEquipmentForClient(String clientId) {
        return equipmentRepo.findByClientId(clientId).stream()
                .map(this::toView)
                .collect(Collectors.toList());
    }

    public FleetSummary getFleetSummary(String clientId) {
        List<EquipmentEntry> rentedFleet = equipmentRepo.findByClientId(clientId);

        List<FleetSummary.UnderutilizedEquipment> underutilized = rentedFleet.stream()
                .filter(calculationService::isLikelyUnused)
                .map(e -> new FleetSummary.UnderutilizedEquipment(
                        e.getId(), e.getEquipmentCode(), calculationService.unusedReason(e)))
                .collect(Collectors.toList());

        int total = rentedFleet.size();
        int underutilizedCount = underutilized.size();
        return new FleetSummary(clientId, total, total - underutilizedCount, underutilizedCount, underutilized);
    }

    private ClientEquipmentView toView(EquipmentEntry equipment) {
        return new ClientEquipmentView(
                equipment.getId(),
                equipment.getEquipmentCode(),
                equipment.getType(),
                equipment.getStatus(),
                equipment.getCurrentLocation(),
                equipment.getLastOperatorId(),
                equipment.getEngineHoursPerDay(),
                equipment.getIdleHoursPerDay(),
                equipment.getOperatingDays(),
                equipment.getCheckOutDate(),
                equipment.getExpectedReturnDate(),
                equipment.getEngineTemperature(),
                equipment.getFuelLevel(),
                equipment.getSeatbeltEngaged(),
                calculationService.calculateUtilization(equipment),
                calculationService.isOverdue(equipment),
                calculationService.isOverheating(equipment),
                calculationService.isLowFuel(equipment),
                calculationService.isSeatbeltViolation(equipment),
                calculationService.isIdleHoursAnomaly(equipment),
                calculationService.isUnassignedUse(equipment),
                equipment.getEngineHoursHistory(),
                equipment.getIdleHoursHistory(),
                equipment.getFuelLevelHistory(),
                calculationService.averageEngineHours(equipment),
                calculationService.averageIdleHours(equipment),
                calculationService.averageFuelLevel(equipment)
        );
    }
}
