package com.sparktech.motorx.controller;

import com.sparktech.motorx.Services.IOrderService;
import com.sparktech.motorx.dto.error.ResponseErrorDTO;
import com.sparktech.motorx.dto.order.AddProcedureToOrderDTO;
import com.sparktech.motorx.dto.order.AddSpareToOrderDTO;
import com.sparktech.motorx.dto.order.OrderResponseDTO;
import com.sparktech.motorx.dto.order.UpdateOrderProcedureCostDTO;
import com.sparktech.motorx.dto.order.TechnicianDailyOrderDTO;
import com.sparktech.motorx.dto.appointment.TechnicianAppointmentSummaryDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Ordenes de Servicio", description = "Flujo transaccional de ordenes por cita")
@SecurityRequirement(name = "bearerAuth")
public class OrderServiceController {

    private final IOrderService orderService;

    @PostMapping("/appointment/{appointmentId}")
    @PreAuthorize("hasRole('TECHNICIAN')")
    @Operation(summary = "Crear orden para una cita")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Orden creada"),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "404", description = "Cita no encontrada", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "422", description = "Cita no elegible", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class)))
    })
    public ResponseEntity<@NotNull OrderResponseDTO> createOrder(
            @Parameter(description = "ID de la cita") @PathVariable Long appointmentId
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createOrder(appointmentId));
    }

    @GetMapping("/appointment/{appointmentId}")
    @PreAuthorize("hasAnyRole('ADMIN','TECHNICIAN')")
    @Operation(summary = "Consultar orden por cita")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Orden encontrada"),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "404", description = "Orden no encontrada", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class)))
    })
    public ResponseEntity<@NotNull OrderResponseDTO> getByAppointment(
            @Parameter(description = "ID de la cita") @PathVariable Long appointmentId
    ) {
        return ResponseEntity.ok(orderService.getOrderByAppointment(appointmentId));
    }

    @PostMapping("/{orderId}/procedures")
    @PreAuthorize("hasRole('TECHNICIAN')")
    @Operation(summary = "Agregar procedimiento a una orden")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Procedimiento agregado"),
            @ApiResponse(responseCode = "400", description = "Solicitud invalida", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "404", description = "Orden o procedimiento no encontrado", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class)))
    })
    public ResponseEntity<@NotNull OrderResponseDTO> addProcedure(
            @Parameter(description = "ID de la orden") @PathVariable Long orderId,
            @Valid @RequestBody AddProcedureToOrderDTO dto
    ) {
        return ResponseEntity.ok(orderService.addProcedure(orderId, dto));
    }

    @PatchMapping("/{orderId}/procedures/{procedureId}")
    @PreAuthorize("hasRole('TECHNICIAN')")
    @Operation(summary = "Actualizar costo de procedimiento en una orden")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Costo actualizado"),
            @ApiResponse(responseCode = "400", description = "Solicitud invalida", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "404", description = "Orden o procedimiento no encontrado", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class)))
    })
    public ResponseEntity<@NotNull OrderResponseDTO> updateProcedureCost(
            @Parameter(description = "ID de la orden") @PathVariable Long orderId,
            @Parameter(description = "ID del procedimiento") @PathVariable Long procedureId,
            @Valid @RequestBody UpdateOrderProcedureCostDTO dto
    ) {
        return ResponseEntity.ok(orderService.updateProcedureCost(orderId, procedureId, dto));
    }

    @PostMapping("/{orderId}/spares")
    @PreAuthorize("hasRole('TECHNICIAN')")
    @Operation(summary = "Agregar repuesto a una orden")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Repuesto agregado"),
            @ApiResponse(responseCode = "400", description = "Solicitud invalida", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "404", description = "Orden o repuesto no encontrado", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "422", description = "Stock insuficiente", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class)))
    })
    public ResponseEntity<@NotNull OrderResponseDTO> addSpare(
            @Parameter(description = "ID de la orden") @PathVariable Long orderId,
            @Valid @RequestBody AddSpareToOrderDTO dto
    ) {
        return ResponseEntity.ok(orderService.addSpare(orderId, dto));
    }

    @PostMapping("/{orderId}/complete")
    @PreAuthorize("hasRole('TECHNICIAN')")
    @Operation(summary = "Completar una orden")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Orden completada"),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "404", description = "Orden no encontrada", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class)))
    })
    public ResponseEntity<@NotNull OrderResponseDTO> completeOrder(
            @Parameter(description = "ID de la orden") @PathVariable Long orderId
    ) {
        return ResponseEntity.ok(orderService.completeOrder(orderId));
    }

    @PostMapping("/{orderId}/send-service-details")
    @PreAuthorize("hasRole('TECHNICIAN')")
    @Operation(summary = "Enviar al cliente el detalle del servicio por correo")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Correo enviado"),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "404", description = "Orden no encontrada", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class)))
    })
    public ResponseEntity<@NotNull Void> sendServiceDetails(
            @Parameter(description = "ID de la orden") @PathVariable Long orderId
    ) {
        orderService.sendServiceDetails(orderId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/my/today")
    @PreAuthorize("hasRole('TECHNICIAN')")
    @Operation(summary = "Listar citas con recepcion confirmada hoy para el tecnico autenticado")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Listado de citas del dia"),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class)))
    })
    public ResponseEntity<@NotNull List<TechnicianDailyOrderDTO>> getMyTodayOrders() {
        return ResponseEntity.ok(orderService.getMyTodayOrders());
    }

    @GetMapping("/appointment/{appointmentId}/summary")
    @PreAuthorize("hasAnyRole('ADMIN','TECHNICIAN')")
    @Operation(summary = "Resumen de cita para tecnico", description = "Devuelve datos no sensibles de la cita para trabajo en orden.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Resumen encontrado"),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "404", description = "Cita no encontrada", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class)))
    })
    public ResponseEntity<@NotNull TechnicianAppointmentSummaryDTO> getAppointmentSummary(
            @Parameter(description = "ID de la cita") @PathVariable Long appointmentId
    ) {
        return ResponseEntity.ok(orderService.getAppointmentSummary(appointmentId));
    }
    @GetMapping("/my/active")
    @PreAuthorize("hasRole('TECHNICIAN')")
    @Operation(summary = "Listar todas las citas IN_PROGRESS asignadas al tecnico autenticado (sin filtro de fecha)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Listado de citas activas"),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class)))
    })
    public ResponseEntity<@NotNull List<TechnicianDailyOrderDTO>> getMyActiveOrders() {
        return ResponseEntity.ok(orderService.getMyActiveOrders());
    }
}
