package com.SRTS.CAT.service;

import com.SRTS.CAT.dto.BatchCheckoutResult;
import com.SRTS.CAT.entity.EquipmentEntry;
import com.SRTS.CAT.entity.EquipmentStatus;
import com.SRTS.CAT.repo.ClientRepo;
import com.SRTS.CAT.repo.EquipmentRepo;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@Service
public class EquipmentService {

    private static final String YARD_LOCATION = "CAT Yard";

    private final EquipmentRepo equipmentRepo;
    private final ClientRepo clientRepo;

    public EquipmentService(EquipmentRepo equipmentRepo, ClientRepo clientRepo) {
        this.equipmentRepo = equipmentRepo;
        this.clientRepo = clientRepo;
    }

    public EquipmentEntry create(EquipmentEntry equipment) {
        equipment.setStatus(EquipmentStatus.AVAILABLE);
        equipment.setActiveState(true);
        return equipmentRepo.save(equipment);
    }

    public List<EquipmentEntry> getAll() {
        return equipmentRepo.findAll();
    }

    public EquipmentEntry getById(String id) {
        return equipmentRepo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Equipment not found: " + id));
    }

    public EquipmentEntry checkOut(String id, ObjectId siteId, ObjectId operatorId, String location, LocalDate expectedReturnDate, String clientId) {
        EquipmentEntry equipment = getById(id);
        if (equipment.getStatus() == EquipmentStatus.RENTED) {
            throw new IllegalArgumentException("Equipment is already rented: " + id);
        }
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalArgumentException("clientId is required to check out equipment");
        }
        if (!clientRepo.existsById(clientId)) {
            throw new IllegalArgumentException("Client not found: " + clientId);
        }
        if (expectedReturnDate == null) {
            throw new IllegalArgumentException("expectedReturnDate is required to check out equipment");
        }
        equipment.setStatus(EquipmentStatus.RENTED);
        equipment.setActiveState(true);
        equipment.setSiteId(siteId);
        equipment.setLastOperatorId(operatorId);
        equipment.setCurrentLocation(location);
        equipment.setCheckOutDate(LocalDate.now());
        equipment.setExpectedReturnDate(expectedReturnDate);
        equipment.setClientId(clientId);
        return equipmentRepo.save(equipment);
    }

    public BatchCheckoutResult checkOutBatch(List<String> equipmentIds, ObjectId siteId, ObjectId operatorId, String location, LocalDate expectedReturnDate, String clientId) {
        List<EquipmentEntry> checkedOut = new ArrayList<>();
        Map<String, String> failed = new LinkedHashMap<>();
        for (String equipmentId : equipmentIds) {
            try {
                checkedOut.add(checkOut(equipmentId, siteId, operatorId, location, expectedReturnDate, clientId));
            } catch (RuntimeException e) {
                failed.put(equipmentId, e.getMessage());
            }
        }
        return new BatchCheckoutResult(checkedOut, failed);
    }

    public EquipmentEntry checkIn(String id) {
        EquipmentEntry equipment = getById(id);
        if (equipment.getStatus() != EquipmentStatus.RENTED) {
            throw new IllegalArgumentException("Equipment is not currently rented: " + id);
        }
        equipment.setStatus(EquipmentStatus.AVAILABLE);
        equipment.setActiveState(false);
        equipment.setClientId(null);
        equipment.setCurrentLocation(YARD_LOCATION);
        equipment.setCheckInDate(LocalDate.now());
        return equipmentRepo.save(equipment);
    }

    public EquipmentEntry startMaintenance(String id) {
        EquipmentEntry equipment = getById(id);
        if (equipment.getStatus() != EquipmentStatus.AVAILABLE) {
            throw new IllegalArgumentException("Only available equipment can be sent to maintenance: " + id);
        }
        equipment.setStatus(EquipmentStatus.MAINTENANCE);
        equipment.setActiveState(false);
        // Reset to a clean, freshly-serviced baseline so old sensor readings
        // don't keep tripping alerts (overheating/low-fuel/idle-anomaly) once
        // this machine is back in service.
        equipment.setEngineTemperature(25.0);
        equipment.setFuelLevel(100);
        equipment.setSeatbeltEngaged(true);
        equipment.setEngineHoursPerDay(0);
        equipment.setIdleHoursPerDay(0);
        equipment.getEngineHoursHistory().clear();
        equipment.getIdleHoursHistory().clear();
        equipment.getFuelLevelHistory().clear();
        return equipmentRepo.save(equipment);
    }

    public EquipmentEntry endMaintenance(String id) {
        EquipmentEntry equipment = getById(id);
        if (equipment.getStatus() != EquipmentStatus.MAINTENANCE) {
            throw new IllegalArgumentException("Equipment is not currently in maintenance: " + id);
        }
        equipment.setStatus(EquipmentStatus.AVAILABLE);
        return equipmentRepo.save(equipment);
    }

    public EquipmentEntry updateUsage(String id, Integer engineHoursPerDay, Integer idleHoursPerDay, Integer operatingDays) {
        EquipmentEntry equipment = getById(id);
        if (engineHoursPerDay != null) {
            equipment.setEngineHoursPerDay(engineHoursPerDay);
            equipment.getEngineHoursHistory().add(engineHoursPerDay);
        }
        if (idleHoursPerDay != null) {
            equipment.setIdleHoursPerDay(idleHoursPerDay);
            equipment.getIdleHoursHistory().add(idleHoursPerDay);
        }
        if (operatingDays != null) equipment.setOperatingDays(operatingDays);
        return equipmentRepo.save(equipment);
    }

    public EquipmentEntry updateTelemetry(String id, Double engineTemperature, Integer fuelLevel, Boolean seatbeltEngaged) {
        EquipmentEntry equipment = getById(id);
        if (engineTemperature != null) equipment.setEngineTemperature(engineTemperature);
        if (fuelLevel != null) {
            equipment.setFuelLevel(fuelLevel);
            equipment.getFuelLevelHistory().add(fuelLevel);
        }
        if (seatbeltEngaged != null) equipment.setSeatbeltEngaged(seatbeltEngaged);
        return equipmentRepo.save(equipment);
    }
}
