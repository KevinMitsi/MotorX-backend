package com.sparktech.motorx.Services.impl;

import com.sparktech.motorx.Services.IMetricsService;
import com.sparktech.motorx.controller.AdminMetricsController;
import com.sparktech.motorx.dto.metrics.*;
import com.sparktech.motorx.repository.JpaAppointmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MetricsServiceImpl - Unit Tests")
class MetricsServiceImplTest {

    @Mock
    private JpaAppointmentRepository appointmentRepository;

    @Mock
    private RequestMappingHandlerMapping requestMappingHandlerMapping;

    private MetricsServiceImpl sut;

    @BeforeEach
    void setUp() throws Exception {
        Map<String, Object> beans = new HashMap<>();
        beans.put("adminMetricsController", new AdminMetricsController(mock(IMetricsService.class)));
        beans.put("appointmentRepository", appointmentRepository);

        sut = new MetricsServiceImpl(appointmentRepository, requestMappingHandlerMapping, beans);
        beans.put("metricsService", sut);

        Method method = DummyController.class.getDeclaredMethod("dummy");
        HandlerMethod handlerMethod = new HandlerMethod(new DummyController(), method);

        RequestMappingInfo admin = RequestMappingInfo.paths("/api/v1/admin/metrics/performance").build();
        RequestMappingInfo user = RequestMappingInfo.paths("/api/v1/user/appointments").build();
        RequestMappingInfo publicEndpoint = RequestMappingInfo.paths("/api/public/ping").build();

        Map<RequestMappingInfo, HandlerMethod> mappings = new HashMap<>();
        mappings.put(admin, handlerMethod);
        mappings.put(user, handlerMethod);
        mappings.put(publicEndpoint, handlerMethod);

        lenient().when(requestMappingHandlerMapping.getHandlerMethods()).thenReturn(mappings);
        lenient().when(appointmentRepository.count()).thenReturn(20L);
        lenient().when(appointmentRepository.countValidRecords()).thenReturn(19L);

        sut.initialize();
    }

    @Test
    @DisplayName("recordEndpointResponseTime calcula promedio y cumplimiento por endpoint")
    void shouldComputePerformanceMetrics() {
        sut.recordEndpointResponseTime("/api/auth/login", 300);
        sut.recordEndpointResponseTime("/api/auth/login", 7000);

        List<PerformanceMetricsDTO> result = sut.getPerformanceMetrics();
        PerformanceMetricsDTO login = result.stream()
                .filter(r -> "/api/auth/login".equals(r.endpoint()))
                .findFirst()
                .orElseThrow();

        assertThat(login.totalRequests()).isEqualTo(2L);
        assertThat(login.avgResponseTimeMs()).isEqualTo(3650L);
        assertThat(login.requestsUnderThreshold()).isEqualTo(1L);
        assertThat(login.compliancePercent()).isEqualTo(50.0);
    }

    @Test
    @DisplayName("recordEndpointResponseTime ignora endpoints no trackeados")
    void shouldIgnoreUnknownEndpoint() {
        sut.recordEndpointResponseTime("/api/other", 200);

        List<PerformanceMetricsDTO> result = sut.getPerformanceMetrics();
        assertThat(result).isNotEmpty();
        assertThat(result).allMatch(r -> r.totalRequests() == 0L);
    }

    @Test
    @DisplayName("recordUnauthorizedAttemptWithoutToken solo cuenta rutas protegidas")
    void shouldCountUnauthorizedOnlyForProtectedEndpoints() {
        sut.recordUnauthorizedAttemptWithoutToken("/api/public/ping");
        sut.recordUnauthorizedAttemptWithoutToken("/api/v1/user/vehicles");

        SecurityMetricsDTO security = sut.getSecurityMetrics();
        assertThat(security.unauthorizedAttempts401()).isEqualTo(1L);
    }

    @Test
    @DisplayName("recordForbiddenAttempt solo cuenta rutas admin")
    void shouldCountForbiddenOnlyForAdminEndpoint() {
        sut.recordForbiddenAttempt("/api/v1/user/appointments");
        sut.recordForbiddenAttempt("/api/v1/admin/users");

        SecurityMetricsDTO security = sut.getSecurityMetrics();
        assertThat(security.forbiddenAttempts403()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getMaintainabilityMetrics retorna conteos y gate esperado")
    void shouldReturnMaintainabilityMetrics() {
        MaintainabilityMetricsDTO dto = sut.getMaintainabilityMetrics();

        assertThat(dto.totalControllers()).isEqualTo(1);
        assertThat(dto.totalServices()).isEqualTo(1);
        assertThat(dto.totalRepositories()).isEqualTo(1);
        assertThat(dto.standardizedErrorHandlingEnabled()).isTrue();
        assertThat(dto.jacocoCoverageGatePercent()).isEqualTo(60);
    }

    @Test
    @DisplayName("getSecurityMetrics refleja endpoints protegidos y compliance")
    void shouldReturnSecurityMetrics() {
        SecurityMetricsDTO dto = sut.getSecurityMetrics();

        assertThat(dto.totalProtectedEndpoints()).isEqualTo(2);
        assertThat(dto.endpointsWithAuthEnforced()).isEqualTo(2);
        assertThat(dto.accessControlCompliancePercent()).isEqualTo(100.0);
    }

    @Test
    @DisplayName("getAppointmentsMetrics calcula compliance e integridad")
    void shouldReturnAppointmentsMetrics() {
        sut.recordAppointmentCreationAttempt();
        sut.recordAppointmentCreationAttempt();
        sut.recordAppointmentCreationSuccess();
        sut.recordAppointmentCreationRejected();

        AppointmentsMetricsDTO dto = sut.getAppointmentsMetrics();

        assertThat(dto.totalCreationAttempts()).isEqualTo(2L);
        assertThat(dto.successfulAppointments()).isEqualTo(1L);
        assertThat(dto.rejectedByBusinessRules()).isEqualTo(1L);
        assertThat(dto.businessRuleCompliancePercent()).isEqualTo(100.0);
        assertThat(dto.totalAppointmentsInDB()).isEqualTo(20L);
        assertThat(dto.validRecordsInDB()).isEqualTo(19L);
        assertThat(dto.dataIntegrityPercent()).isEqualTo(95.0);
    }

    @Test
    @DisplayName("getSummaryMetrics consolida todos los bloques")
    void shouldReturnSummaryMetrics() {
        MetricsSummaryDTO summary = sut.getSummaryMetrics();

        assertThat(summary.performance()).hasSize(3);
        assertThat(summary.security()).isNotNull();
        assertThat(summary.maintainability()).isNotNull();
        assertThat(summary.appointments()).isNotNull();
    }

    static class DummyController {
        public void dummy() {
            // Método marcador para construir HandlerMethod en pruebas.
        }
    }
}

