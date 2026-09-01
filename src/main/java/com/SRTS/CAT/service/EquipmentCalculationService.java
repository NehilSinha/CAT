package com.SRTS.CAT.service;

import com.SRTS.CAT.entity.EquipmentEntry;
import com.SRTS.CAT.entity.EquipmentStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class EquipmentCalculationService {

    private static final double UNDERUTILIZATION_THRESHOLD = 0.3;
    private static final int UPCOMING_RETURN_WINDOW_DAYS = 2;
    private static final double OVERHEATING_THRESHOLD_CELSIUS = 100.0;
    private static final int LOW_FUEL_THRESHOLD_PERCENT = 15;
    private static final int NEAR_FULL_FUEL_THRESHOLD_PERCENT = 95;
    private static final double IDLE_ANOMALY_MULTIPLIER = 1.5;
    private static final int MIN_HISTORY_FOR_ANOMALY = 3;

    public double calculateUtilization(EquipmentEntry equipment) {
        Integer engineHours = equipment.getEngineHoursPerDay();
        Integer idleHours = equipment.getIdleHoursPerDay();
        if (engineHours == null || idleHours == null) {
            return 0.0;
        }
        int totalHours = engineHours + idleHours;
        if (totalHours == 0) {
            return 0.0;
        }
        return (double) engineHours / totalHours;
    }

    public boolean isOverdue(EquipmentEntry equipment) {
        return equipment.getStatus() == EquipmentStatus.RENTED
                && equipment.getExpectedReturnDate() != null
                && equipment.getExpectedReturnDate().isBefore(LocalDate.now());
    }

    public boolean isUpcomingReturn(EquipmentEntry equipment) {
        if (equipment.getStatus() != EquipmentStatus.RENTED || equipment.getExpectedReturnDate() == null) {
            return false;
        }
        LocalDate today = LocalDate.now();
        LocalDate returnDate = equipment.getExpectedReturnDate();
        return !returnDate.isBefore(today) && !returnDate.isAfter(today.plusDays(UPCOMING_RETURN_WINDOW_DAYS));
    }

    public boolean isUnderutilized(EquipmentEntry equipment) {
        return equipment.getStatus() == EquipmentStatus.RENTED
                && calculateUtilization(equipment) < UNDERUTILIZATION_THRESHOLD;
    }

    public boolean isOverheating(EquipmentEntry equipment) {
        return equipment.getEngineTemperature() != null
                && equipment.getEngineTemperature() > OVERHEATING_THRESHOLD_CELSIUS;
    }

    public boolean isLowFuel(EquipmentEntry equipment) {
        return equipment.getFuelLevel() != null
                && equipment.getFuelLevel() < LOW_FUEL_THRESHOLD_PERCENT;
    }

    public boolean isSeatbeltViolation(EquipmentEntry equipment) {
        return equipment.getStatus() == EquipmentStatus.RENTED
                && Boolean.FALSE.equals(equipment.getSeatbeltEngaged());
    }

    public double average(List<Integer> history) {
        if (history == null || history.isEmpty()) {
            return 0.0;
        }
        return history.stream().mapToInt(Integer::intValue).average().orElse(0.0);
    }

    /**
     * dayNumber is 1-indexed (day 1 = the first recorded reading), matching how
     * people naturally refer to "day 3" rather than Java's 0-based list indexing.
     */
    public Integer getDay(List<Integer> history, int dayNumber) {
        if (history == null || dayNumber < 1 || dayNumber > history.size()) {
            throw new IllegalArgumentException("No reading for day " + dayNumber);
        }
        return history.get(dayNumber - 1);
    }

    public double averageEngineHours(EquipmentEntry equipment) {
        return average(equipment.getEngineHoursHistory());
    }

    public double averageIdleHours(EquipmentEntry equipment) {
        return average(equipment.getIdleHoursHistory());
    }

    public double averageFuelLevel(EquipmentEntry equipment) {
        return average(equipment.getFuelLevelHistory());
    }

    private boolean fuelBarelyUsed(EquipmentEntry equipment) {
        return equipment.getFuelLevel() != null && equipment.getFuelLevel() >= NEAR_FULL_FUEL_THRESHOLD_PERCENT;
    }

    /**
     * A rented machine that's likely not earning its rental - fuel gauge basically
     * untouched since checkout, or a low engine-vs-idle ratio. Either signal alone is enough.
     */
    public boolean isLikelyUnused(EquipmentEntry equipment) {
        return equipment.getStatus() == EquipmentStatus.RENTED
                && (fuelBarelyUsed(equipment) || isUnderutilized(equipment));
    }

    public String unusedReason(EquipmentEntry equipment) {
        if (fuelBarelyUsed(equipment)) {
            return "fuel barely used (" + equipment.getFuelLevel() + "% remaining)";
        }
        return "low engine utilization (" + Math.round(calculateUtilization(equipment) * 100) + "%)";
    }

    /**
     * True anomaly detection: compares the latest idle-hours reading against
     * THIS machine's own historical baseline, not a fixed threshold. Needs
     * a few prior readings before it can say what's "normal" for this machine.
     */
    public boolean isIdleHoursAnomaly(EquipmentEntry equipment) {
        List<Integer> history = equipment.getIdleHoursHistory();
        if (history == null || history.size() < MIN_HISTORY_FOR_ANOMALY) {
            return false;
        }
        List<Integer> priorReadings = history.subList(0, history.size() - 1);
        double baseline = average(priorReadings);
        int latest = history.get(history.size() - 1);
        return baseline > 0 && latest > baseline * IDLE_ANOMALY_MULTIPLIER;
    }

    /**
     * "Unassigned equipment" proxy: there's no reliable operator-assignment
     * workflow to check against, so instead this infers the same real-world
     * problem from sensors we actually trust - the engine is actively doing
     * work but nobody's seatbelt is engaged, meaning no verified operator.
     */
    public boolean isUnassignedUse(EquipmentEntry equipment) {
        return equipment.getStatus() == EquipmentStatus.RENTED
                && equipment.isActiveState()
                && Boolean.FALSE.equals(equipment.getSeatbeltEngaged());
    }
}
