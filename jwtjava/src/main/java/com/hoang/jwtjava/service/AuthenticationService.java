package com.hoang.jwtjava.service;

import com.hoang.jwtjava.dto.request.AuthenticationRequest;
import com.hoang.jwtjava.dto.request.IntrospectRequest;
import com.hoang.jwtjava.dto.response.IntrospectResponse;
import com.hoang.jwtjava.entity.User;
import com.hoang.jwtjava.exception.AppException;
import com.hoang.jwtjava.exception.ErrorCode;
import com.hoang.jwtjava.exception.InvalidCredentialsException;
import com.hoang.jwtjava.repository.UserRepository;
import com.hoang.jwtjava.security.JwtTokenKind;
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

import java.security.Key;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenRevocationService tokenRevocationService;

    @Value("${jwt.signer-key}")
    private String signerKey;

    @Value("${jwt.valid-duration}")
    private long validDuration;

    @Value("${jwt.refreshable-duration}")
    private long refreshableDuration;

    public AuthTokens authenticate(AuthenticationRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword()))
            throw new InvalidCredentialsException();

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

    @Getter
    @AllArgsConstructor
    public static class AuthTokens {
        private String accessToken;
        private String refreshToken;
    }
}
