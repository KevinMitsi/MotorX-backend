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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
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

    @GetMapping("/inventory/top-selling")
    @Operation(
            summary = "Repuestos mas vendidos",
            description = "Retorna ranking de repuestos por unidades vendidas."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ranking calculado exitosamente",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = TopSellingSpareMetricDTO.class)))),
            @ApiResponse(responseCode = "400", description = "Parametro limit invalido",
                    content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado por falta de permisos ADMIN",
                    content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class)))
    })
    public List<TopSellingSpareMetricDTO> getTopSellingSpares(
            @RequestParam(defaultValue = "10") int limit
    ) {
        return metricsService.getTopSellingSpares(limit);
    }

    @GetMapping("/inventory/profit")
    @Operation(
            summary = "Ganancia de inventario por periodo",
            description = "Calcula ventas brutas y ganancia estimada entre fecha de inicio y fecha fin usando margen de repuestos normales (35%) y aceites (25%)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Metricas de rentabilidad calculadas",
                    content = @Content(schema = @Schema(implementation = InventoryProfitMetricsDTO.class))),
            @ApiResponse(responseCode = "400", description = "Fechas invalidas o faltantes",
                    content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado por falta de permisos ADMIN",
                    content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class)))
    })
    public InventoryProfitMetricsDTO getInventoryProfit(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return metricsService.getInventoryProfitMetrics(startDate, endDate);
    }

    @GetMapping("/inventory/stagnant")
    @Operation(
            summary = "Repuestos estancados",
            description = "Lista repuestos sin ventas recientes (o nunca vendidos) segun una ventana de dias."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Listado de repuestos estancados",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = StagnantSpareMetricDTO.class)))),
            @ApiResponse(responseCode = "400", description = "Parametro daysWithoutSales invalido",
                    content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado por falta de permisos ADMIN",
                    content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class)))
    })
    public List<StagnantSpareMetricDTO> getStagnantSpares(
            @RequestParam(defaultValue = "60") int daysWithoutSales
    ) {
        return metricsService.getStagnantSpares(daysWithoutSales);
    }

    @GetMapping("/inventory/below-threshold-percentage")
    @Operation(
            summary = "Porcentaje de repuestos bajo umbral",
            description = "Retorna el porcentaje de repuestos con umbral configurado que estan por debajo de su stock minimo."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Porcentaje calculado exitosamente",
                    content = @Content(schema = @Schema(implementation = InventoryThresholdMetricsDTO.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado por falta de permisos ADMIN",
                    content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class)))
    })
    public InventoryThresholdMetricsDTO getInventoryThresholdMetrics() {
        return metricsService.getInventoryThresholdMetrics();
    }
}
