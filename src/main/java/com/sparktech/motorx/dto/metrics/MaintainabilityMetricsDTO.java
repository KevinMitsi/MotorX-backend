package com.sparktech.motorx.dto.metrics;

public record MaintainabilityMetricsDTO(
        int totalControllers,
        int totalServices,
        int totalRepositories,
        boolean standardizedErrorHandlingEnabled,
        int jacocoCoverageGatePercent
) {
}

