package com.hoang.jwtjava.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Phase 0: log requests slower than the threshold to spot DB/Redis/cold-start bottlenecks on Render.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class SlowRequestLoggingFilter extends OncePerRequestFilter {

    @Value("${app.observability.slow-request-ms:1000}")
    private long slowRequestMs;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        long start = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long elapsedMs = (System.nanoTime() - start) / 1_000_000L;
            if (elapsedMs >= Math.max(1, slowRequestMs)) {
                log.warn(
                        "Slow request method={} path={} status={} elapsedMs={}",
                        request.getMethod(),
                        request.getRequestURI(),
                        response.getStatus(),
                        elapsedMs);
            }
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path != null && (path.startsWith("/swagger")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/actuator"));
    }
}
