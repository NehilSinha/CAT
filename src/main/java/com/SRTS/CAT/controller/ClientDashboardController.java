package com.SRTS.CAT.controller;

import com.SRTS.CAT.dto.ClientEquipmentView;
import com.SRTS.CAT.dto.FleetSummary;
import com.SRTS.CAT.service.ClientDashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/clients/{clientId}")
public class ClientDashboardController {

    private final ClientDashboardService clientDashboardService;

    public ClientDashboardController(ClientDashboardService clientDashboardService) {
        this.clientDashboardService = clientDashboardService;
    }

    @GetMapping("/equipment")
    public List<ClientEquipmentView> getEquipmentForClient(@PathVariable String clientId) {
        return clientDashboardService.getEquipmentForClient(clientId);
    }

    @GetMapping("/fleet-summary")
    public FleetSummary getFleetSummary(@PathVariable String clientId) {
        return clientDashboardService.getFleetSummary(clientId);
    }
}
