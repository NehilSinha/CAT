package com.SRTS.CAT.seed;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.SRTS.CAT.util.EnvLoader;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

/**
 * One-off standalone seed utility. Plain Java, not a Spring bean - inserts
 * sample AVAILABLE (not yet rented) equipment and exits.
 *
 * Run it once from an IDE "Run" on main(), or:
 *   java -cp target/classes;<mongodb-driver-sync + bson jars> com.SRTS.CAT.seed.DataSeeder
 *
 * Pass the MongoDB URI as the first argument, or set MONGODB_URI in the
 * environment / a ".env" file in the project root - no default is baked
 * in, so no credentials ever live in source.
 */
public class DataSeeder {

    // { EquipmentType enum name, code prefix, display name, starting number block }
    private static final String[][] TYPES = {
            {"EXCAVATOR", "EX", "Excavator", "101"},
            {"CRANE", "CR", "Crane", "201"},
            {"BULLDOZER", "BD", "Bulldozer", "301"},
            {"GRADER", "GR", "Grader", "401"},
            {"DUMP_TRUCK", "DT", "Dump Truck", "501"},
            {"WHEEL_LOADER", "WL", "Wheel Loader", "601"},
            {"COMPACTOR", "CP", "Compactor", "701"},
            {"FORKLIFT", "FL", "Forklift", "801"},
    };

    private static final String[] LOCATIONS = {"CAT Yard - Site A", "CAT Yard - Site B", "CAT Yard - Site C"};
    private static final int MACHINES_PER_TYPE = 7;

    public static void main(String[] args) {
        String uri = args.length > 0 ? args[0] : EnvLoader.get("MONGODB_URI");
        if (uri == null || uri.isBlank()) {
            System.err.println("Provide a MongoDB URI as the first argument, or set the MONGODB_URI environment variable.");
            System.exit(1);
        }
        String databaseName = args.length > 1 ? args[1] : "CAT";

        try (MongoClient client = MongoClients.create(uri)) {
            MongoCollection<Document> equipment = client.getDatabase(databaseName).getCollection("equipment");

            List<Document> seedData = buildSeedData();
            equipment.insertMany(seedData);
            System.out.printf("Inserted %d AVAILABLE equipment records into '%s'.%n", seedData.size(), databaseName);
        }
    }

    private static List<Document> buildSeedData() {
        List<Document> seedData = new ArrayList<>();
        int locationIndex = 0;

        for (String[] typeInfo : TYPES) {
            String type = typeInfo[0];
            String prefix = typeInfo[1];
            String displayName = typeInfo[2];
            int startNumber = Integer.parseInt(typeInfo[3]);

            for (int i = 0; i < MACHINES_PER_TYPE; i++) {
                int number = startNumber + i;
                String code = prefix + "-" + number;
                String name = displayName + " " + number;
                String location = LOCATIONS[locationIndex % LOCATIONS.length];
                locationIndex++;
                seedData.add(equipmentDoc(code, name, type, location));
            }
        }

        return seedData;
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
