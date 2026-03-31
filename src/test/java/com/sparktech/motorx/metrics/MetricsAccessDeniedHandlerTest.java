package com.sparktech.motorx.metrics;

import com.sparktech.motorx.Services.IMetricsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MetricsAccessDeniedHandler - Unit Tests")
class MetricsAccessDeniedHandlerTest {

    @Mock
    private IMetricsService metricsService;

    @Test
    @DisplayName("Cuenta 403 en ruta admin y responde JSON")
    void shouldCountForbiddenForAdminEndpoint() throws Exception {
        MetricsAccessDeniedHandler handler = new MetricsAccessDeniedHandler(metricsService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/admin/users");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.handle(request, response, new AccessDeniedException("forbidden"));

        verify(metricsService).recordForbiddenAttempt("/api/v1/admin/users");
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).contains("application/json");
        assertThat(response.getContentAsString()).contains("Acceso denegado");
    }

    @Test
    @DisplayName("No cuenta 403 en rutas no admin")
    void shouldNotCountForbiddenForNonAdminEndpoint() throws Exception {
        MetricsAccessDeniedHandler handler = new MetricsAccessDeniedHandler(metricsService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/user/appointments");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.handle(request, response, new AccessDeniedException("forbidden"));

        verifyNoInteractions(metricsService);
    }
}

