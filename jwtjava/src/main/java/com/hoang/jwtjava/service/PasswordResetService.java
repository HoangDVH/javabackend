package com.hoang.jwtjava.service;

import com.hoang.jwtjava.config.RateLimitProperties;
import com.hoang.jwtjava.dto.request.ForgotPasswordRequest;
import com.hoang.jwtjava.dto.request.ResetPasswordRequest;
import com.hoang.jwtjava.entity.User;
import com.hoang.jwtjava.exception.AppException;
import com.hoang.jwtjava.exception.ErrorCode;
import com.hoang.jwtjava.exception.RateLimitExceededException;
import com.hoang.jwtjava.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordResetTokenStore passwordResetTokenStore;
    private final MailService mailService;
    private final PasswordEncoder passwordEncoder;
    private final RateLimitService rateLimitService;
    private final RateLimitProperties rateLimitProperties;

    @Transactional
    public void requestPasswordReset(ForgotPasswordRequest request) {
        String email = normalizeEmail(request.getEmail());
        enforceForgotPasswordRateLimit(email);

        userRepository.findByEmail(email).ifPresent(user -> {
            String rawToken = generateRawToken();
            String tokenHash = hashToken(rawToken);
            passwordResetTokenStore.save(tokenHash, user.getEmail());
            try {
                mailService.sendPasswordResetEmail(user.getEmail(), rawToken);
            } catch (RuntimeException ex) {
                log.error("Failed to send password reset email to {}: {}", user.getEmail(), ex.getMessage());
            }
        });
    }

    public void resetPassword(ResetPasswordRequest request) {
        if (request.getToken() == null || request.getToken().isBlank())
            throw new AppException(ErrorCode.INVALID_RESET_TOKEN);

        String email = passwordResetTokenStore.consume(hashToken(request.getToken().trim()))
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_RESET_TOKEN));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_RESET_TOKEN));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    private void enforceForgotPasswordRateLimit(String email) {
        var decision = rateLimitService.check(
                "forgot-password-email",
                email,
                rateLimitProperties.getForgotPasswordByEmail());
        if (!decision.allowed())
            throw new RateLimitExceededException(decision.retryAfterSeconds());
    }

    private static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private static String generateRawToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }
}
