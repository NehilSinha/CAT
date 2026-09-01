package com.SRTS.CAT.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

@Data
@Document(collection = "clients")
public class Client {

    // Randomly generated (see ClientService), not a standard Mongo ObjectId -
    // this value is handed to the client and doubles as their login code.
    @Id
    private String id;

    private String companyName;
    private LocalDate createdDate;
}
