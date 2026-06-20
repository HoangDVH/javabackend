package com.hoang.jwtjava.controller;

import com.hoang.jwtjava.dto.request.AuthenticationRequest;
import com.hoang.jwtjava.dto.request.ForgotPasswordRequest;
import com.hoang.jwtjava.dto.request.IntrospectRequest;
import com.hoang.jwtjava.dto.request.LogoutRequest;
import com.hoang.jwtjava.dto.request.ResetPasswordRequest;
import com.hoang.jwtjava.dto.request.UserCreationRequest;
import com.hoang.jwtjava.dto.response.ApiResponse;
import com.hoang.jwtjava.dto.response.AuthenticationResponse;
import com.hoang.jwtjava.dto.response.IntrospectResponse;
import com.hoang.jwtjava.dto.response.UserResponse;
import com.hoang.jwtjava.service.AuthenticationService;
import com.hoang.jwtjava.service.AuthenticationService.AuthTokens;
import com.hoang.jwtjava.service.PasswordResetService;
import com.hoang.jwtjava.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
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
    private final PasswordResetService passwordResetService;

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
    @SecurityRequirements
    @Operation(
            summary = "Register new user",
            description = "Public. Rate limit Redis: 5 req / 5 phút theo IP.")
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
    @SecurityRequirements
    @Operation(
            summary = "Login",
            description = """
                    Public. Trả access token JSON; set HttpOnly cookie `refresh_token`.
                    Rate limit Redis: 10 req/phút theo IP, 5 req/phút theo email.
                    """)
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
     * POST /api/v1/auth/forgot-password — send reset link via email (Resend).
     */
    @PostMapping("/forgot-password")
    @SecurityRequirements
    @Operation(
            summary = "Forgot password",
            description = """
                    Public. Gửi link đặt lại mật khẩu qua email (Resend) nếu tài khoản tồn tại.
                    Luôn trả 200 để không tiết lộ email có hay không.
                    Rate limit: 5 req/15 phút theo IP, 3 req/15 phút theo email.
                    """)
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@RequestBody @Valid ForgotPasswordRequest request) {
        passwordResetService.requestPasswordReset(request);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message("If an account exists for this email, password reset instructions have been sent.")
                .build());
    }

    /**
     * POST /api/v1/auth/reset-password — set new password using token from email link.
     */
    @PostMapping("/reset-password")
    @SecurityRequirements
    @Operation(
            summary = "Reset password",
            description = """
                    Public. Đặt mật khẩu mới bằng token từ link email (?token=...).
                    Token one-time, TTL 30 phút. Rate limit: 10 req/phút theo IP.
                    """)
    public ResponseEntity<ApiResponse<Void>> resetPassword(@RequestBody @Valid ResetPasswordRequest request) {
        passwordResetService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message("Password reset successful. You can log in with your new password.")
                .build());
    }

    /**
     * POST /api/v1/auth/introspect — validate an access token.
     */
    @PostMapping("/introspect")
    @SecurityRequirements
    @Operation(summary = "Introspect access token", description = "Public. Kiểm tra token còn hợp lệ (kể cả blacklist Redis/DB).")
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
            description = """
                    Public. Đọc refresh token từ HttpOnly cookie only — không cần request body.
                    Rate limit Redis: 20 req/phút theo IP. Set lại cookie refresh mới.
                    """)
    @SecurityRequirements
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
    @Operation(
            summary = "Logout",
            description = """
                    Yêu cầu JWT. Revoke access token (Redis blacklist + DB) và refresh token nếu có.
                    Xóa cookie `refresh_token`.
                    """)
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
