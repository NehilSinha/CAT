package com.SRTS.CAT.entity;

import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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
 private LocalDate checkOutDate;
 private LocalDate checkInDate;
 private LocalDate expectedReturnDate;
 private String clientId;
 private Double engineTemperature;
 private Integer fuelLevel;
 private Boolean seatbeltEngaged;

 // History logs: one entry appended per recorded reading (index 0 = 1st reading).
 // Feeds averages now, and forecasting/anomaly-detection later.
 private List<Integer> engineHoursHistory = new ArrayList<>();
 private List<Integer> idleHoursHistory = new ArrayList<>();
 private List<Integer> fuelLevelHistory = new ArrayList<>();

}
