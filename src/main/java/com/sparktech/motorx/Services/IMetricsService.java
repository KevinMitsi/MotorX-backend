package com.sparktech.motorx.Services;

import com.sparktech.motorx.dto.metrics.*;

import java.time.LocalDate;
import java.util.List;

public interface IMetricsService {

    void recordEndpointResponseTime(String endpoint, long responseTimeMs);

    void recordUnauthorizedAttemptWithoutToken(String endpoint);

    void recordForbiddenAttempt(String endpoint);

    void recordAppointmentCreationAttempt();

    void recordAppointmentCreationSuccess();

    void recordAppointmentCreationRejected();

    List<PerformanceMetricsDTO> getPerformanceMetrics();

    SecurityMetricsDTO getSecurityMetrics();

    MaintainabilityMetricsDTO getMaintainabilityMetrics();

    AppointmentsMetricsDTO getAppointmentsMetrics();

    MetricsSummaryDTO getSummaryMetrics();

    List<TopSellingSpareMetricDTO> getTopSellingSpares(int limit);

    InventoryProfitMetricsDTO getInventoryProfitMetrics(LocalDate startDate, LocalDate endDate);

    List<StagnantSpareMetricDTO> getStagnantSpares(int daysWithoutSales);

    InventoryThresholdMetricsDTO getInventoryThresholdMetrics();
}

