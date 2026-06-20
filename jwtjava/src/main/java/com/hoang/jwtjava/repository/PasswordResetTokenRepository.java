package com.hoang.jwtjava.repository;

import com.hoang.jwtjava.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, String> {

    void deleteByEmailIgnoreCase(String email);
}
