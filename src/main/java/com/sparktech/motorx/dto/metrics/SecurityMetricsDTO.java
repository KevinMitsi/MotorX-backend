package com.sparktech.motorx.dto.metrics;

public record SecurityMetricsDTO(
        long unauthorizedAttempts401,
        long forbiddenAttempts403,
        int totalProtectedEndpoints,
        int endpointsWithAuthEnforced,
        double accessControlCompliancePercent
) {
}

