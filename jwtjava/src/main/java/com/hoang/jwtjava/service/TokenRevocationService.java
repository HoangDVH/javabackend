package com.hoang.jwtjava.service;

import com.hoang.jwtjava.config.AppRedisProperties;
import com.hoang.jwtjava.repository.InvalidatedTokenRepository;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Date;
import java.util.Optional;

@Slf4j
@Service
public class TokenRevocationService {

    private final DbTokenRevocationStore dbTokenRevocationStore;
    private final Optional<RedisTokenRevocationStore> redisTokenRevocationStore;
    private final AppRedisProperties redisProperties;
    private final InvalidatedTokenRepository invalidatedTokenRepository;

    public TokenRevocationService(
            DbTokenRevocationStore dbTokenRevocationStore,
            @Autowired(required = false) RedisTokenRevocationStore redisTokenRevocationStore,
            AppRedisProperties redisProperties,
            InvalidatedTokenRepository invalidatedTokenRepository) {
        this.dbTokenRevocationStore = dbTokenRevocationStore;
        this.redisTokenRevocationStore = Optional.ofNullable(redisTokenRevocationStore);
        this.redisProperties = redisProperties;
        this.invalidatedTokenRepository = invalidatedTokenRepository;
    }

    @Transactional
    public void revoke(Claims claims) {
        String tokenId = claims.getId();
        if (tokenId == null || tokenId.isBlank())
            return;

        Date expiry = claims.getExpiration();
        if (expiry == null)
            return;
        Instant expiresAt = expiry.toInstant();
        if (expiresAt.isBefore(Instant.now()))
            return;

        String tokenKind = claims.get("token_kind", String.class);
        revoke(tokenId, expiresAt, tokenKind);
    }

    public void revoke(String tokenId, Instant expiresAt, String tokenKind) {
        dbTokenRevocationStore.revoke(tokenId, expiresAt, tokenKind);
        if (redisEnabled())
            redisTokenRevocationStore.ifPresent(store -> store.revoke(tokenId, expiresAt, tokenKind));
    }

    public boolean isRevoked(String tokenId) {
        if (tokenId == null || tokenId.isBlank())
            return false;

        if (redisEnabled()) {
            try {
                if (redisTokenRevocationStore.get().isRevoked(tokenId))
                    return true;
            } catch (RuntimeException ex) {
                log.warn("Redis blacklist check failed, falling back to DB: {}", ex.getMessage());
            }
        }

        return dbTokenRevocationStore.isRevoked(tokenId);
    }

    @Transactional
    public void cleanupExpired() {
        invalidatedTokenRepository.deleteByExpiresAtBefore(Instant.now());
    }

    private boolean redisEnabled() {
        return redisProperties.isEnabled() && redisTokenRevocationStore.isPresent();
    }
}
