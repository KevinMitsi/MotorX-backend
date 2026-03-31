package com.sparktech.motorx.controller;

import com.sparktech.motorx.Services.IMetricsService;
import com.sparktech.motorx.dto.error.ResponseErrorDTO;
import com.sparktech.motorx.dto.metrics.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
    @Operation(
            summary = "Métricas de rendimiento",
            description = "Retorna métricas de tiempos de respuesta y cumplimiento de umbrales por endpoint monitoreado."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Métricas de rendimiento obtenidas exitosamente",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = PerformanceMetricsDTO.class)))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado por falta de permisos ADMIN",
                    content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class)))
    })
    public List<PerformanceMetricsDTO> getPerformanceMetrics() {
        return metricsService.getPerformanceMetrics();
    }

    @GetMapping("/security")
    @Operation(
            summary = "Métricas de seguridad",
            description = "Resume intentos no autorizados (401), accesos prohibidos (403) y cumplimiento de controles de acceso."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Métricas de seguridad obtenidas exitosamente",
                    content = @Content(schema = @Schema(implementation = SecurityMetricsDTO.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado por falta de permisos ADMIN",
                    content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class)))
    })
    public SecurityMetricsDTO getSecurityMetrics() {
        return metricsService.getSecurityMetrics();
    }

    @GetMapping("/maintainability")
    @Operation(
            summary = "Métricas de mantenibilidad",
            description = "Expone métricas estructurales del sistema, estandarización de errores y cobertura mínima configurada."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Métricas de mantenibilidad obtenidas exitosamente",
                    content = @Content(schema = @Schema(implementation = MaintainabilityMetricsDTO.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado por falta de permisos ADMIN",
                    content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class)))
    })
    public MaintainabilityMetricsDTO getMaintainabilityMetrics() {
        return metricsService.getMaintainabilityMetrics();
    }

    @GetMapping("/appointments")
    @Operation(
            summary = "Métricas de citas",
            description = "Muestra intentos, éxitos, rechazos por reglas de negocio e integridad de datos de citas en base de datos."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Métricas de citas obtenidas exitosamente",
                    content = @Content(schema = @Schema(implementation = AppointmentsMetricsDTO.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado por falta de permisos ADMIN",
                    content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class)))
    })
    public AppointmentsMetricsDTO getAppointmentsMetrics() {
        return metricsService.getAppointmentsMetrics();
    }

    @GetMapping("/summary")
    @Operation(
            summary = "Resumen consolidado de métricas",
            description = "Retorna un consolidado de rendimiento, seguridad, mantenibilidad y citas en una sola respuesta."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Resumen de métricas obtenido exitosamente",
                    content = @Content(schema = @Schema(implementation = MetricsSummaryDTO.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado por falta de permisos ADMIN",
                    content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class)))
    })
    public MetricsSummaryDTO getSummaryMetrics() {
        return metricsService.getSummaryMetrics();
    }
}
