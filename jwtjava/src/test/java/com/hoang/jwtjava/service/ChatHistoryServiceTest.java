package com.hoang.jwtjava.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hoang.jwtjava.config.AppRedisProperties;
import com.hoang.jwtjava.config.GeminiProperties;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatHistoryServiceTest {

    @Test
    void blankSessionSkipsPersistence() {
        ChatHistoryService service = newService(false);
        service.appendTurn("  ", "hello", "hi");
        assertTrue(service.getRecentTurns("  ").isEmpty());
    }

    @Test
    void disabledRedisReturnsEmptyHistory() {
        ChatHistoryService service = newService(false);
        service.appendTurn("s1", "hello", "hi");
        assertTrue(service.getRecentTurns("s1").isEmpty());
    }

    @Test
    void trimKeepsOnlyLatestTurns() {
        List<ChatHistoryService.ChatTurn> turns = new ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            turns.add(new ChatHistoryService.ChatTurn("user", "u" + i));
            turns.add(new ChatHistoryService.ChatTurn("assistant", "a" + i));
        }
        int maxMessages = 3 * 2;
        if (turns.size() > maxMessages)
            turns = new ArrayList<>(turns.subList(turns.size() - maxMessages, turns.size()));

        assertEquals(6, turns.size());
        assertEquals("u2", turns.get(0).text());
        assertEquals("a4", turns.get(5).text());
    }

    @Test
    void historyJsonRoundTrip() throws Exception {
        ConcurrentHashMap<String, AtomicReference<String>> store = new ConcurrentHashMap<>();
        ObjectMapper mapper = new ObjectMapper();
        List<ChatHistoryService.ChatTurn> turns = List.of(
                new ChatHistoryService.ChatTurn("user", "hello"),
                new ChatHistoryService.ChatTurn("assistant", "hi"));

        store.put("chat:history:s1", new AtomicReference<>(mapper.writeValueAsString(turns)));
        List<ChatHistoryService.ChatTurn> parsed = mapper.readValue(
                store.get("chat:history:s1").get(),
                new com.fasterxml.jackson.core.type.TypeReference<>() {
                });

        assertEquals(2, parsed.size());
        assertEquals("hello", parsed.get(0).text());
    }

    private static ChatHistoryService newService(boolean redisEnabled) {
        GeminiProperties geminiProperties = new GeminiProperties();
        AppRedisProperties redisProperties = new AppRedisProperties();
        redisProperties.setEnabled(redisEnabled);
        return new ChatHistoryService(geminiProperties, redisProperties, new ObjectMapper(), null);
    }
}
