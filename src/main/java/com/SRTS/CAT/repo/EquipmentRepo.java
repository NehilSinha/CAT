package com.SRTS.CAT.repo;
import com.SRTS.CAT.entity.EquipmentEntry;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface EquipmentRepo extends MongoRepository<EquipmentEntry, String>{
    List<EquipmentEntry> findByClientId(String clientId);
}
