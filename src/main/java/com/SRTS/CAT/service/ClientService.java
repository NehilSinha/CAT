package com.SRTS.CAT.service;

import com.SRTS.CAT.entity.Client;
import com.SRTS.CAT.repo.ClientRepo;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class ClientService {

    // Excludes O/0/I/1 so a client reading the code aloud/typing it can't confuse characters.
    private static final String ID_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int ID_LENGTH = 8;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ClientRepo clientRepo;

    public ClientService(ClientRepo clientRepo) {
        this.clientRepo = clientRepo;
    }

    public Client create(String companyName) {
        Client client = new Client();
        client.setId(generateUniqueId());
        client.setCompanyName(companyName);
        client.setCreatedDate(LocalDate.now());
        return clientRepo.save(client);
    }

    public List<Client> getAll() {
        return clientRepo.findAll();
    }

    public Client getById(String id) {
        return clientRepo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Client not found: " + id));
    }

    public boolean exists(String id) {
        return clientRepo.existsById(id);
    }

    private String generateUniqueId() {
        String id;
        do {
            id = generateId();
        } while (clientRepo.existsById(id));
        return id;
    }

    private String generateId() {
        StringBuilder code = new StringBuilder(ID_LENGTH);
        for (int i = 0; i < ID_LENGTH; i++) {
            code.append(ID_CHARS.charAt(RANDOM.nextInt(ID_CHARS.length())));
        }
        return code.toString();
    }
}
