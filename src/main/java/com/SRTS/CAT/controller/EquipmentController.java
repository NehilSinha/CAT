package com.SRTS.CAT.controller;

import com.SRTS.CAT.entity.EquipmentEntry;
import com.SRTS.CAT.service.EquipmentService;
import org.bson.types.ObjectId;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/equipment")
public class EquipmentController {

    private final EquipmentService equipmentService;

    public EquipmentController(EquipmentService equipmentService) {
        this.equipmentService = equipmentService;
    }

    @GetMapping
    public List<EquipmentEntry> getAll() {
        return equipmentService.getAll();
    }

    @GetMapping("/{id}")
    public EquipmentEntry getById(@PathVariable String id) {
        return equipmentService.getById(id);
    }

    @PostMapping
    public EquipmentEntry create(@RequestBody EquipmentEntry equipment) {
        return equipmentService.create(equipment);
    }

    @PatchMapping("/{id}/checkout")
    public EquipmentEntry checkOut(@PathVariable String id, @RequestBody Map<String, String> body) {
        ObjectId siteId = body.get("siteId") != null ? new ObjectId(body.get("siteId")) : null;
        ObjectId operatorId = body.get("operatorId") != null ? new ObjectId(body.get("operatorId")) : null;
        String location = body.get("location");
        return equipmentService.checkOut(id, siteId, operatorId, location);
    }

    @PatchMapping("/{id}/checkin")
    public EquipmentEntry checkIn(@PathVariable String id) {
        return equipmentService.checkIn(id);
    }

    @PatchMapping("/{id}/usage")
    public EquipmentEntry updateUsage(@PathVariable String id, @RequestBody Map<String, Integer> body) {
        return equipmentService.updateUsage(
                id,
                body.get("engineHoursPerDay"),
                body.get("idleHoursPerDay"),
                body.get("operatingDays")
        );
    }
}
