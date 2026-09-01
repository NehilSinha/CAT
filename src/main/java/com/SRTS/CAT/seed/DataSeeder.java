package com.SRTS.CAT.seed;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import org.bson.Document;

import java.util.Arrays;
import java.util.List;

/**
 * One-off standalone seed utility. Plain Java, not a Spring bean - inserts
 * sample AVAILABLE (not yet rented) equipment and exits.
 *
 * Run it once from an IDE "Run" on main(), or:
 *   java -cp target/classes;<mongodb-driver-sync + bson jars> com.SRTS.CAT.seed.DataSeeder
 *
 * Pass the MongoDB URI as the first argument, or set the MONGODB_URI
 * environment variable - no default is baked in, so no credentials ever
 * live in source.
 */
public class DataSeeder {

    public static void main(String[] args) {
        String uri = args.length > 0 ? args[0] : System.getenv("MONGODB_URI");
        if (uri == null || uri.isBlank()) {
            System.err.println("Provide a MongoDB URI as the first argument, or set the MONGODB_URI environment variable.");
            System.exit(1);
        }
        String databaseName = args.length > 1 ? args[1] : "CAT";

        try (MongoClient client = MongoClients.create(uri)) {
            MongoCollection<Document> equipment = client.getDatabase(databaseName).getCollection("equipment");

            List<Document> seedData = Arrays.asList(
                    equipmentDoc("EX-101", "Excavator 101", "EXCAVATOR", "CAT Yard - Site A"),
                    equipmentDoc("EX-102", "Excavator 102", "EXCAVATOR", "CAT Yard - Site A"),
                    equipmentDoc("EX-103", "Excavator 103", "EXCAVATOR", "CAT Yard - Site B"),
                    equipmentDoc("CR-201", "Crane 201", "CRANE", "CAT Yard - Site A"),
                    equipmentDoc("CR-202", "Crane 202", "CRANE", "CAT Yard - Site B"),
                    equipmentDoc("BD-301", "Bulldozer 301", "BULLDOZER", "CAT Yard - Site A"),
                    equipmentDoc("BD-302", "Bulldozer 302", "BULLDOZER", "CAT Yard - Site B"),
                    equipmentDoc("BD-303", "Bulldozer 303", "BULLDOZER", "CAT Yard - Site B"),
                    equipmentDoc("GR-401", "Grader 401", "GRADER", "CAT Yard - Site A"),
                    equipmentDoc("GR-402", "Grader 402", "GRADER", "CAT Yard - Site B")
            );

            equipment.insertMany(seedData);
            System.out.printf("Inserted %d AVAILABLE equipment records into '%s'.%n", seedData.size(), databaseName);
        }
    }

    private static Document equipmentDoc(String code, String name, String type, String location) {
        return new Document()
                .append("equipmentCode", code)
                .append("equipmentName", name)
                .append("type", type)
                .append("status", "AVAILABLE")
                .append("activeState", false)
                .append("currentLocation", location)
                .append("engineHoursPerDay", 0)
                .append("idleHoursPerDay", 0)
                .append("operatingDays", 0)
                .append("engineTemperature", 25.0)
                .append("fuelLevel", 100)
                .append("seatbeltEngaged", true);
    }
}
