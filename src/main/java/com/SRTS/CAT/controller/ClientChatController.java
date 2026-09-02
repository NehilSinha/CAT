package com.SRTS.CAT.controller;

import com.SRTS.CAT.dto.ChatRequest;
import com.SRTS.CAT.dto.ChatResponse;
import com.SRTS.CAT.dto.ClientEquipmentView;
import com.SRTS.CAT.dto.FleetSummary;
import com.SRTS.CAT.entity.Operator;
import com.SRTS.CAT.service.ClientDashboardService;
import com.SRTS.CAT.service.ClientService;
import com.SRTS.CAT.service.GroqChatService;
import com.SRTS.CAT.service.OperatorService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/clients/{clientId}/chat")
public class ClientChatController {

    private final ClientService clientService;
    private final ClientDashboardService clientDashboardService;
    private final OperatorService operatorService;
    private final GroqChatService groqChatService;

    public ClientChatController(ClientService clientService, ClientDashboardService clientDashboardService, OperatorService operatorService, GroqChatService groqChatService) {
        this.clientService = clientService;
        this.clientDashboardService = clientDashboardService;
        this.operatorService = operatorService;
        this.groqChatService = groqChatService;
    }

    @PostMapping
    public ChatResponse chat(@PathVariable String clientId, @RequestBody ChatRequest request) {
        clientService.getById(clientId); // 404s here if the client code is invalid

        FleetSummary summary = clientDashboardService.getFleetSummary(clientId);
        List<ClientEquipmentView> equipment = clientDashboardService.getEquipmentForClient(clientId);
        List<Operator> operators = operatorService.getForClient(clientId);
        Map<String, String> operatorNames = operators.stream()
                .collect(Collectors.toMap(Operator::getId, Operator::getName));

        String context = buildContext(summary, equipment, operators, operatorNames);
        String reply = groqChatService.ask(context, request.getMessage());
        return new ChatResponse(reply);
    }

    private String buildContext(FleetSummary summary, List<ClientEquipmentView> equipment,
                                 List<Operator> operators, Map<String, String> operatorNames) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are an assistant for a Caterpillar equipment rental client. ");
        sb.append("Answer only using the data below - never invent numbers, dates, or machines not listed. ");
        sb.append("Be decisive: when data supports a clear recommendation (e.g. return an underused machine, ");
        sb.append("flag an overdue one), say so directly instead of hedging. ");
        sb.append("Use markdown formatting (bold, bullet lists) when it makes a multi-machine answer easier to scan.\n\n");
        sb.append("Fleet summary: ").append(summary.getTotalRented()).append(" machines rented, ")
                .append(summary.getActivelyUsed()).append(" actively used, ")
                .append(summary.getUnderutilizedCount()).append(" underutilized.\n\n");
        sb.append("Operators registered by this client: ");
        if (operators.isEmpty()) {
            sb.append("none.\n\n");
        } else {
            sb.append(operators.stream().map(Operator::getName).collect(Collectors.joining(", "))).append(".\n\n");
        }
        sb.append("Per-machine detail:\n");
        for (ClientEquipmentView e : equipment) {
            String operatorName = operatorNames.getOrDefault(e.getLastOperatorId(), "Unassigned");
            sb.append("- ").append(e.getEquipmentCode())
                    .append(" (").append(e.getType()).append("): status=").append(e.getStatus())
                    .append(", assignedOperator=").append(operatorName)
                    .append(", location=").append(e.getCurrentLocation())
                    .append(", checkedOut=").append(e.getCheckOutDate())
                    .append(", expectedReturn=").append(e.getExpectedReturnDate())
                    .append(", operatingDays=").append(e.getOperatingDays())
                    .append(", currentUtilization=").append(Math.round(e.getUtilization() * 100)).append("%")
                    .append(", fuelLevel=").append(e.getFuelLevel())
                    .append(", avgFuelLevelSinceCheckout=").append(Math.round(e.getAverageFuelLevel()))
                    .append(", avgEngineHoursSinceCheckout=").append(Math.round(e.getAverageEngineHours()))
                    .append(", overdue=").append(e.isOverdue())
                    .append(", overheating=").append(e.isOverheating())
                    .append(", lowFuel=").append(e.isLowFuel())
                    .append(", seatbeltViolation=").append(e.isSeatbeltViolation())
                    .append(", idleHoursAnomaly=").append(e.isIdleAnomaly())
                    .append(" (idleHoursAnomaly means today's idle hours are unusually high compared to this machine's own recent history)")
                    .append(", unassignedUse=").append(e.isUnassignedUse())
                    .append(" (unassignedUse means the engine is actively running but no seatbelt is engaged, so no verified operator)")
                    .append("\n");
        }
        return sb.toString();
    }
}
