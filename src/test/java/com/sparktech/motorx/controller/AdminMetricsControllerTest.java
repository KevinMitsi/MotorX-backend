package com.sparktech.motorx.controller;

import com.sparktech.motorx.Services.IMetricsService;
import com.sparktech.motorx.controller.error.GlobalControllerAdvice;
import com.sparktech.motorx.dto.metrics.*;
import com.sparktech.motorx.security.CustomUserDetailsService;
import com.sparktech.motorx.security.JwtAuthenticationFilter;
import com.sparktech.motorx.security.JwtService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AdminMetricsController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalControllerAdvice.class, AdminMetricsControllerTest.TestConfig.class})
@DisplayName("AdminMetricsController - Tests")
class AdminMetricsControllerTest {

    @Resource
    private MockMvc mockMvc;

    @Resource
    private IMetricsService metricsService;

    @Test
    @DisplayName("GET /performance retorna lista de performance")
    void shouldGetPerformanceMetrics() throws Exception {
        when(metricsService.getPerformanceMetrics()).thenReturn(List.of(
                new PerformanceMetricsDTO("/api/auth/login", 320, 54, 54, 100.0)
        ));

        mockMvc.perform(get("/api/v1/admin/metrics/performance"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].endpoint", is("/api/auth/login")))
                .andExpect(jsonPath("$[0].avgResponseTimeMs", is(320)));

        verify(metricsService).getPerformanceMetrics();
    }

    @Test
    @DisplayName("GET /security retorna métricas de seguridad")
    void shouldGetSecurityMetrics() throws Exception {
        when(metricsService.getSecurityMetrics()).thenReturn(
                new SecurityMetricsDTO(12, 3, 18, 18, 100.0)
        );

        mockMvc.perform(get("/api/v1/admin/metrics/security"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unauthorizedAttempts401", is(12)))
                .andExpect(jsonPath("$.forbiddenAttempts403", is(3)))
                .andExpect(jsonPath("$.accessControlCompliancePercent", is(100.0)));

        verify(metricsService).getSecurityMetrics();
    }

    @Test
    @DisplayName("GET /maintainability retorna métricas estructurales")
    void shouldGetMaintainabilityMetrics() throws Exception {
        when(metricsService.getMaintainabilityMetrics()).thenReturn(
                new MaintainabilityMetricsDTO(5, 6, 4, true, 60)
        );

        mockMvc.perform(get("/api/v1/admin/metrics/maintainability"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalControllers", is(5)))
                .andExpect(jsonPath("$.standardizedErrorHandlingEnabled", is(true)))
                .andExpect(jsonPath("$.jacocoCoverageGatePercent", is(60)));

        verify(metricsService).getMaintainabilityMetrics();
    }

    @Test
    @DisplayName("GET /appointments retorna métricas de citas")
    void shouldGetAppointmentsMetrics() throws Exception {
        when(metricsService.getAppointmentsMetrics()).thenReturn(
                new AppointmentsMetricsDTO(30, 27, 3, 100.0, 27, 27, 100.0)
        );

        mockMvc.perform(get("/api/v1/admin/metrics/appointments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCreationAttempts", is(30)))
                .andExpect(jsonPath("$.successfulAppointments", is(27)))
                .andExpect(jsonPath("$.dataIntegrityPercent", is(100.0)));

        verify(metricsService).getAppointmentsMetrics();
    }

    @Test
    @DisplayName("GET /summary retorna consolidado")
    void shouldGetSummaryMetrics() throws Exception {
        MetricsSummaryDTO summary = new MetricsSummaryDTO(
                List.of(new PerformanceMetricsDTO("/api/auth/login", 100, 1, 1, 100.0)),
                new SecurityMetricsDTO(1, 0, 2, 2, 100.0),
                new MaintainabilityMetricsDTO(1, 1, 1, true, 60),
                new AppointmentsMetricsDTO(1, 1, 0, 100.0, 1, 1, 100.0)
        );
        when(metricsService.getSummaryMetrics()).thenReturn(summary);

        mockMvc.perform(get("/api/v1/admin/metrics/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.performance", hasSize(1)))
                .andExpect(jsonPath("$.security.unauthorizedAttempts401", is(1)))
                .andExpect(jsonPath("$.appointments.totalAppointmentsInDB", is(1)));

        verify(metricsService).getSummaryMetrics();
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        IMetricsService metricsService() {
            return mock(IMetricsService.class);
        }

        @Bean
        @Primary
        JwtService jwtService() {
            return mock(JwtService.class);
        }

        @Bean
        @Primary
        JwtAuthenticationFilter jwtAuthenticationFilter() {
            return mock(JwtAuthenticationFilter.class);
        }

        @Bean
        @Primary
        CustomUserDetailsService customUserDetailsService() {
            return mock(CustomUserDetailsService.class);
        }
    }
}

