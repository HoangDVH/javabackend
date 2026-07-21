package com.hoang.jwtjava.service;

import com.hoang.jwtjava.config.GoogleAuthProperties;
import com.hoang.jwtjava.config.RateLimitProperties;
import com.hoang.jwtjava.entity.User;
import com.hoang.jwtjava.exception.AppException;
import com.hoang.jwtjava.exception.ErrorCode;
import com.hoang.jwtjava.repository.UserRepository;
import com.hoang.jwtjava.service.GoogleTokenVerifier.GoogleProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoogleAuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private TokenRevocationService tokenRevocationService;
    @Mock
    private RateLimitService rateLimitService;
    @Mock
    private GoogleTokenVerifier googleTokenVerifier;

    private GoogleAuthProperties googleAuthProperties;
    private RateLimitProperties rateLimitProperties;
    private AuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        googleAuthProperties = new GoogleAuthProperties();
        googleAuthProperties.setEnabled(true);
        googleAuthProperties.setClientId("test-client.apps.googleusercontent.com");

        rateLimitProperties = new RateLimitProperties();
        org.mockito.Mockito.lenient().when(rateLimitService.check(eq("login-email"), anyString(), any()))
                .thenReturn(RateLimitService.RateLimitDecision.allowed(5));

        authenticationService = new AuthenticationService(
                userRepository,
                passwordEncoder,
                tokenRevocationService,
                rateLimitService,
                rateLimitProperties,
                googleTokenVerifier,
                googleAuthProperties);
        ReflectionTestUtils.setField(authenticationService, "signerKey", "bXktc2VjcmV0LWp3dC1zaWduaW5nLWtleS0yNTZiaXQ=");
        ReflectionTestUtils.setField(authenticationService, "validDuration", 3600L);
        ReflectionTestUtils.setField(authenticationService, "refreshableDuration", 36000L);
    }

    @Test
    void googleLoginCreatesNewUser() {
        when(googleTokenVerifier.verify("token")).thenReturn(
                new GoogleProfile("new@gmail.com", "New User", "https://lh3.googleusercontent.com/a/photo", "sub-1"));
        when(userRepository.findByEmail("new@gmail.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId("id-1");
            return u;
        });

        var tokens = authenticationService.authenticateWithGoogle("token");

        assertNotNull(tokens.getAccessToken());
        assertNotNull(tokens.getRefreshToken());
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals("new@gmail.com", captor.getValue().getEmail());
        assertEquals("New User", captor.getValue().getFullName());
        assertEquals("https://lh3.googleusercontent.com/a/photo", captor.getValue().getAvatarUrl());
        assertEquals(Set.of("USER"), captor.getValue().getRoles());
    }

    @Test
    void googleLoginReusesExistingUser() {
        User existing = User.builder()
                .id("id-2")
                .email("old@gmail.com")
                .fullName("Old")
                .password("hash")
                .roles(Set.of("USER"))
                .build();
        when(googleTokenVerifier.verify("token")).thenReturn(
                new GoogleProfile("old@gmail.com", "Old", null, "sub-2"));
        when(userRepository.findByEmail("old@gmail.com")).thenReturn(Optional.of(existing));

        var tokens = authenticationService.authenticateWithGoogle("token");

        assertNotNull(tokens.getAccessToken());
        verify(userRepository, never()).save(any());
    }

    @Test
    void googleLoginDisabledThrows() {
        googleAuthProperties.setEnabled(false);
        AppException ex = assertThrows(AppException.class,
                () -> authenticationService.authenticateWithGoogle("token"));
        assertEquals(ErrorCode.GOOGLE_AUTH_DISABLED, ex.getErrorCode());
        verify(googleTokenVerifier, never()).verify(any());
    }
}
