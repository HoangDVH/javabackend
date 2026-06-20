package com.hoang.jwtjava.service;

import com.hoang.jwtjava.entity.InvalidatedToken;
import com.hoang.jwtjava.repository.InvalidatedTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class DbTokenRevocationStore implements TokenRevocationStore {

    private final InvalidatedTokenRepository invalidatedTokenRepository;

    @Override
    @Transactional
    public void revoke(String tokenId, Instant expiresAt, String tokenKind) {
        if (tokenId == null || tokenId.isBlank() || expiresAt == null)
            return;
        if (expiresAt.isBefore(Instant.now()))
            return;

        invalidatedTokenRepository.save(InvalidatedToken.builder()
                .id(tokenId)
                .expiresAt(expiresAt)
                .tokenKind(tokenKind != null ? tokenKind : "UNKNOWN")
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isRevoked(String tokenId) {
        if (tokenId == null || tokenId.isBlank())
            return false;
        return invalidatedTokenRepository.existsById(tokenId);
    }
}
