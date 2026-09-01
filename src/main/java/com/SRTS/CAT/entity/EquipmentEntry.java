package com.SRTS.CAT.entity;

import lombok.Data;

@Data
public class EquipmentEntry {
    
 private Object id;

 private String equipmentCode;
 private String type;
 private String status;
 private String siteId;
 private String currentLocation;
 private String engineHoursPerDay;
 private String idleHoursPerDay;
 private String operatingDays;
 private String lastOperatorId;

}
