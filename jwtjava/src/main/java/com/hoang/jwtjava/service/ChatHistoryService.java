package com.hoang.jwtjava.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hoang.jwtjava.config.AppRedisProperties;
import com.hoang.jwtjava.config.GeminiProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class ChatHistoryService {

    private static final String REDIS_KEY_PREFIX = "chat:history:";

    private final GeminiProperties geminiProperties;
    private final AppRedisProperties redisProperties;
    private final ObjectMapper objectMapper;
    private final Optional<StringRedisTemplate> redisTemplate;

    public ChatHistoryService(
            GeminiProperties geminiProperties,
            AppRedisProperties redisProperties,
            ObjectMapper objectMapper,
            @Autowired(required = false) StringRedisTemplate redisTemplate) {
        this.geminiProperties = geminiProperties;
        this.redisProperties = redisProperties;
        this.objectMapper = objectMapper;
        this.redisTemplate = Optional.ofNullable(redisTemplate);
    }

    public List<ChatTurn> getRecentTurns(String sessionId) {
        if (!usableSession(sessionId) || !useRedis())
            return List.of();

        try {
            String raw = redisTemplate.get().opsForValue().get(redisKey(sessionId));
            if (raw == null || raw.isBlank())
                return List.of();
            List<ChatTurn> turns = objectMapper.readValue(raw, new TypeReference<>() {
            });
            return turns == null ? List.of() : List.copyOf(turns);
        } catch (Exception ex) {
            log.warn("Chat history load failed for session {}: {}", sessionId, ex.getMessage());
            return List.of();
        }
    }

    public void appendTurn(String sessionId, String userMessage, String assistantReply) {
        if (!usableSession(sessionId) || !useRedis())
            return;

        try {
            List<ChatTurn> turns = new ArrayList<>(getRecentTurns(sessionId));
            turns.add(new ChatTurn("user", userMessage));
            turns.add(new ChatTurn("assistant", assistantReply));

            int maxMessages = Math.max(2, geminiProperties.getHistory().getMaxTurns() * 2);
            if (turns.size() > maxMessages)
                turns = new ArrayList<>(turns.subList(turns.size() - maxMessages, turns.size()));

            int ttlSeconds = Math.max(60, geminiProperties.getHistory().getTtlSeconds());
            redisTemplate.get().opsForValue().set(
                    redisKey(sessionId),
                    objectMapper.writeValueAsString(turns),
                    Duration.ofSeconds(ttlSeconds));
        } catch (Exception ex) {
            log.warn("Chat history save failed for session {}: {}", sessionId, ex.getMessage());
        }
    }

    private boolean useRedis() {
        return redisProperties.isEnabled() && redisTemplate.isPresent();
    }

    private static boolean usableSession(String sessionId) {
        return sessionId != null && !sessionId.isBlank();
    }

    private static String redisKey(String sessionId) {
        return REDIS_KEY_PREFIX + sessionId.trim();
    }

    public record ChatTurn(String role, String text) {
    }
}
