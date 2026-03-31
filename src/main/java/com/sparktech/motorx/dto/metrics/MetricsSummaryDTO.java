package com.sparktech.motorx.dto.metrics;

import java.util.List;

public record MetricsSummaryDTO(
        List<PerformanceMetricsDTO> performance,
        SecurityMetricsDTO security,
        MaintainabilityMetricsDTO maintainability,
        AppointmentsMetricsDTO appointments
) {
}

