package com.SRTS.CAT.service;

import com.SRTS.CAT.entity.EquipmentEntry;
import com.SRTS.CAT.entity.EquipmentStatus;
import com.SRTS.CAT.repo.EquipmentRepo;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class EquipmentService {

    private final EquipmentRepo equipmentRepo;

    public EquipmentService(EquipmentRepo equipmentRepo) {
        this.equipmentRepo = equipmentRepo;
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

    public EquipmentEntry checkOut(String id, ObjectId siteId, ObjectId operatorId, String location) {
        EquipmentEntry equipment = getById(id);
        if (equipment.getStatus() == EquipmentStatus.RENTED) {
            throw new IllegalArgumentException("Equipment is already rented: " + id);
        }
        equipment.setStatus(EquipmentStatus.RENTED);
        equipment.setActiveState(true);
        equipment.setSiteId(siteId);
        equipment.setLastOperatorId(operatorId);
        equipment.setCurrentLocation(location);
        return equipmentRepo.save(equipment);
    }

    public EquipmentEntry checkIn(String id) {
        EquipmentEntry equipment = getById(id);
        if (equipment.getStatus() != EquipmentStatus.RENTED) {
            throw new IllegalArgumentException("Equipment is not currently rented: " + id);
        }
        equipment.setStatus(EquipmentStatus.AVAILABLE);
        equipment.setActiveState(false);
        return equipmentRepo.save(equipment);
    }

    public EquipmentEntry updateUsage(String id, Integer engineHoursPerDay, Integer idleHoursPerDay, Integer operatingDays) {
        EquipmentEntry equipment = getById(id);
        if (engineHoursPerDay != null) equipment.setEngineHoursPerDay(engineHoursPerDay);
        if (idleHoursPerDay != null) equipment.setIdleHoursPerDay(idleHoursPerDay);
        if (operatingDays != null) equipment.setOperatingDays(operatingDays);
        return equipmentRepo.save(equipment);
    }
}
