package com.sparktech.motorx.metrics;

import com.sparktech.motorx.Services.IMetricsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.FilterChain;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PerformanceMetricsFilter - Unit Tests")
class PerformanceMetricsFilterTest {

    @Mock
    private IMetricsService metricsService;

    @Test
    @DisplayName("Mide endpoint trackeado y registra tiempo")
    void shouldRecordTimeForTrackedEndpoint() throws Exception {
        PerformanceMetricsFilter filter = new PerformanceMetricsFilter(metricsService);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> {
        };

        filter.doFilter(request, response, chain);

        verify(metricsService).recordEndpointResponseTime(eq("/api/auth/login"), anyLong());
    }

    @Test
    @DisplayName("No registra métricas para endpoint no trackeado")
    void shouldIgnoreUntrackedEndpoint() throws Exception {
        PerformanceMetricsFilter filter = new PerformanceMetricsFilter(metricsService);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/public/ping");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> {
        };

        filter.doFilter(request, response, chain);

        verifyNoInteractions(metricsService);
    }
}

