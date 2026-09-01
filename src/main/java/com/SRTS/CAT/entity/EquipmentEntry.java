package com.SRTS.CAT.entity;

import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;

import java.util.List;

@Data
public class EquipmentEntry {

 @Id
 private Object id;


 private String equipmentCode;
 private String equipmentName;
 private String type;
 private String status;
 private boolean activeState;
 private ObjectId siteId;
 private String currentLocation;
 private Integer engineHoursPerDay;
 private Integer idleHoursPerDay;
 private Integer operatingDays;
 private ObjectId lastOperatorId;

}
