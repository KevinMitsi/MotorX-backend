package com.sparktech.motorx.dto.metrics;

public record PerformanceMetricsDTO(
        String endpoint,
        long avgResponseTimeMs,
        long totalRequests,
        long requestsUnderThreshold,
        double compliancePercent
) {
}

