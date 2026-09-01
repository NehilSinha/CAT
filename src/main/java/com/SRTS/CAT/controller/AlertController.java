package com.SRTS.CAT.controller;

import com.SRTS.CAT.dto.EquipmentAlert;
import com.SRTS.CAT.service.AlertService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping
    public List<EquipmentAlert> getAlerts() {
        return alertService.getAlerts();
    }
}
