package com.SRTS.CAT.simulator;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Standalone telemetry simulator for rented equipment.
 *
 * This is plain Java, not a Spring bean, and is never started by the
 * Spring Boot application. Run it as its own process, separately from
 * the hosted backend, e.g. from an IDE "Run" on main(), or:
 *
 *   java -cp target/classes;<mongodb-driver-sync + bson jars from your local .m2> \
 *        com.SRTS.CAT.simulator.TelemetrySimulator "<mongodb-uri>" [databaseName] [intervalSeconds]
 *
 * Pass the MongoDB URI as the first argument, or set the MONGODB_URI
 * environment variable - no default is baked in, so no credentials ever
 * live in source. Stops on Ctrl+C.
 */
public class TelemetrySimulator {

    private static final double MIN_TEMP_CELSIUS = 60.0;
    private static final double MAX_TEMP_CELSIUS = 130.0;
    private static final int MIN_FUEL_PERCENT = 0;
    private static final int MAX_FUEL_PERCENT = 100;

    public static void main(String[] args) {
        String uri = args.length > 0 ? args[0] : System.getenv("MONGODB_URI");
        if (uri == null || uri.isBlank()) {
            System.err.println("Provide a MongoDB URI as the first argument, or set the MONGODB_URI environment variable.");
            System.exit(1);
        }
        String databaseName = args.length > 1 ? args[1] : "CAT";
        long intervalSeconds = args.length > 2 ? Long.parseLong(args[2]) : 60;

        MongoClient client = MongoClients.create(uri);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nShutting down telemetry simulator...");
            client.close();
        }));

        MongoCollection<Document> equipment = client.getDatabase(databaseName).getCollection("equipment");

        System.out.printf("Telemetry simulator started against database '%s'. Updating rented equipment every %ds. Press Ctrl+C to stop.%n",
                databaseName, intervalSeconds);

        while (true) {
            try {
                tick(equipment);
                Thread.sleep(intervalSeconds * 1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("Simulator tick failed: " + e.getMessage());
            }
        }
    }

    private static void tick(MongoCollection<Document> equipment) {
        FindIterable<Document> rented = equipment.find(Filters.eq("status", "RENTED"));
        int updatedCount = 0;

        for (Document doc : rented) {
            ObjectId id = doc.getObjectId("_id");
            ThreadLocalRandom random = ThreadLocalRandom.current();

            // Real machines work in stretches, not random blips - mostly continue
            // whatever they were doing last tick, with a chance to switch.
            Boolean previousActive = doc.getBoolean("activeState");
            boolean activeThisTick = previousActive == null
                    ? random.nextInt(0, 10) > 2
                    : random.nextInt(0, 100) < 75 ? previousActive : !previousActive;

            // Temperature rises while actually working, cools back down while idle.
            double currentTemp = doc.get("engineTemperature") != null ? doc.getDouble("engineTemperature") : 85.0;
            double tempDelta = activeThisTick ? random.nextDouble(1, 5) : random.nextDouble(-3, 0.5);
            double newTemp = clamp(currentTemp + tempDelta, MIN_TEMP_CELSIUS, MAX_TEMP_CELSIUS);

            // Fuel burns faster under load than idling.
            int currentFuel = doc.get("fuelLevel") != null ? doc.getInteger("fuelLevel") : 100;
            int fuelBurn = activeThisTick ? random.nextInt(1, 4) : random.nextInt(0, 2);
            int newFuel = (int) clamp(currentFuel - fuelBurn, MIN_FUEL_PERCENT, MAX_FUEL_PERCENT);

            int engineHours = doc.get("engineHoursPerDay") != null ? doc.getInteger("engineHoursPerDay") : 0;
            int idleHours = doc.get("idleHoursPerDay") != null ? doc.getInteger("idleHoursPerDay") : 0;
            if (activeThisTick) {
                engineHours += 1;
            } else {
                idleHours += 1;
            }

            boolean seatbeltEngaged = random.nextInt(0, 20) > 1;

            equipment.updateOne(
                    Filters.eq("_id", id),
                    Updates.combine(
                            Updates.set("engineTemperature", newTemp),
                            Updates.set("fuelLevel", newFuel),
                            Updates.set("activeState", activeThisTick),
                            Updates.set("engineHoursPerDay", engineHours),
                            Updates.set("idleHoursPerDay", idleHours),
                            Updates.set("seatbeltEngaged", seatbeltEngaged),
                            Updates.push("engineHoursHistory", engineHours),
                            Updates.push("idleHoursHistory", idleHours),
                            Updates.push("fuelLevelHistory", newFuel)
                    )
            );
            updatedCount++;
        }

        System.out.printf("[%s] Updated telemetry for %d rented equipment.%n", LocalDateTime.now(), updatedCount);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
