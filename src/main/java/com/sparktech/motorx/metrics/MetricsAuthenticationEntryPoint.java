package com.sparktech.motorx.metrics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparktech.motorx.Services.IMetricsService;
import com.sparktech.motorx.dto.error.ResponseErrorDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class MetricsAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final IMetricsService metricsService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request,
                         @NotNull HttpServletResponse response,
                         @NotNull AuthenticationException authException) throws IOException {
        String endpoint = request.getRequestURI();
        String authHeader = request.getHeader("Authorization");

        if (isProtected(endpoint) && (authHeader == null || authHeader.isBlank() || !authHeader.startsWith("Bearer "))) {
            metricsService.recordUnauthorizedAttemptWithoutToken(endpoint);
        }

        ResponseErrorDTO body = new ResponseErrorDTO(
                HttpServletResponse.SC_UNAUTHORIZED,
                "No autorizado",
                Map.of("detalle", "Se requiere un token JWT válido")
        );

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), body);
    }

    private boolean isProtected(String endpoint) {
        return endpoint != null && (endpoint.startsWith("/api/v1/user/") || endpoint.startsWith("/api/v1/admin/"));
    }
}

