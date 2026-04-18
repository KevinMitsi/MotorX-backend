package com.sparktech.motorx.dto.metrics;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

    @Test
    @DisplayName("TopSellingSpareMetricDTO expone sus componentes")
    void shouldCreateTopSellingSpareMetricDto() {
        TopSellingSpareMetricDTO dto = new TopSellingSpareMetricDTO(2L, "Filtro", "SAV-2", 45);

        assertThat(dto.spareId()).isEqualTo(2L);
        assertThat(dto.spareName()).isEqualTo("Filtro");
        assertThat(dto.savCode()).isEqualTo("SAV-2");
        assertThat(dto.unitsSold()).isEqualTo(45);
    }

    @Test
    @DisplayName("InventoryProfitMetricsDTO expone sus componentes")
    void shouldCreateInventoryProfitMetricsDto() {
        InventoryProfitMetricsDTO dto = new InventoryProfitMetricsDTO(
                LocalDate.parse("2026-01-01"),
                LocalDate.parse("2026-01-31"),
                12,
                new BigDecimal("1620.00"),
                new BigDecimal("420.00")
        );

        assertThat(dto.startDate()).isEqualTo(LocalDate.parse("2026-01-01"));
        assertThat(dto.endDate()).isEqualTo(LocalDate.parse("2026-01-31"));
        assertThat(dto.totalUnitsSold()).isEqualTo(12);
        assertThat(dto.grossSalesAmount()).isEqualByComparingTo("1620.00");
        assertThat(dto.estimatedProfitAmount()).isEqualByComparingTo("420.00");
    }

    @Test
    @DisplayName("StagnantSpareMetricDTO expone sus componentes")
    void shouldCreateStagnantSpareMetricDto() {
        LocalDateTime lastSale = LocalDateTime.parse("2025-12-01T09:00:00");
        StagnantSpareMetricDTO dto = new StagnantSpareMetricDTO(8L, "Bujia", "SAV-8", 5, lastSale, 140L, false);

        assertThat(dto.spareId()).isEqualTo(8L);
        assertThat(dto.currentStock()).isEqualTo(5);
        assertThat(dto.lastSaleDate()).isEqualTo(lastSale);
        assertThat(dto.daysWithoutSales()).isEqualTo(140L);
        assertThat(dto.neverSold()).isFalse();
    }

    @Test
    @DisplayName("InventoryThresholdMetricsDTO expone sus componentes")
    void shouldCreateInventoryThresholdMetricsDto() {
        InventoryThresholdMetricsDTO dto = new InventoryThresholdMetricsDTO(4, 10, 40.0);

        assertThat(dto.sparesBelowThreshold()).isEqualTo(4);
        assertThat(dto.sparesWithThreshold()).isEqualTo(10);
        assertThat(dto.belowThresholdPercent()).isEqualTo(40.0);
    }
}

