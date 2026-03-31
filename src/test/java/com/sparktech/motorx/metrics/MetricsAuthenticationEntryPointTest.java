package com.sparktech.motorx.metrics;

import com.sparktech.motorx.Services.IMetricsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MetricsAuthenticationEntryPoint - Unit Tests")
class MetricsAuthenticationEntryPointTest {

    @Mock
    private IMetricsService metricsService;

    @Test
    @DisplayName("Cuenta 401 sin token en ruta protegida y responde JSON")
    void shouldCountUnauthorizedWithoutToken() throws Exception {
        MetricsAuthenticationEntryPoint entryPoint = new MetricsAuthenticationEntryPoint(metricsService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/user/appointments");
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, mock(org.springframework.security.core.AuthenticationException.class));

        verify(metricsService).recordUnauthorizedAttemptWithoutToken("/api/v1/user/appointments");
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).contains("application/json");
        assertThat(response.getContentAsString()).contains("No autorizado");
    }

    @Test
    @DisplayName("No cuenta 401 si ya hay bearer token")
    void shouldNotCountWhenBearerTokenExists() throws Exception {
        MetricsAuthenticationEntryPoint entryPoint = new MetricsAuthenticationEntryPoint(metricsService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/admin/metrics/security");
        request.addHeader("Authorization", "Bearer token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, mock(org.springframework.security.core.AuthenticationException.class));

        verifyNoInteractions(metricsService);
    }

    @Test
    @DisplayName("No cuenta 401 en ruta pública")
    void shouldNotCountForPublicEndpoint() throws Exception {
        MetricsAuthenticationEntryPoint entryPoint = new MetricsAuthenticationEntryPoint(metricsService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/public/ping");
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, mock(org.springframework.security.core.AuthenticationException.class));

        verifyNoInteractions(metricsService);
    }
}

