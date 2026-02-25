package com.sparktech.motorx.controller;

import com.sparktech.motorx.dto.appointment.CreateAppointmentRequestDTO;
import com.sparktech.motorx.dto.appointment.AppointmentResponseDTO;
import com.sparktech.motorx.dto.appointment.AvailableSlotsResponseDTO;
import com.sparktech.motorx.dto.appointment.LicensePlateRestrictionResponseDTO;
import com.sparktech.motorx.dto.appointment.ReworkRedirectResponseDTO;
import com.sparktech.motorx.entity.AppointmentType;
import com.sparktech.motorx.Services.IUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/user/appointments")
@RequiredArgsConstructor
@Tag(name = "User - Citas", description = "Endpoints para que el cliente gestione sus citas")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final IUserService userService;

    // ---------------------------------------------------------------
    // CONSULTAS PREVIAS AL AGENDAMIENTO
    // ---------------------------------------------------------------

    @GetMapping("/available-slots")
    @Operation(
            summary = "Consultar horarios disponibles",
            description = "Devuelve los slots disponibles para una fecha y tipo de cita. " +
                    "Un slot aparece disponible si al menos un técnico tiene ese horario libre."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Slots consultados exitosamente"),
            @ApiResponse(responseCode = "400", description = "Fecha o tipo de cita inválidos"),
            @ApiResponse(responseCode = "401", description = "No autenticado")
    })
    public ResponseEntity<@NotNull AvailableSlotsResponseDTO> getAvailableSlots(
            @Parameter(description = "Fecha deseada para la cita (yyyy-MM-dd)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Parameter(description = "Tipo de cita")
            @RequestParam AppointmentType type
    ) {
        return ResponseEntity.ok(userService.getAvailableSlots(date, type));
    }



    @GetMapping("/check-plate-restriction")
    @Operation(
            summary = "Verificar pico y placa",
            description = "Verifica si el vehículo tiene restricción de movilidad (pico y placa) " +
                    "en la fecha indicada. Se recomienda llamar ANTES de mostrar los slots."
    )
    @ApiResponses(value ={
            @ApiResponse(responseCode = "200", description = "Sin restricción de movilidad"),
            @ApiResponse(responseCode = "409", description = "Vehículo con pico y placa ese día")
    }
    )
    public ResponseEntity<@NotNull LicensePlateRestrictionResponseDTO> checkPlateRestriction(
            @RequestParam Long vehicleId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        LicensePlateRestrictionResponseDTO restriction =
                userService.checkLicensePlateRestriction(vehicleId, date);

        return ResponseEntity.status(HttpStatus.CONFLICT).body(restriction);

    }

    @GetMapping("/rework-info")
    @Operation(
            summary = "Información para agendar un reproceso",
            description = "Devuelve los datos de contacto del taller para agendar un reproceso. " +
                    "Los reprocesos no pueden agendarse en línea."
    )
    public ResponseEntity<@NotNull ReworkRedirectResponseDTO> getReworkInfo() {
        return ResponseEntity.ok(userService.getReworkRedirectInfo());
    }

    // ---------------------------------------------------------------
    // AGENDAMIENTO
    // ---------------------------------------------------------------

    @PostMapping
    @Operation(
            summary = "Agendar una cita",
            description = "Crea una nueva cita para el usuario autenticado. " +
                    "El sistema valida pico y placa, marca del vehículo, horario y disponibilidad " +
                    "de técnicos. El técnico se asigna automáticamente."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Cita agendada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o horario no permitido"),
            @ApiResponse(responseCode = "409", description = "Sin técnicos disponibles o pico y placa")
    })
    public ResponseEntity<@NotNull AppointmentResponseDTO> scheduleAppointment(
            @Valid @RequestBody CreateAppointmentRequestDTO request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userService.scheduleAppointment(request));
    }

    // ---------------------------------------------------------------
    // GESTIÓN DE CITAS PROPIAS
    // ---------------------------------------------------------------

    @GetMapping("/my")
    @Operation(summary = "Historial de mis citas", description = "Lista todas las citas del cliente autenticado.")
    public ResponseEntity<@NotNull List<AppointmentResponseDTO>> getMyAppointments() {
        return ResponseEntity.ok(userService.getMyAppointmentHistory());
    }

    @GetMapping("/my/{appointmentId}")
    @Operation(summary = "Detalle de una cita", description = "Devuelve el detalle de una cita específica del cliente.")
    public ResponseEntity<@NotNull AppointmentResponseDTO> getMyAppointmentById(
            @PathVariable Long appointmentId
    ) {
        return ResponseEntity.ok(userService.getMyAppointmentById(appointmentId));
    }

    @GetMapping("/my/vehicle/{vehicleId}")
    @Operation(summary = "Citas de un vehículo", description = "Lista el historial de citas de un vehículo del cliente.")
    public ResponseEntity<@NotNull List<AppointmentResponseDTO>> getMyVehicleAppointments(
            @PathVariable Long vehicleId
    ) {
        return ResponseEntity.ok(userService.getMyVehicleAppointments(vehicleId));
    }

    @DeleteMapping("/my/{appointmentId}")
    @Operation(summary = "Cancelar mi cita", description = "Cancela una cita del cliente autenticado.")
    @ApiResponses(value= {
            @ApiResponse(responseCode = "200", description = "Cita cancelada exitosamente"),
            @ApiResponse(responseCode = "403", description = "La cita no pertenece al usuario")
    })
    public ResponseEntity<@NotNull AppointmentResponseDTO> cancelMyAppointment(
            @PathVariable Long appointmentId
    ) {
        return ResponseEntity.ok(userService.cancelMyAppointment(appointmentId));
    }
}