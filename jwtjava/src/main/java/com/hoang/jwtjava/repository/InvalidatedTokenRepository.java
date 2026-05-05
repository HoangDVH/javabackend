package com.hoang.jwtjava.repository;

import com.hoang.jwtjava.entity.InvalidatedToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

public interface InvalidatedTokenRepository extends JpaRepository<InvalidatedToken, String> {
    void deleteByExpiresAtBefore(Instant now);
}
