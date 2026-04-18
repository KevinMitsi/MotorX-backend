package com.sparktech.motorx.Services.impl;

import com.sparktech.motorx.Services.IMetricsService;
import com.sparktech.motorx.controller.AdminMetricsController;
import com.sparktech.motorx.dto.metrics.*;
import com.sparktech.motorx.entity.SaleTransactionItem;
import com.sparktech.motorx.entity.Spare;
import com.sparktech.motorx.repository.JpaAppointmentRepository;
import com.sparktech.motorx.repository.JpaSaleTransactionRepository;
import com.sparktech.motorx.repository.JpaSpareRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Pageable;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.math.BigDecimal;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MetricsServiceImpl - Unit Tests")
class MetricsServiceImplTest {

    @Mock
    private JpaAppointmentRepository appointmentRepository;

    @Mock
    private JpaSaleTransactionRepository saleTransactionRepository;

    @Mock
    private JpaSpareRepository spareRepository;

    @Mock
    private RequestMappingHandlerMapping requestMappingHandlerMapping;

    @Mock
    private ApplicationContext applicationContext;

    private MetricsServiceImpl sut;

    @BeforeEach
    void setUp() throws Exception {
        sut = new MetricsServiceImpl(appointmentRepository, saleTransactionRepository, spareRepository, requestMappingHandlerMapping);

        Map<String, Object> beans = new HashMap<>();
        beans.put("adminMetricsController", new AdminMetricsController(mock(IMetricsService.class)));
        beans.put("appointmentRepository", appointmentRepository);
        beans.put("metricsService", sut);

        when(applicationContext.getBeansOfType(Object.class)).thenReturn(beans);
        sut.setApplicationContext(applicationContext);

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

    @Test
    @DisplayName("getTopSellingSpares retorna ranking limitado")
    void shouldGetTopSellingSpares() {
        List<Object[]> rows = new java.util.ArrayList<>();
        rows.add(new Object[]{1L, "Filtro", "SAV-1", 14L});
        when(saleTransactionRepository.findTopSellingSpares(any(Pageable.class))).thenReturn(rows);

        List<TopSellingSpareMetricDTO> result = sut.getTopSellingSpares(5);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().spareId()).isEqualTo(1L);
        assertThat(result.getFirst().unitsSold()).isEqualTo(14L);
    }

    @Test
    @DisplayName("getTopSellingSpares valida limit mayor a cero")
    void shouldValidateTopSellingLimit() {
        assertThatThrownBy(() -> sut.getTopSellingSpares(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("limit");
    }

    @Test
    @DisplayName("getInventoryProfitMetrics calcula venta bruta y ganancia estimada")
    void shouldCalculateInventoryProfitMetrics() {
        Spare spare = new Spare();
        spare.setId(1L);
        spare.setIsOil(false);

        SaleTransactionItem item = new SaleTransactionItem();
        item.setSpare(spare);
        item.setQuantity(2);
        item.setSalePriceAtMoment(new BigDecimal("135.00"));

        when(saleTransactionRepository.findSoldItemsBetween(
                LocalDateTime.parse("2026-01-01T00:00:00"),
                LocalDateTime.parse("2026-02-01T00:00:00")))
                .thenReturn(List.of(item));

        InventoryProfitMetricsDTO result = sut.getInventoryProfitMetrics(
                LocalDate.parse("2026-01-01"),
                LocalDate.parse("2026-01-31")
        );

        assertThat(result.totalUnitsSold()).isEqualTo(2L);
        assertThat(result.grossSalesAmount()).isEqualByComparingTo("270.00");
        assertThat(result.estimatedProfitAmount()).isEqualByComparingTo("70.00");
    }

    @Test
    @DisplayName("getInventoryProfitMetrics falla cuando el rango es invalido")
    void shouldValidateProfitDateRange() {
        assertThatThrownBy(() -> sut.getInventoryProfitMetrics(
                LocalDate.parse("2026-02-01"),
                LocalDate.parse("2026-01-01")
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("inicio");
    }

    @Test
    @DisplayName("getStagnantSpares incluye nunca vendidos y antiguos")
    void shouldGetStagnantSpares() {
        Spare neverSold = new Spare();
        neverSold.setId(10L);
        neverSold.setName("Empaque");
        neverSold.setSavCode("SAV-10");
        neverSold.setQuantity(3);

        Spare soldLongAgo = new Spare();
        soldLongAgo.setId(11L);
        soldLongAgo.setName("Bujia");
        soldLongAgo.setSavCode("SAV-11");
        soldLongAgo.setQuantity(9);

        when(spareRepository.findAll()).thenReturn(List.of(neverSold, soldLongAgo));
        List<Object[]> rows = new java.util.ArrayList<>();
        rows.add(new Object[]{11L, LocalDateTime.now().minusDays(120)});
        when(saleTransactionRepository.findLastSaleDatePerSpare()).thenReturn(rows);

        List<StagnantSpareMetricDTO> result = sut.getStagnantSpares(60);

        assertThat(result).hasSize(2);
        assertThat(result).anyMatch(StagnantSpareMetricDTO::neverSold);
        assertThat(result).anyMatch(dto -> !dto.neverSold() && dto.spareId().equals(11L));
    }

    @Test
    @DisplayName("getInventoryThresholdMetrics calcula porcentaje bajo umbral")
    void shouldGetInventoryThresholdMetrics() {
        Spare below = new Spare();
        below.setStockThreshold(5);
        below.setQuantity(3);

        Spare ok = new Spare();
        ok.setStockThreshold(5);
        ok.setQuantity(7);

        Spare noThreshold = new Spare();
        noThreshold.setStockThreshold(0);
        noThreshold.setQuantity(0);

        when(spareRepository.findAll()).thenReturn(List.of(below, ok, noThreshold));

        InventoryThresholdMetricsDTO result = sut.getInventoryThresholdMetrics();

        assertThat(result.sparesWithThreshold()).isEqualTo(2);
        assertThat(result.sparesBelowThreshold()).isEqualTo(1);
        assertThat(result.belowThresholdPercent()).isEqualTo(50.0);
    }

    static class DummyController {
        public void dummy() {
            // Method marcador para construir HandlerMethod en pruebas.
        }
    }
}