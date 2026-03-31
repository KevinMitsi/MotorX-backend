package com.sparktech.motorx.controller;

import com.sparktech.motorx.Services.IMetricsService;
import com.sparktech.motorx.dto.metrics.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/metrics")
@RequiredArgsConstructor
@Tag(name = "Admin - Métricas", description = "Métricas de calidad y seguridad del sistema")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminMetricsController {

    private final IMetricsService metricsService;

    @GetMapping("/performance")
    public List<PerformanceMetricsDTO> getPerformanceMetrics() {
        return metricsService.getPerformanceMetrics();
    }

    @GetMapping("/security")
    public SecurityMetricsDTO getSecurityMetrics() {
        return metricsService.getSecurityMetrics();
    }

    @GetMapping("/maintainability")
    public MaintainabilityMetricsDTO getMaintainabilityMetrics() {
        return metricsService.getMaintainabilityMetrics();
    }

    @GetMapping("/appointments")
    public AppointmentsMetricsDTO getAppointmentsMetrics() {
        return metricsService.getAppointmentsMetrics();
    }

    @GetMapping("/summary")
    public MetricsSummaryDTO getSummaryMetrics() {
        return metricsService.getSummaryMetrics();
    }
}

