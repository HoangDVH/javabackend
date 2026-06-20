package com.hoang.jwtjava.service;

import com.hoang.jwtjava.config.RateLimitProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@Service
public class RateLimitService {

    private final RateLimitProperties rateLimitProperties;
    private final Optional<StringRedisTemplate> redisTemplate;

    public RateLimitService(
            RateLimitProperties rateLimitProperties,
            @Autowired(required = false) StringRedisTemplate redisTemplate) {
        this.rateLimitProperties = rateLimitProperties;
        this.redisTemplate = Optional.ofNullable(redisTemplate);
    }

    public RateLimitDecision check(String endpoint, String identifier, RateLimitProperties.EndpointLimit limit) {
        if (!rateLimitProperties.isEnabled())
            return RateLimitDecision.allowed(Integer.MAX_VALUE);

        if (identifier == null || identifier.isBlank())
            identifier = "unknown";

        if (redisTemplate.isEmpty()) {
            log.warn("Rate limit skipped for {}:{} — Redis unavailable", endpoint, identifier);
            return RateLimitDecision.allowed(limit.getMaxRequests());
        }

        long windowSeconds = Math.max(1, limit.getWindowSeconds());
        long windowBucket = System.currentTimeMillis() / (windowSeconds * 1000L);
        String key = "rl:" + endpoint + ":" + identifier + ":" + windowBucket;

        try {
            Long count = redisTemplate.get().opsForValue().increment(key);
            if (count != null && count == 1L)
                redisTemplate.get().expire(key, Duration.ofSeconds(windowSeconds + 1));

            int remaining = Math.max(0, limit.getMaxRequests() - (count != null ? count.intValue() : 0));
            if (count != null && count > limit.getMaxRequests()) {
                long retryAfter = windowSeconds - (System.currentTimeMillis() / 1000L % windowSeconds);
                return RateLimitDecision.denied(remaining, Math.max(1, retryAfter));
            }
            return RateLimitDecision.allowed(remaining);
        } catch (RuntimeException ex) {
            log.warn("Rate limit check failed for {}:{} — allowing request: {}", endpoint, identifier, ex.getMessage());
            return RateLimitDecision.allowed(limit.getMaxRequests());
        }
    }

    public record RateLimitDecision(boolean allowed, int remaining, long retryAfterSeconds) {
        public static RateLimitDecision allowed(int remaining) {
            return new RateLimitDecision(true, remaining, 0);
        }

        public static RateLimitDecision denied(int remaining, long retryAfterSeconds) {
            return new RateLimitDecision(false, remaining, retryAfterSeconds);
        }
    }
}
