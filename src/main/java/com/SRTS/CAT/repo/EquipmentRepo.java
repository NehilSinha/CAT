package com.SRTS.CAT.repo;
import com.SRTS.CAT.entity.EquipmentEntry;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface EquipmentRepo extends MongoRepository<EquipmentEntry, String>{
}
