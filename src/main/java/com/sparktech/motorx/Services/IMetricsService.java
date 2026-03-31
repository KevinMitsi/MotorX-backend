package com.sparktech.motorx.Services;

import com.sparktech.motorx.dto.metrics.*;

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
}

