package com.hoang.jwtjava.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hoang.jwtjava.config.GeminiProperties;
import com.hoang.jwtjava.exception.AppException;
import com.hoang.jwtjava.exception.ErrorCode;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiClient {

    private final GeminiProperties geminiProperties;
    private final ObjectMapper objectMapper;

    @PostConstruct
    void logStartupStatus() {
        String key = trimmedApiKey();
        log.info("Gemini chat: enabled={}, apiKeyConfigured={}, keyPrefix={}, model={}",
                geminiProperties.isEnabled(),
                !key.isBlank(),
                keyPrefix(key),
                geminiProperties.getModel());
    }

    public String generateText(String systemInstruction, List<ChatHistoryService.ChatTurn> history, String userMessage) {
        if (!geminiProperties.isEnabled())
            throw new AppException(ErrorCode.CHAT_UNAVAILABLE);

        String apiKey = trimmedApiKey();
        if (apiKey.isBlank())
            throw new AppException(ErrorCode.CHAT_UNAVAILABLE);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("systemInstruction", Map.of(
                "parts", List.of(Map.of("text", systemInstruction))));
        body.put("contents", buildContents(history, userMessage));
        body.put("generationConfig", Map.of(
                "temperature", 0.4,
                "maxOutputTokens", geminiProperties.getMaxOutputTokens(),
                "responseMimeType", "application/json"));

        RestClientResponseException lastHttpError = null;
        for (String model : candidateModels()) {
            try {
                return callModel(model, apiKey, body);
            } catch (RestClientResponseException ex) {
                lastHttpError = ex;
                int status = ex.getStatusCode().value();
                log.warn("Gemini model {} failed with HTTP {}: {}",
                        model, status, truncate(sanitize(ex.getResponseBodyAsString()), 400));
                if (status == 404 || status == 400) {
                    // try next candidate model
                    continue;
                }
                throw mapHttpError(ex);
            } catch (RestClientException ex) {
                log.error("Gemini request failed: {}", sanitize(ex.getMessage()));
                throw new AppException(ErrorCode.CHAT_UNAVAILABLE);
            }
        }

        if (lastHttpError != null)
            throw mapHttpError(lastHttpError);
        throw new AppException(ErrorCode.CHAT_UNAVAILABLE);
    }

    private String callModel(String model, String apiKey, Map<String, Object> body) {
        // Keep ":generateContent" unencoded. Prefer query ?key= (AIza + AQ. auth keys).
        String endpoint = geminiProperties.getApiBaseUrl().replaceAll("/$", "")
                + "/models/" + model + ":generateContent"
                + "?key=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8);
        URI uri = URI.create(endpoint);

        String raw = restClient().post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class);

        return extractText(raw);
    }

    private AppException mapHttpError(RestClientResponseException ex) {
        int status = ex.getStatusCode().value();
        String body = sanitize(ex.getResponseBodyAsString());
        if (status == 401 || status == 403)
            return new AppException(ErrorCode.CHAT_AUTH_FAILED);
        if (status == 429 || body.toLowerCase().contains("quota") || body.contains("RESOURCE_EXHAUSTED"))
            return new AppException(ErrorCode.CHAT_QUOTA_EXCEEDED);
        return new AppException(ErrorCode.CHAT_UNAVAILABLE);
    }

    private List<String> candidateModels() {
        Set<String> models = new LinkedHashSet<>();
        if (geminiProperties.getModel() != null && !geminiProperties.getModel().isBlank())
            models.add(geminiProperties.getModel().trim());
        models.add("gemini-2.5-flash");
        models.add("gemini-2.0-flash");
        models.add("gemini-flash-latest");
        return List.copyOf(models);
    }

    private String trimmedApiKey() {
        String apiKey = geminiProperties.getApiKey();
        return apiKey == null ? "" : apiKey.trim();
    }

    private RestClient restClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        int timeoutMs = Math.max(5, geminiProperties.getTimeoutSeconds()) * 1000;
        factory.setConnectTimeout(timeoutMs);
        factory.setReadTimeout(timeoutMs);
        return RestClient.builder().requestFactory(factory).build();
    }

    private List<Map<String, Object>> buildContents(
            List<ChatHistoryService.ChatTurn> history,
            String userMessage) {
        List<Map<String, Object>> contents = new ArrayList<>();
        if (history != null) {
            for (ChatHistoryService.ChatTurn turn : history) {
                if (turn == null || turn.role() == null || turn.text() == null || turn.text().isBlank())
                    continue;
                String role = "assistant".equalsIgnoreCase(turn.role()) ? "model" : "user";
                contents.add(Map.of(
                        "role", role,
                        "parts", List.of(Map.of("text", turn.text()))));
            }
        }
        contents.add(Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", userMessage))));
        return contents;
    }

    private String extractText(String raw) {
        if (raw == null || raw.isBlank())
            throw new AppException(ErrorCode.CHAT_INVALID_RESPONSE);
        try {
            JsonNode root = objectMapper.readTree(raw);
            JsonNode candidates = root.path("candidates");
            if (!candidates.isArray() || candidates.isEmpty())
                throw new AppException(ErrorCode.CHAT_INVALID_RESPONSE);

            StringBuilder text = new StringBuilder();
            for (JsonNode part : candidates.get(0).path("content").path("parts")) {
                if (part.hasNonNull("text"))
                    text.append(part.get("text").asText());
            }
            String result = text.toString().trim();
            if (result.isBlank())
                throw new AppException(ErrorCode.CHAT_INVALID_RESPONSE);
            return result;
        } catch (AppException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("Failed to parse Gemini response: {}", sanitize(ex.getMessage()));
            throw new AppException(ErrorCode.CHAT_INVALID_RESPONSE);
        }
    }

    private static String keyPrefix(String key) {
        if (key == null || key.isBlank())
            return "none";
        if (key.startsWith("AQ."))
            return "AQ.";
        if (key.startsWith("AIza"))
            return "AIza";
        return "other";
    }

    private static String sanitize(String message) {
        if (message == null)
            return "";
        return message
                .replaceAll("(?i)(key|apikey|api_key)=[^\\s&\"']+", "$1=***")
                .replaceAll("AQ\\.[A-Za-z0-9_-]+", "AQ.***")
                .replaceAll("AIza[A-Za-z0-9_-]+", "AIza***");
    }

    private static String truncate(String value, int max) {
        if (value == null)
            return "";
        if (value.length() <= max)
            return value;
        return value.substring(0, max) + "…";
    }
}
