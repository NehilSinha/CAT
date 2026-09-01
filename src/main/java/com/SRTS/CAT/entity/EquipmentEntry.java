package com.SRTS.CAT.entity;

import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "equipment")
public class EquipmentEntry {

 @Id
 private String id;

 private String equipmentCode;
 private String equipmentName;
 private EquipmentType type;
 private EquipmentStatus status;
 private boolean activeState;
 private ObjectId siteId;
 private String currentLocation;
 private Integer engineHoursPerDay;
 private Integer idleHoursPerDay;
 private Integer operatingDays;
 private ObjectId lastOperatorId;

}
