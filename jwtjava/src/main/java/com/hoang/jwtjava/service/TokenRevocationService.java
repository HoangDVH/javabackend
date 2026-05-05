package com.hoang.jwtjava.service;

import com.hoang.jwtjava.entity.InvalidatedToken;
import com.hoang.jwtjava.repository.InvalidatedTokenRepository;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class TokenRevocationService {

    private final InvalidatedTokenRepository invalidatedTokenRepository;

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

        invalidatedTokenRepository.save(InvalidatedToken.builder()
                .id(tokenId)
                .expiresAt(expiresAt)
                .tokenKind(tokenKind != null ? tokenKind : "UNKNOWN")
                .build());
    }

    @Transactional(readOnly = true)
    public boolean isRevoked(String tokenId) {
        if (tokenId == null || tokenId.isBlank())
            return false;
        return invalidatedTokenRepository.existsById(tokenId);
    }

    @Transactional
    public void cleanupExpired() {
        invalidatedTokenRepository.deleteByExpiresAtBefore(Instant.now());
    }
}
