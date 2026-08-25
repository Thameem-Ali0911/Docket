package com.docket.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Thin wrapper around the Gemini "Generate Content" REST API (legacy generateContent
 * endpoint - the Interactions API is now Google's recommended surface as of 2026, but
 * generateContent remains supported and is what architecture.md/phases.md assume).
 *
 * Endpoint + request/response shape confirmed against the live Gemini API docs
 * (ai.google.dev/gemini-api/docs/generate-content/structured-output) on 2026-08-15,
 * not assumed from Anthropic's /v1/messages shape - see memory.md Session 6.
 */
@Service
public class GeminiClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);

    private static final String ENDPOINT_TEMPLATE =
        "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent";

    private final String apiKey;
    private final String model;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public GeminiClient(@Value("${gemini.api-key}") String apiKey,
                         @Value("${gemini.model}") String model,
                         ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.model = model;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    }

    /**
     * Calls Gemini with a prompt and a JSON Schema, returning the raw JSON text of the
     * schema-constrained response (not yet deserialized into a specific DTO - callers
     * pick the target type since the schema differs per document type).
     *
     * @param prompt           the user-facing prompt text
     * @param jsonSchema       a JSON Schema string describing the desired response shape
     * @throws GeminiException on network failure, non-2xx response, or malformed response body
     */
    public String generateStructuredJson(String prompt, String jsonSchema) throws GeminiException {
        if (apiKey == null || apiKey.isBlank()) {
            throw new GeminiException("GEMINI_API_KEY is not configured.");
        }

        int maxAttempts = 3;
        long backoffMs = 1000;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                JsonNode schemaNode = objectMapper.readTree(jsonSchema);

                ObjectNode requestBody = objectMapper.createObjectNode();
                ObjectNode content = requestBody.putArray("contents").addObject();
                content.putArray("parts").addObject().put("text", prompt);

                ObjectNode generationConfig = requestBody.putObject("generationConfig");
                generationConfig.put("responseMimeType", "application/json");
                generationConfig.set("responseSchema", schemaNode);

                String requestJson = objectMapper.writeValueAsString(requestBody);
                String url = String.format(ENDPOINT_TEMPLATE, model);

                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("x-goog-api-key", apiKey)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(60))
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                    .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                int statusCode = response.statusCode();
                if (statusCode >= 200 && statusCode < 300) {
                    JsonNode root = objectMapper.readTree(response.body());
                    JsonNode candidates = root.path("candidates");
                    if (!candidates.isArray() || candidates.isEmpty()) {
                        throw new GeminiException("Gemini response had no candidates: " + response.body());
                    }
                    JsonNode parts = candidates.get(0).path("content").path("parts");
                    if (!parts.isArray() || parts.isEmpty()) {
                        throw new GeminiException("Gemini response candidate had no content parts: " + response.body());
                    }

                    return parts.get(0).path("text").asText();
                }

                // Check if error is retriable (429 rate limit or 5xx server error)
                boolean isRetriable = (statusCode == 429 || statusCode >= 500);
                if (isRetriable && attempt < maxAttempts) {
                    log.warn("Gemini API returned HTTP {} (attempt {} of {}). Retrying in {} ms...",
                            statusCode, attempt, maxAttempts, backoffMs);
                    Thread.sleep(backoffMs + (long) (Math.random() * 200));
                    backoffMs *= 2;
                    continue;
                }

                throw new GeminiException("Gemini API returned HTTP " + statusCode + ": " + response.body());

            } catch (GeminiException e) {
                if (attempt == maxAttempts) {
                    throw e;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new GeminiException("Gemini call was interrupted: " + e.getMessage(), e);
            } catch (Exception e) {
                if (attempt < maxAttempts) {
                    log.warn("Gemini API network error on attempt {} of {}: {}. Retrying in {} ms...",
                            attempt, maxAttempts, e.getMessage(), backoffMs);
                    try {
                        Thread.sleep(backoffMs + (long) (Math.random() * 200));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new GeminiException("Gemini call retry interrupted", ie);
                    }
                    backoffMs *= 2;
                } else {
                    log.error("Gemini API call failed after {} attempts", maxAttempts, e);
                    throw new GeminiException("Failed to call Gemini API after retries: " + e.getMessage(), e);
                }
            }
        }

        throw new GeminiException("Gemini API call failed after " + maxAttempts + " attempts.");
    }

    public static class GeminiException extends Exception {
        public GeminiException(String message) {
            super(message);
        }

        public GeminiException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
