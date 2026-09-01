package com.SRTS.CAT.controller;

import com.SRTS.CAT.entity.Client;
import com.SRTS.CAT.service.ClientService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/clients")
public class ClientController {

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    @PostMapping
    public Client create(@RequestBody Map<String, String> body) {
        return clientService.create(body.get("companyName"));
    }

    @GetMapping
    public List<Client> getAll() {
        return clientService.getAll();
    }

    @GetMapping("/{id}")
    public Client getById(@PathVariable String id) {
        return clientService.getById(id);
    }
}
