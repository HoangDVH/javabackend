package com.hoang.jwtjava.service;

import com.hoang.jwtjava.config.AppRedisProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.redis.enabled", havingValue = "true", matchIfMissing = true)
public class RedisTokenRevocationStore implements TokenRevocationStore {

    private static final String KEY_PREFIX = "jwt:blacklist:";

    private final StringRedisTemplate redisTemplate;
    private final AppRedisProperties redisProperties;

    @Override
    public void revoke(String tokenId, Instant expiresAt, String tokenKind) {
        if (tokenId == null || tokenId.isBlank() || expiresAt == null)
            return;

        long ttlSeconds = Duration.between(Instant.now(), expiresAt).getSeconds();
        if (ttlSeconds <= 0)
            return;

        try {
            redisTemplate.opsForValue().set(
                    key(tokenId),
                    tokenKind != null ? tokenKind : "UNKNOWN",
                    Duration.ofSeconds(ttlSeconds));
        } catch (RuntimeException ex) {
            log.warn("Failed to revoke token {} in Redis: {}", tokenId, ex.getMessage());
        }
    }

    @Override
    public boolean isRevoked(String tokenId) {
        if (tokenId == null || tokenId.isBlank())
            return false;

        try {
            Boolean exists = redisTemplate.hasKey(key(tokenId));
            return Boolean.TRUE.equals(exists);
        } catch (RuntimeException ex) {
            log.warn("Redis blacklist lookup failed for token {}: {}", tokenId, ex.getMessage());
            if (redisProperties.isBlacklistFailOpen())
                return false;
            throw ex;
        }
    }

    private String key(String tokenId) {
        return KEY_PREFIX + tokenId;
    }
}
