package com.hoang.jwtjava.service;

import com.hoang.jwtjava.config.AppRedisProperties;
import com.hoang.jwtjava.config.MailProperties;
import com.hoang.jwtjava.entity.PasswordResetToken;
import com.hoang.jwtjava.repository.PasswordResetTokenRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Slf4j
@Service
public class PasswordResetTokenStore {

    private static final String REDIS_KEY_PREFIX = "pwd-reset:";

    private final MailProperties mailProperties;
    private final AppRedisProperties redisProperties;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final Optional<StringRedisTemplate> redisTemplate;

    public PasswordResetTokenStore(
            MailProperties mailProperties,
            AppRedisProperties redisProperties,
            PasswordResetTokenRepository passwordResetTokenRepository,
            @Autowired(required = false) StringRedisTemplate redisTemplate) {
        this.mailProperties = mailProperties;
        this.redisProperties = redisProperties;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.redisTemplate = Optional.ofNullable(redisTemplate);
    }

    public void save(String tokenHash, String email) {
        int ttlSeconds = Math.max(60, mailProperties.getPasswordReset().getTokenTtlSeconds());
        passwordResetTokenRepository.deleteByEmailIgnoreCase(email);

        if (useRedis()) {
            try {
                redisTemplate.get().opsForValue().set(
                        redisKey(tokenHash),
                        email.toLowerCase(),
                        Duration.ofSeconds(ttlSeconds));
                return;
            } catch (RuntimeException ex) {
                log.warn("Redis password-reset save failed, falling back to DB: {}", ex.getMessage());
            }
        }

        passwordResetTokenRepository.save(PasswordResetToken.builder()
                .tokenHash(tokenHash)
                .email(email.toLowerCase())
                .expiresAt(Instant.now().plusSeconds(ttlSeconds))
                .build());
    }

    public Optional<String> consume(String tokenHash) {
        if (useRedis()) {
            try {
                String email = redisTemplate.get().opsForValue().get(redisKey(tokenHash));
                if (email != null) {
                    redisTemplate.get().delete(redisKey(tokenHash));
                    return Optional.of(email);
                }
            } catch (RuntimeException ex) {
                log.warn("Redis password-reset consume failed, falling back to DB: {}", ex.getMessage());
            }
        }

        return passwordResetTokenRepository.findById(tokenHash)
                .filter(token -> token.getExpiresAt().isAfter(Instant.now()))
                .map(token -> {
                    passwordResetTokenRepository.delete(token);
                    return token.getEmail();
                });
    }

    private boolean useRedis() {
        return redisProperties.isEnabled() && redisTemplate.isPresent();
    }

    private static String redisKey(String tokenHash) {
        return REDIS_KEY_PREFIX + tokenHash;
    }
}
