package com.sparktech.motorx.dto.metrics;

public record AppointmentsMetricsDTO(
        long totalCreationAttempts,
        long successfulAppointments,
        long rejectedByBusinessRules,
        double businessRuleCompliancePercent,
        long totalAppointmentsInDB,
        long validRecordsInDB,
        double dataIntegrityPercent
) {
}

