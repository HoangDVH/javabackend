package com.hoang.jwtjava.service;

import com.hoang.jwtjava.config.GoogleAuthProperties;
import com.hoang.jwtjava.config.RateLimitProperties;
import com.hoang.jwtjava.dto.request.AuthenticationRequest;
import com.hoang.jwtjava.dto.request.IntrospectRequest;
import com.hoang.jwtjava.dto.response.IntrospectResponse;
import com.hoang.jwtjava.entity.User;
import com.hoang.jwtjava.exception.AppException;
import com.hoang.jwtjava.exception.ErrorCode;
import com.hoang.jwtjava.exception.InvalidCredentialsException;
import com.hoang.jwtjava.exception.RateLimitExceededException;
import com.hoang.jwtjava.repository.UserRepository;
import com.hoang.jwtjava.security.JwtTokenKind;
import com.hoang.jwtjava.service.GoogleTokenVerifier.GoogleProfile;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Key;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenRevocationService tokenRevocationService;
    private final RateLimitService rateLimitService;
    private final RateLimitProperties rateLimitProperties;
    private final GoogleTokenVerifier googleTokenVerifier;
    private final GoogleAuthProperties googleAuthProperties;

    @Value("${jwt.signer-key}")
    private String signerKey;

    @Value("${jwt.valid-duration}")
    private long validDuration;

    @Value("${jwt.refreshable-duration}")
    private long refreshableDuration;

    public AuthTokens authenticate(AuthenticationRequest request) {
        enforceLoginEmailRateLimit(request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(InvalidCredentialsException::new);

        if (user.getPassword() == null || user.getPassword().isBlank())
            throw new InvalidCredentialsException();
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword()))
            throw new InvalidCredentialsException();

        return new AuthTokens(generateAccessToken(user), generateRefreshToken(user));
    }

    /**
     * Google Sign-In: verify ID token, find-or-create user by email, issue app JWT.
     */
    @Transactional
    public AuthTokens authenticateWithGoogle(String idToken) {
        if (!googleAuthProperties.isEnabled())
            throw new AppException(ErrorCode.GOOGLE_AUTH_DISABLED);

        GoogleProfile profile = googleTokenVerifier.verify(idToken);
        enforceLoginEmailRateLimit(profile.email());

        User user = userRepository.findByEmail(profile.email()).orElse(null);
        if (user == null) {
            user = User.builder()
                    .email(profile.email())
                    .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .fullName(blankToNull(profile.name()))
                    .roles(Set.of("USER"))
                    .build();
            user = userRepository.save(user);
        } else if ((user.getFullName() == null || user.getFullName().isBlank())
                && profile.name() != null && !profile.name().isBlank()) {
            user.setFullName(profile.name().trim());
            user = userRepository.save(user);
        }

        return new AuthTokens(generateAccessToken(user), generateRefreshToken(user));
    }

    public AuthTokens refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank())
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        Claims claims;
        try {
            claims = getClaims(refreshToken);
        } catch (JwtException e) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        if (!JwtTokenKind.REFRESH.equals(claims.get(JwtTokenKind.CLAIM_NAME, String.class)))
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        if (tokenRevocationService.isRevoked(claims.getId()))
            throw new AppException(ErrorCode.UNAUTHENTICATED);

        User user = userRepository.findByEmail(claims.getSubject())
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));

        return new AuthTokens(generateAccessToken(user), generateRefreshToken(user));
    }

    public IntrospectResponse introspect(IntrospectRequest request) {
        boolean isValid = true;
        try {
            Claims c = getClaims(request.getToken());
            if (!JwtTokenKind.ACCESS.equals(c.get(JwtTokenKind.CLAIM_NAME, String.class)))
                isValid = false;
            if (tokenRevocationService.isRevoked(c.getId()))
                isValid = false;
        } catch (JwtException e) {
            isValid = false;
        }
        return IntrospectResponse.builder().valid(isValid).build();
    }

    public void logout(String accessToken, String refreshToken) {
        tokenRevocationService.cleanupExpired();
        revokeTokenSilently(accessToken, JwtTokenKind.ACCESS);
        revokeTokenSilently(refreshToken, JwtTokenKind.REFRESH);
    }

    private String generateAccessToken(User user) {
        return Jwts.builder()
                .setSubject(user.getEmail())
                .setId(UUID.randomUUID().toString())
                .setIssuedAt(new Date())
                .setExpiration(Date.from(Instant.now().plus(validDuration, ChronoUnit.SECONDS)))
                .claim(JwtTokenKind.CLAIM_NAME, JwtTokenKind.ACCESS)
                .claim("scope", buildScope(user))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    private String generateRefreshToken(User user) {
        return Jwts.builder()
                .setSubject(user.getEmail())
                .setId(UUID.randomUUID().toString())
                .setIssuedAt(new Date())
                .setExpiration(Date.from(Instant.now().plus(refreshableDuration, ChronoUnit.SECONDS)))
                .claim(JwtTokenKind.CLAIM_NAME, JwtTokenKind.REFRESH)
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    private Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private void revokeTokenSilently(String token, String expectedKind) {
        if (token == null || token.isBlank())
            return;
        try {
            Claims claims = getClaims(token);
            String tokenKind = claims.get(JwtTokenKind.CLAIM_NAME, String.class);
            if (!expectedKind.equals(tokenKind))
                return;
            tokenRevocationService.revoke(claims);
        } catch (JwtException ignored) {
            // Token invalid/expired already; nothing left to revoke.
        }
    }

    private Key getSignInKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(signerKey));
    }

    private String buildScope(User user) {
        if (user.getRoles() == null || user.getRoles().isEmpty())
            return "";
        return String.join(" ", user.getRoles());
    }

    private void enforceLoginEmailRateLimit(String email) {
        if (email == null || email.isBlank())
            return;
        var decision = rateLimitService.check(
                "login-email",
                email.trim().toLowerCase(),
                rateLimitProperties.getLoginByEmail());
        if (!decision.allowed())
            throw new RateLimitExceededException(decision.retryAfterSeconds());
    }

    private static String blankToNull(String value) {
        if (value == null)
            return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    @Getter
    @AllArgsConstructor
    public static class AuthTokens {
        private String accessToken;
        private String refreshToken;
    }
}
