package com.hoang.jwtjava.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hoang.jwtjava.config.GeminiProperties;
import com.hoang.jwtjava.exception.AppException;
import com.hoang.jwtjava.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiClient {

    private final GeminiProperties geminiProperties;
    private final ObjectMapper objectMapper;

    public String generateText(String systemInstruction, List<ChatHistoryService.ChatTurn> history, String userMessage) {
        if (!geminiProperties.isEnabled())
            throw new AppException(ErrorCode.CHAT_UNAVAILABLE);

        String apiKey = geminiProperties.getApiKey();
        if (apiKey == null || apiKey.isBlank())
            throw new AppException(ErrorCode.CHAT_UNAVAILABLE);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("systemInstruction", Map.of(
                "parts", List.of(Map.of("text", systemInstruction))));
        body.put("contents", buildContents(history, userMessage));
        body.put("generationConfig", Map.of(
                "temperature", 0.4,
                "maxOutputTokens", geminiProperties.getMaxOutputTokens(),
                "responseMimeType", "application/json"));

        String url = geminiProperties.getApiBaseUrl().replaceAll("/$", "")
                + "/models/" + geminiProperties.getModel()
                + ":generateContent";

        try {
            String raw = restClient().post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("x-goog-api-key", apiKey)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            return extractText(raw);
        } catch (RestClientException ex) {
            log.error("Gemini request failed: {}", sanitize(ex.getMessage()));
            throw new AppException(ErrorCode.CHAT_UNAVAILABLE);
        }
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

    private static String sanitize(String message) {
        if (message == null)
            return "";
        return message.replaceAll("(?i)(key|apikey|api_key)=[^\\s&]+", "$1=***");
    }
}
