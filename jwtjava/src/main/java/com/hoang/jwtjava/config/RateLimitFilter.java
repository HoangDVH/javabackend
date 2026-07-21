package com.hoang.jwtjava.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hoang.jwtjava.dto.response.ApiResponse;
import com.hoang.jwtjava.exception.ErrorCode;
import com.hoang.jwtjava.service.RateLimitService;
import com.hoang.jwtjava.service.RateLimitService.RateLimitDecision;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/api/v1/auth/login";
    private static final String GOOGLE_LOGIN_PATH = "/api/v1/auth/google";
    private static final String REFRESH_PATH = "/api/v1/auth/refresh";
    private static final String REGISTER_PATH = "/api/v1/auth/register";
    private static final String FORGOT_PASSWORD_PATH = "/api/v1/auth/forgot-password";
    private static final String RESET_PASSWORD_PATH = "/api/v1/auth/reset-password";

    private final RateLimitService rateLimitService;
    private final RateLimitProperties rateLimitProperties;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        if (!HttpMethod.POST.matches(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        String clientIp = resolveClientIp(request);

        RateLimitDecision decision = switch (path) {
            case LOGIN_PATH -> rateLimitService.check("login", clientIp, rateLimitProperties.getLogin());
            case GOOGLE_LOGIN_PATH -> rateLimitService.check("google-login", clientIp, rateLimitProperties.getGoogleLogin());
            case REFRESH_PATH -> rateLimitService.check("refresh", clientIp, rateLimitProperties.getRefresh());
            case REGISTER_PATH -> rateLimitService.check("register", clientIp, rateLimitProperties.getRegister());
            case FORGOT_PASSWORD_PATH -> rateLimitService.check("forgot-password", clientIp, rateLimitProperties.getForgotPassword());
            case RESET_PASSWORD_PATH -> rateLimitService.check("reset-password", clientIp, rateLimitProperties.getResetPassword());
            default -> RateLimitDecision.allowed(Integer.MAX_VALUE);
        };

        if (!decision.allowed()) {
            writeTooManyRequests(response, decision.retryAfterSeconds());
            return;
        }

        response.setHeader("X-RateLimit-Remaining", String.valueOf(decision.remaining()));
        filterChain.doFilter(request, response);
    }

    private void writeTooManyRequests(HttpServletResponse response, long retryAfterSeconds) throws IOException {
        ErrorCode errorCode = ErrorCode.TOO_MANY_REQUESTS;
        ApiResponse<Void> body = ApiResponse.<Void>builder()
                .code(errorCode.getCode())
                .message("Too many requests. Try again in " + retryAfterSeconds + " seconds.")
                .build();

        response.setStatus(errorCode.getHttpStatusCode().value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        objectMapper.writeValue(response.getWriter(), body);
    }

    static String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            return (comma >= 0 ? forwarded.substring(0, comma) : forwarded).trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank())
            return realIp.trim();
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
    }
}
