package com.SRTS.CAT.repo;

import com.SRTS.CAT.entity.Client;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ClientRepo extends MongoRepository<Client, String> {
}
