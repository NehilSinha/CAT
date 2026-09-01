package com.SRTS.CAT.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Thin client for Groq's chat completions API (OpenAI-compatible).
 * Only ever asked to explain data Java has already computed - never to
 * calculate anything itself. See EquipmentCalculationService for the actual logic.
 */
@Service
public class GroqChatService {

    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL = "openai/gpt-oss-120b";

    @Value("${groq.api.key}")
    private String apiKey;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String ask(String systemContext, String userMessage) {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            root.put("model", MODEL);
            root.put("temperature", 0.3);

            ArrayNode messages = root.putArray("messages");
            messages.addObject().put("role", "system").put("content", systemContext);
            messages.addObject().put("role", "user").put("content", userMessage);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GROQ_URL))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(root)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new IllegalStateException("Groq API error (" + response.statusCode() + "): " + response.body());
            }

            JsonNode responseJson = objectMapper.readTree(response.body());
            return responseJson.at("/choices/0/message/content").asText();
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to reach Groq API: " + e.getMessage(), e);
        }
    }
}
