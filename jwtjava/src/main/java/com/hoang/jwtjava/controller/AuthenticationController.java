package com.hoang.jwtjava.controller;

import com.hoang.jwtjava.dto.request.AuthenticationRequest;
import com.hoang.jwtjava.dto.request.IntrospectRequest;
import com.hoang.jwtjava.dto.request.LogoutRequest;
import com.hoang.jwtjava.dto.request.UserCreationRequest;
import com.hoang.jwtjava.dto.response.ApiResponse;
import com.hoang.jwtjava.dto.response.AuthenticationResponse;
import com.hoang.jwtjava.dto.response.IntrospectResponse;
import com.hoang.jwtjava.dto.response.UserResponse;
import com.hoang.jwtjava.service.AuthenticationService;
import com.hoang.jwtjava.service.AuthenticationService.AuthTokens;
import com.hoang.jwtjava.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;

@Tag(name = "Authentication")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService authenticationService;
    private final UserService userService;

    @Value("${jwt.refresh-cookie.name:refresh_token}")
    private String refreshCookieName;

    /**
     * Default: Lax (works for localhost different ports).
     * If your frontend is on a different site, set to None and enable secure=true (HTTPS required).
     */
    @Value("${jwt.refresh-cookie.same-site:Lax}")
    private String refreshCookieSameSite;

    @Value("${jwt.refresh-cookie.secure:false}")
    private boolean refreshCookieSecure;

    @Value("${jwt.refreshable-duration}")
    private long refreshCookieMaxAgeSeconds;

    /**
     * POST /api/v1/auth/register — public user registration.
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(@RequestBody @Valid UserCreationRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.<UserResponse>builder()
                        .result(userService.createUser(request))
                        .build());
    }

    /**
     * POST /api/v1/auth/login — returns access token and sets refresh token cookie.
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthenticationResponse>> login(@RequestBody @Valid AuthenticationRequest request) {
        AuthTokens tokens = authenticationService.authenticate(request);
        ResponseCookie refreshCookie = buildRefreshCookie(tokens.getRefreshToken());
        AuthenticationResponse body = AuthenticationResponse.builder()
                .accessToken(tokens.getAccessToken())
                .authenticated(true)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(ApiResponse.<AuthenticationResponse>builder()
                        .message("Login successful")
                        .result(body)
                        .build());
    }

    /**
     * POST /api/v1/auth/introspect — validate an access token.
     */
    @PostMapping("/introspect")
    public ResponseEntity<ApiResponse<IntrospectResponse>> introspect(@RequestBody IntrospectRequest request) {
        return ResponseEntity.ok(ApiResponse.<IntrospectResponse>builder()
                .result(authenticationService.introspect(request))
                .build());
    }

    /**
     * POST /api/v1/auth/refresh — exchange refresh token from HttpOnly cookie for new tokens.
     */
    @Operation(
            summary = "Refresh access token (cookie-based)",
            description = "Reads refresh token from HttpOnly cookie only. No request body."
    )
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthenticationResponse>> refresh(
            @Parameter(hidden = true) HttpServletRequest httpRequest) {
        String refreshTokenFromCookie = readCookie(httpRequest, refreshCookieName);
        AuthTokens tokens = authenticationService.refresh(refreshTokenFromCookie);

        ResponseCookie refreshCookie = buildRefreshCookie(tokens.getRefreshToken());
        AuthenticationResponse body = AuthenticationResponse.builder()
                .accessToken(tokens.getAccessToken())
                .authenticated(true)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(ApiResponse.<AuthenticationResponse>builder()
                        .message("Token refreshed successfully")
                        .result(body)
                        .build());
    }

    /**
     * POST /api/v1/auth/logout — revoke current access token and optional refresh token.
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest httpRequest,
            @RequestBody(required = false) LogoutRequest request) {
        String refreshTokenFromCookie = readCookie(httpRequest, refreshCookieName);
        String refreshToken = (request != null && request.getRefreshToken() != null && !request.getRefreshToken().isBlank())
                ? request.getRefreshToken()
                : refreshTokenFromCookie;
        authenticationService.logout(jwt.getTokenValue(), refreshToken);

        ResponseCookie clearCookie = clearRefreshCookie();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, clearCookie.toString())
                .body(ApiResponse.<Void>builder()
                        .message("Logout successful")
                        .build());
    }

    private ResponseCookie buildRefreshCookie(String refreshToken) {
        return ResponseCookie.from(refreshCookieName, refreshToken)
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .sameSite(refreshCookieSameSite)
                .path("/api/v1/auth")
                .maxAge(Duration.ofSeconds(refreshCookieMaxAgeSeconds))
                .build();
    }

    private ResponseCookie clearRefreshCookie() {
        return ResponseCookie.from(refreshCookieName, "")
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .sameSite(refreshCookieSameSite)
                .path("/api/v1/auth")
                .maxAge(Duration.ZERO)
                .build();
    }

    private String readCookie(HttpServletRequest request, String name) {
        if (request == null || request.getCookies() == null)
            return null;
        for (var c : request.getCookies()) {
            if (c != null && name.equals(c.getName()))
                return c.getValue();
        }
        return null;
    }
}
