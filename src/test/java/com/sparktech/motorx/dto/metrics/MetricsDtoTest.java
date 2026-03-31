package com.sparktech.motorx.dto.metrics;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Metrics DTO - Unit Tests")
class MetricsDtoTest {

    @Test
    @DisplayName("PerformanceMetricsDTO expone sus componentes")
    void shouldCreatePerformanceMetricsDto() {
        PerformanceMetricsDTO dto = new PerformanceMetricsDTO("/api/auth/login", 320, 54, 54, 100.0);

        assertThat(dto.endpoint()).isEqualTo("/api/auth/login");
        assertThat(dto.avgResponseTimeMs()).isEqualTo(320);
        assertThat(dto.totalRequests()).isEqualTo(54);
        assertThat(dto.requestsUnderThreshold()).isEqualTo(54);
        assertThat(dto.compliancePercent()).isEqualTo(100.0);
    }

    @Test
    @DisplayName("SecurityMetricsDTO expone sus componentes")
    void shouldCreateSecurityMetricsDto() {
        SecurityMetricsDTO dto = new SecurityMetricsDTO(12, 3, 18, 18, 100.0);

        assertThat(dto.unauthorizedAttempts401()).isEqualTo(12);
        assertThat(dto.forbiddenAttempts403()).isEqualTo(3);
        assertThat(dto.totalProtectedEndpoints()).isEqualTo(18);
        assertThat(dto.endpointsWithAuthEnforced()).isEqualTo(18);
        assertThat(dto.accessControlCompliancePercent()).isEqualTo(100.0);
    }

    @Test
    @DisplayName("MaintainabilityMetricsDTO expone sus componentes")
    void shouldCreateMaintainabilityMetricsDto() {
        MaintainabilityMetricsDTO dto = new MaintainabilityMetricsDTO(5, 6, 4, true, 60);

        assertThat(dto.totalControllers()).isEqualTo(5);
        assertThat(dto.totalServices()).isEqualTo(6);
        assertThat(dto.totalRepositories()).isEqualTo(4);
        assertThat(dto.standardizedErrorHandlingEnabled()).isTrue();
        assertThat(dto.jacocoCoverageGatePercent()).isEqualTo(60);
    }

    @Test
    @DisplayName("AppointmentsMetricsDTO expone sus componentes")
    void shouldCreateAppointmentsMetricsDto() {
        AppointmentsMetricsDTO dto = new AppointmentsMetricsDTO(30, 27, 3, 100.0, 27, 27, 100.0);

        assertThat(dto.totalCreationAttempts()).isEqualTo(30);
        assertThat(dto.successfulAppointments()).isEqualTo(27);
        assertThat(dto.rejectedByBusinessRules()).isEqualTo(3);
        assertThat(dto.businessRuleCompliancePercent()).isEqualTo(100.0);
        assertThat(dto.totalAppointmentsInDB()).isEqualTo(27);
        assertThat(dto.validRecordsInDB()).isEqualTo(27);
        assertThat(dto.dataIntegrityPercent()).isEqualTo(100.0);
    }

    @Test
    @DisplayName("MetricsSummaryDTO consolida los cuatro bloques")
    void shouldCreateSummaryDto() {
        MetricsSummaryDTO dto = new MetricsSummaryDTO(
                List.of(new PerformanceMetricsDTO("/api/auth/login", 100, 1, 1, 100.0)),
                new SecurityMetricsDTO(1, 0, 2, 2, 100.0),
                new MaintainabilityMetricsDTO(1, 1, 1, true, 60),
                new AppointmentsMetricsDTO(1, 1, 0, 100.0, 1, 1, 100.0)
        );

        assertThat(dto.performance()).hasSize(1);
        assertThat(dto.security().unauthorizedAttempts401()).isEqualTo(1);
        assertThat(dto.maintainability().jacocoCoverageGatePercent()).isEqualTo(60);
        assertThat(dto.appointments().totalAppointmentsInDB()).isEqualTo(1);
    }
}

