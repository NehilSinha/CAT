package com.SRTS.CAT.controller;

import com.SRTS.CAT.dto.BatchCheckoutRequest;
import com.SRTS.CAT.dto.BatchCheckoutResult;
import com.SRTS.CAT.dto.EquipmentHistoryView;
import com.SRTS.CAT.dto.TelemetryUpdateRequest;
import com.SRTS.CAT.entity.EquipmentEntry;
import com.SRTS.CAT.service.EquipmentCalculationService;
import com.SRTS.CAT.service.EquipmentService;
import org.bson.types.ObjectId;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/equipment")
public class EquipmentController {

    private final EquipmentService equipmentService;
    private final EquipmentCalculationService calculationService;

    public EquipmentController(EquipmentService equipmentService, EquipmentCalculationService calculationService) {
        this.equipmentService = equipmentService;
        this.calculationService = calculationService;
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
        LocalDate expectedReturnDate = body.get("expectedReturnDate") != null
                ? LocalDate.parse(body.get("expectedReturnDate"))
                : null;
        String clientId = body.get("clientId");
        return equipmentService.checkOut(id, siteId, operatorId, location, expectedReturnDate, clientId);
    }

    @PatchMapping("/checkout-batch")
    public BatchCheckoutResult checkOutBatch(@RequestBody BatchCheckoutRequest request) {
        ObjectId siteId = request.getSiteId() != null ? new ObjectId(request.getSiteId()) : null;
        ObjectId operatorId = request.getOperatorId() != null ? new ObjectId(request.getOperatorId()) : null;
        LocalDate expectedReturnDate = request.getExpectedReturnDate() != null
                ? LocalDate.parse(request.getExpectedReturnDate())
                : null;
        return equipmentService.checkOutBatch(
                request.getEquipmentIds(),
                siteId,
                operatorId,
                request.getLocation(),
                expectedReturnDate,
                request.getClientId()
        );
    }

    @PatchMapping("/{id}/checkin")
    public EquipmentEntry checkIn(@PathVariable String id) {
        return equipmentService.checkIn(id);
    }

    @PatchMapping("/{id}/maintenance/start")
    public EquipmentEntry startMaintenance(@PathVariable String id) {
        return equipmentService.startMaintenance(id);
    }

    @PatchMapping("/{id}/maintenance/end")
    public EquipmentEntry endMaintenance(@PathVariable String id) {
        return equipmentService.endMaintenance(id);
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

    @PatchMapping("/{id}/telemetry")
    public EquipmentEntry updateTelemetry(@PathVariable String id, @RequestBody TelemetryUpdateRequest request) {
        return equipmentService.updateTelemetry(
                id,
                request.getEngineTemperature(),
                request.getFuelLevel(),
                request.getSeatbeltEngaged()
        );
    }

    @GetMapping("/{id}/history")
    public EquipmentHistoryView getHistory(@PathVariable String id) {
        return toHistoryView(equipmentService.getById(id));
    }

    @GetMapping("/history")
    public List<EquipmentHistoryView> getAllHistory() {
        return equipmentService.getAll().stream()
                .map(this::toHistoryView)
                .collect(Collectors.toList());
    }

    private EquipmentHistoryView toHistoryView(EquipmentEntry equipment) {
        return new EquipmentHistoryView(
                equipment.getId(),
                equipment.getEquipmentCode(),
                equipment.getEngineHoursHistory(),
                equipment.getIdleHoursHistory(),
                equipment.getFuelLevelHistory(),
                calculationService.averageEngineHours(equipment),
                calculationService.averageIdleHours(equipment),
                calculationService.averageFuelLevel(equipment)
        );
    }
}
