package com.hoang.jwtjava.controller;

import com.hoang.jwtjava.dto.request.AuthenticationRequest;
import com.hoang.jwtjava.dto.request.IntrospectRequest;
import com.hoang.jwtjava.dto.request.LogoutRequest;
import com.hoang.jwtjava.dto.request.RefreshTokenRequest;
import com.hoang.jwtjava.dto.request.UserCreationRequest;
import com.hoang.jwtjava.dto.response.ApiResponse;
import com.hoang.jwtjava.dto.response.AuthenticationResponse;
import com.hoang.jwtjava.dto.response.IntrospectResponse;
import com.hoang.jwtjava.dto.response.UserResponse;
import com.hoang.jwtjava.service.AuthenticationService;
import com.hoang.jwtjava.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Authentication")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService authenticationService;
    private final UserService userService;

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
     * POST /api/v1/auth/login — returns access and refresh tokens.
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthenticationResponse>> login(@RequestBody @Valid AuthenticationRequest request) {
        return ResponseEntity.ok(ApiResponse.<AuthenticationResponse>builder()
                .message("Login successful")
                .result(authenticationService.authenticate(request))
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
     * POST /api/v1/auth/refresh — exchange refresh token for new tokens.
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthenticationResponse>> refresh(@RequestBody @Valid RefreshTokenRequest request) {
        return ResponseEntity.ok(ApiResponse.<AuthenticationResponse>builder()
                .message("Token refreshed successfully")
                .result(authenticationService.refresh(request))
                .build());
    }

    /**
     * POST /api/v1/auth/logout — revoke current access token and optional refresh token.
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody(required = false) LogoutRequest request) {
        String refreshToken = request != null ? request.getRefreshToken() : null;
        authenticationService.logout(jwt.getTokenValue(), refreshToken);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message("Logout successful")
                .build());
    }
}
