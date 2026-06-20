package com.hoang.jwtjava.service;

import java.time.Instant;

public interface TokenRevocationStore {

    void revoke(String tokenId, Instant expiresAt, String tokenKind);

    boolean isRevoked(String tokenId);
}
