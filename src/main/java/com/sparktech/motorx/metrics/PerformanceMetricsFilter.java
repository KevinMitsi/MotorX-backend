package com.sparktech.motorx.metrics;

import com.sparktech.motorx.Services.IMetricsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class PerformanceMetricsFilter extends OncePerRequestFilter {

    private static final Set<String> TRACKED_ENDPOINTS = Set.of(
            "/api/auth/login",
            "/api/auth/verify-2fa",
            "/api/v1/user/appointments/available-slots"
    );

    private final IMetricsService metricsService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !TRACKED_ENDPOINTS.contains(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long startNs = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long elapsedMs = (System.nanoTime() - startNs) / 1_000_000;
            metricsService.recordEndpointResponseTime(request.getRequestURI(), elapsedMs);
        }
    }
}

