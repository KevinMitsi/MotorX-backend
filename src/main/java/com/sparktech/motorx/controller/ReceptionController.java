package com.sparktech.motorx.controller;

import com.sparktech.motorx.Services.IReceptionService;
import com.sparktech.motorx.dto.appointment.AppointmentResponseDTO;
import com.sparktech.motorx.dto.error.ResponseErrorDTO;
import com.sparktech.motorx.dto.reception.ConfirmReceptionDTO;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reception")
@RequiredArgsConstructor
@Tag(name = "Recepcion", description = "Flujo de recepcion de motos")
@SecurityRequirement(name = "bearerAuth")
public class ReceptionController {

    private final IReceptionService receptionService;

    @PostMapping("/initiate/{appointmentId}")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    @Operation(summary = "Iniciar recepcion y enviar codigo")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Recepcion iniciada correctamente"),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "404", description = "Cita no encontrada", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "422", description = "Cita no elegible para recepcion", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class)))
    })
    public ResponseEntity<@NotNull AppointmentResponseDTO> initiate(
            @Parameter(description = "ID de la cita") @PathVariable Long appointmentId
    ) {
        return ResponseEntity.ok(receptionService.initiateReception(appointmentId));
    }

    @PostMapping("/confirm")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    @Operation(summary = "Confirmar recepcion con placa y codigo")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Recepcion confirmada correctamente"),
            @ApiResponse(responseCode = "400", description = "Codigo invalido o body incorrecto", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "422", description = "Cita no elegible para recepcion", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class)))
    })
    public ResponseEntity<@NotNull AppointmentResponseDTO> confirm(@Valid @RequestBody ConfirmReceptionDTO dto) {
        return ResponseEntity.ok(receptionService.confirmReception(dto));
    }
}

