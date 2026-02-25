package com.sparktech.motorx.controller;

import com.sparktech.motorx.dto.appointment.CancelAppointmentRequestDTO;
import com.sparktech.motorx.dto.appointment.CreateUnplannedAppointmentRequestDTO;
import com.sparktech.motorx.dto.appointment.UpdateAppointmentTechnicianRequestDTO;
import com.sparktech.motorx.dto.appointment.AppointmentResponseDTO;
import com.sparktech.motorx.dto.appointment.AvailableSlotsResponseDTO;
import com.sparktech.motorx.dto.error.ResponseErrorDTO;
import com.sparktech.motorx.entity.AppointmentType;
import com.sparktech.motorx.Services.IAdminService;
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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/appointments")
@RequiredArgsConstructor
@Tag(name = "Admin - Citas", description = "Endpoints administrativos para la gestión completa de citas")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final IAdminService adminService;

    // ---------------------------------------------------------------
    // VISIBILIDAD DE LA AGENDA
    // ---------------------------------------------------------------

    @GetMapping("/agenda")
    @Operation(
            summary = "Agenda del día",
            description = "Lista todas las citas de una fecha específica, ordenadas por hora."
    )
    public ResponseEntity<@NotNull List<AppointmentResponseDTO>> getDailyAgenda(
            @Parameter(description = "Fecha a consultar (yyyy-MM-dd)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ResponseEntity.ok(adminService.getDailyAgenda(date));
    }

    @GetMapping("/calendar")
    @Operation(
            summary = "Vista de calendario",
            description = "Lista todas las citas en un rango de fechas para la vista de calendario."
    )
    public ResponseEntity<@NotNull List<AppointmentResponseDTO>> getCalendarView(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
    ) {
        return ResponseEntity.ok(adminService.getCalendarView(start, end));
    }

    @GetMapping("/available-slots")
    @Operation(
            summary = "Consultar disponibilidad",
            description = "Consulta los slots disponibles para cualquier fecha y tipo de cita."
    )
    public ResponseEntity<@NotNull AvailableSlotsResponseDTO> getAvailableSlots(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam AppointmentType type
    ) {
        return ResponseEntity.ok(adminService.getAvailableSlots(date, type));
    }

    // ---------------------------------------------------------------
    // OPERACIONES EXCLUSIVAS DEL ADMIN
    // ---------------------------------------------------------------

    @PostMapping("/unplanned")
    @Operation(
            summary = "Registrar cita no planeada",
            description = "Permite al administrador registrar una cita fuera de los horarios de recepción " +
                    "estándar, en espacios donde no hubo cita previa. " +
                    "El técnico puede asignarse manualmente o automáticamente."
    )
    @ApiResponses(value= {
            @ApiResponse(responseCode = "201", description = "Cita no planeada registrada"),
            @ApiResponse(responseCode = "404", description = "Vehículo o técnico no encontrado",
                    content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "409", description = "Técnico no disponible o pico y placa",
                    content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos",
                    content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class)))
    })
    public ResponseEntity<@NotNull AppointmentResponseDTO> registerUnplannedAppointment(
            @Valid @RequestBody CreateUnplannedAppointmentRequestDTO request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminService.registerUnplannedAppointment(request));
    }

    @PatchMapping("/{appointmentId}/cancel")
    @Operation(
            summary = "Cancelar una cita",
            description = "Cancela cualquier cita. El admin puede elegir si enviar notificación al cliente."
    )
    public ResponseEntity<@NotNull AppointmentResponseDTO> cancelAppointment(
            @PathVariable Long appointmentId,
            @Valid @RequestBody CancelAppointmentRequestDTO request
    ) {
        return ResponseEntity.ok(adminService.cancelAppointment(appointmentId, request));
    }

    @PatchMapping("/{appointmentId}/technician")
    @Operation(
            summary = "Cambiar técnico asignado",
            description = "Cambia el técnico de una cita sin modificar el horario. " +
                    "El sistema valida que el nuevo técnico tenga ese slot disponible. " +
                    "El admin puede elegir si notificar al cliente."
    )
    @ApiResponses(value ={
            @ApiResponse(responseCode = "200", description = "Técnico actualizado exitosamente"),

            @ApiResponse(responseCode = "404", description = "Cita o técnico no encontrado",
                    content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "409", description = "El nuevo técnico tiene ese horario ocupado",
                    content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class)))
    })
    public ResponseEntity<@NotNull AppointmentResponseDTO> changeTechnician(
            @PathVariable Long appointmentId,
            @Valid @RequestBody UpdateAppointmentTechnicianRequestDTO request
    ) {
        return ResponseEntity.ok(adminService.changeTechnician(appointmentId, request));
    }

    // ---------------------------------------------------------------
    // CONSULTAS ADMINISTRATIVAS
    // ---------------------------------------------------------------

    @GetMapping("/{appointmentId}")
    @Operation(summary = "Detalle de una cita", description = "Devuelve el detalle completo de cualquier cita.")
    public ResponseEntity<@NotNull AppointmentResponseDTO> getAppointmentById(
            @PathVariable Long appointmentId
    ) {
        return ResponseEntity.ok(adminService.getAppointmentById(appointmentId));
    }

    @GetMapping("/client/{clientId}")
    @Operation(summary = "Historial de citas de un cliente")
    public ResponseEntity<@NotNull List<AppointmentResponseDTO>> getClientHistory(
            @PathVariable Long clientId
    ) {
        return ResponseEntity.ok(adminService.getClientAppointmentHistory(clientId));
    }

    @GetMapping("/vehicle/{vehicleId}")
    @Operation(summary = "Historial de citas de un vehículo")
    public ResponseEntity<@NotNull List<AppointmentResponseDTO>> getVehicleHistory(
            @PathVariable Long vehicleId
    ) {
        return ResponseEntity.ok(adminService.getVehicleAppointmentHistory(vehicleId));
    }
}