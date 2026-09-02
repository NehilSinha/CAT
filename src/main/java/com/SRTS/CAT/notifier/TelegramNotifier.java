package com.SRTS.CAT.notifier;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.SRTS.CAT.util.EnvLoader;
import org.bson.Document;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Standalone Telegram alert notifier for clients.
 *
 * Plain Java, not a Spring bean - not started by the Spring Boot app.
 * Run it as its own process while the backend is up (it calls the same
 * REST API the dashboard uses, so it never recomputes alert logic itself).
 *
 * IMPORTANT: run only ONE instance of this at a time, for one bot token.
 * Telegram's getUpdates only supports a single consumer per bot - two
 * processes polling the same token concurrently causes messages to be
 * split/lost between them unpredictably.
 *
 * How it works - two independent loops, so registration replies are near-instant
 * while alert-checking stays on its own slower cadence:
 *   1. Main thread: continuously long-polls Telegram for new messages (up to
 *      25s per call, returns immediately when one arrives). A client sends
 *      the bot their Client ID once; if valid (checked via GET /api/clients/{id})
 *      their chat is linked to that client in Mongo (telegram_subscriptions).
 *   2. Background thread: every intervalSeconds, for each registered client,
 *      polls GET /api/clients/{id}/equipment and sends a message only for
 *      flags that just turned on since the last check - not every tick.
 *
 * Get a bot token from @BotFather on Telegram: message @BotFather,
 * send /newbot, follow the prompts, copy the token it gives you.
 *
 * Usage:
 *   java -cp target/classes;<mongodb-driver-sync + bson jars> \
 *        com.SRTS.CAT.notifier.TelegramNotifier [telegram-bot-token] [apiBaseUrl] [mongoUri] [intervalSeconds]
 *
 * Defaults: apiBaseUrl=http://localhost:8080/api, intervalSeconds=30.
 * The bot token and mongoUri have no defaults baked in - pass them as
 * arguments, or set TELEGRAM_BOT_TOKEN / MONGODB_URI in the environment
 * or a ".env" file in the project root. Stops on Ctrl+C.
 */
public class TelegramNotifier {

    // NO_PROXY forces direct connections. Without it, HttpClient honors any
    // system/network proxy, which can silently break localhost calls even
    // when curl (which usually ignores proxies for localhost) works fine.
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .proxy(HttpClient.Builder.NO_PROXY)
            .build();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static String botToken;
    private static String apiBase;
    private static long updateOffset = 0;

    // clientId -> set of "equipmentCode:flagName" already known to be active,
    // so we only notify when a flag newly turns on, not every tick it stays on.
    private static final Map<String, Set<String>> activeFlagsByClient = new HashMap<>();

    public static void main(String[] args) {
        botToken = args.length > 0 ? args[0] : EnvLoader.get("TELEGRAM_BOT_TOKEN");
        if (botToken == null || botToken.isBlank()) {
            System.err.println("Provide a Telegram bot token as the first argument, or set TELEGRAM_BOT_TOKEN.");
            System.exit(1);
        }
        apiBase = args.length > 1 ? args[1] : "http://localhost:8080/api";
        String mongoUri = args.length > 2 ? args[2] : EnvLoader.get("MONGODB_URI");
        if (mongoUri == null || mongoUri.isBlank()) {
            System.err.println("Provide a MongoDB URI as the third argument, or set MONGODB_URI.");
            System.exit(1);
        }
        long intervalSeconds = args.length > 3 ? Long.parseLong(args[3]) : 30;

        MongoClient client = MongoClients.create(mongoUri);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nShutting down Telegram notifier...");
            client.close();
        }));

        MongoCollection<Document> subscriptions = client.getDatabase("CAT").getCollection("telegram_subscriptions");

        System.out.printf("Telegram notifier started. Checking for alerts every %ds. Press Ctrl+C to stop.%n", intervalSeconds);

        Thread alertThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    checkAlerts(subscriptions);
                    Thread.sleep(intervalSeconds * 1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    System.err.println("Alert check failed: " + e.getClass().getSimpleName() + " - " + e.getMessage());
                }
            }
        }, "alert-check");
        alertThread.setDaemon(true);
        alertThread.start();

        while (true) {
            try {
                processRegistrations(subscriptions);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("Registration check failed: " + e.getClass().getSimpleName() + " - " + e.getMessage());
                sleepQuietly(2000);
            }
        }
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void processRegistrations(MongoCollection<Document> subscriptions) throws Exception {
        // Long-poll: Telegram holds the connection open up to 25s and returns
        // as soon as a message arrives, so /start gets an almost-instant reply
        // instead of waiting for the next slow alert-check tick.
        String url = "https://api.telegram.org/bot" + botToken + "/getUpdates?offset=" + updateOffset + "&timeout=25";
        HttpResponse<String> response = HTTP.send(
                HttpRequest.newBuilder().uri(URI.create(url)).timeout(Duration.ofSeconds(35)).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );
        JsonNode root = MAPPER.readTree(response.body());
        if (!root.path("ok").asBoolean(false)) {
            return;
        }

        for (JsonNode update : root.path("result")) {
            updateOffset = update.path("update_id").asLong() + 1;
            JsonNode message = update.path("message");
            if (message.isMissingNode()) {
                continue;
            }

            long chatId = message.path("chat").path("id").asLong();
            String text = message.path("text").asText("").trim().toUpperCase();

            if (text.isEmpty() || text.equals("/START")) {
                sendMessage(chatId, "Send me your Client ID to subscribe to alerts for your equipment.");
                continue;
            }

            if (isValidClient(text)) {
                subscriptions.updateOne(
                        Filters.eq("clientId", text),
                        Updates.combine(Updates.set("clientId", text), Updates.set("chatId", chatId)),
                        new UpdateOptions().upsert(true)
                );
                sendMessage(chatId, "Subscribed. You'll get a message here whenever something needs your attention.");
            } else {
                sendMessage(chatId, "That doesn't look like a valid Client ID. Ask CAT retail for your code and try again.");
            }
        }
    }

    private static boolean isValidClient(String clientId) throws Exception {
        HttpResponse<String> response = HTTP.send(
                HttpRequest.newBuilder().uri(URI.create(apiBase + "/clients/" + clientId)).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );
        return response.statusCode() == 200;
    }

    private static void checkAlerts(MongoCollection<Document> subscriptions) {
        for (Document sub : subscriptions.find()) {
            String clientId = sub.getString("clientId");
            long chatId = ((Number) sub.get("chatId")).longValue();
            try {
                notifyNewFlags(clientId, chatId);
            } catch (Exception e) {
                System.err.println("Failed checking alerts for " + clientId + ": "
                        + e.getClass().getSimpleName() + " - " + e.getMessage());
            }
        }
    }

    private static void notifyNewFlags(String clientId, long chatId) throws Exception {
        HttpResponse<String> response = HTTP.send(
                HttpRequest.newBuilder().uri(URI.create(apiBase + "/clients/" + clientId + "/equipment")).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );
        if (response.statusCode() != 200) {
            System.err.println("Equipment fetch for " + clientId + " returned HTTP " + response.statusCode());
            return;
        }

        JsonNode equipmentList = MAPPER.readTree(response.body());
        Map<String, String> currentFlags = new HashMap<>();

        for (JsonNode e : equipmentList) {
            String code = e.path("equipmentCode").asText();
            collectFlag(currentFlags, code, "overdue", e.path("overdue").asBoolean(false), code + ": overdue for return");
            collectFlag(currentFlags, code, "overheating", e.path("overheating").asBoolean(false), code + ": overheating");
            collectFlag(currentFlags, code, "lowFuel", e.path("lowFuel").asBoolean(false), code + ": low on fuel");
            collectFlag(currentFlags, code, "seatbeltViolation", e.path("seatbeltViolation").asBoolean(false), code + ": seatbelt violation");
            collectFlag(currentFlags, code, "idleAnomaly", e.path("idleAnomaly").asBoolean(false), code + ": unusually high idle time");
            collectFlag(currentFlags, code, "unassignedUse", e.path("unassignedUse").asBoolean(false), code + ": running with no operator seatbelt engaged");
        }

        Set<String> previousFlags = activeFlagsByClient.getOrDefault(clientId, Set.of());
        List<String> newLines = new ArrayList<>();
        for (Map.Entry<String, String> entry : currentFlags.entrySet()) {
            if (!previousFlags.contains(entry.getKey())) {
                newLines.add(entry.getValue());
            }
        }
        activeFlagsByClient.put(clientId, new HashSet<>(currentFlags.keySet()));

        if (!newLines.isEmpty()) {
            sendMessage(chatId, "Alert update:\n" + String.join("\n", newLines));
        }
    }

    private static void collectFlag(Map<String, String> flags, String equipmentCode, String flagName, boolean active, String line) {
        if (active) {
            flags.put(equipmentCode + ":" + flagName, line);
        }
    }

    private static void sendMessage(long chatId, String text) {
        try {
            ObjectNode body = MAPPER.createObjectNode();
            body.put("chat_id", chatId);
            body.put("text", text);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.telegram.org/bot" + botToken + "/sendMessage"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(body)))
                    .build();
            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                System.err.println("Telegram rejected sendMessage to chat " + chatId + ": HTTP " + response.statusCode()
                        + " - " + response.body());
            }
        } catch (Exception e) {
            System.err.println("Failed to send Telegram message: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
    }
}
