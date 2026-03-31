package com.sparktech.motorx.metrics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparktech.motorx.Services.IMetricsService;
import com.sparktech.motorx.dto.error.ResponseErrorDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class MetricsAccessDeniedHandler implements AccessDeniedHandler {

    private final IMetricsService metricsService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void handle(HttpServletRequest request,
                       @NotNull HttpServletResponse response,
                       @NotNull AccessDeniedException accessDeniedException) throws IOException {

        String endpoint = request.getRequestURI();
        if (endpoint != null && endpoint.startsWith("/api/v1/admin/")) {
            metricsService.recordForbiddenAttempt(endpoint);
        }

        ResponseErrorDTO body = new ResponseErrorDTO(
                HttpServletResponse.SC_FORBIDDEN,
                "Acceso denegado",
                Map.of("detalle", "No tienes permisos para acceder a este recurso")
        );

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), body);
    }
}

