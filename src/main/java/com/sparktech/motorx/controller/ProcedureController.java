package com.sparktech.motorx.controller;

import com.sparktech.motorx.Services.IProcedureService;
import com.sparktech.motorx.dto.error.ResponseErrorDTO;
import com.sparktech.motorx.dto.procedure.*;
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
@RequestMapping("/api/v1/procedures")
@RequiredArgsConstructor
@Tag(name = "Procedimientos", description = "Catalogo de procedimientos y relacion con servicios")
@SecurityRequirement(name = "bearerAuth")
public class ProcedureController {

    private final IProcedureService procedureService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear procedimiento")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Procedimiento creado"),
            @ApiResponse(responseCode = "400", description = "Solicitud invalida", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "409", description = "Nombre duplicado", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class)))
    })
    public ResponseEntity<@NotNull ProcedureResponseDTO> create(@Valid @RequestBody CreateProcedureDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(procedureService.create(dto));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','TECHNICIAN')")
    @Operation(summary = "Listar procedimientos")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Listado consultado"),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class)))
    })
    public ResponseEntity<@NotNull List<ProcedureResponseDTO>> getAll() {
        return ResponseEntity.ok(procedureService.getAll());
    }

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ADMIN','TECHNICIAN')")
    @Operation(summary = "Listar procedimientos activos")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Listado consultado"),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class)))
    })
    public ResponseEntity<@NotNull List<ProcedureResponseDTO>> getActive() {
        return ResponseEntity.ok(procedureService.getActive());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TECHNICIAN')")
    @Operation(summary = "Consultar procedimiento por ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Procedimiento encontrado"),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "404", description = "No encontrado", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class)))
    })
    public ResponseEntity<@NotNull ProcedureResponseDTO> getById(
            @Parameter(description = "ID del procedimiento") @PathVariable Long id
    ) {
        return ResponseEntity.ok(procedureService.getById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar procedimiento")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Procedimiento actualizado"),
            @ApiResponse(responseCode = "400", description = "Solicitud invalida", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "404", description = "No encontrado", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "409", description = "Nombre duplicado", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class)))
    })
    public ResponseEntity<@NotNull ProcedureResponseDTO> update(
            @Parameter(description = "ID del procedimiento") @PathVariable Long id,
            @Valid @RequestBody UpdateProcedureDTO dto
    ) {
        return ResponseEntity.ok(procedureService.update(id, dto));
    }

    @GetMapping("/service/{serviceId}")
    @PreAuthorize("hasAnyRole('ADMIN','TECHNICIAN')")
    @Operation(summary = "Listar procedimientos base de un servicio")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Listado consultado"),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "404", description = "Servicio no encontrado", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class)))
    })
    public ResponseEntity<@NotNull List<ProcedureResponseDTO>> getByService(
            @Parameter(description = "ID del servicio") @PathVariable Long serviceId
    ) {
        return ResponseEntity.ok(procedureService.getProceduresByService(serviceId));
    }

    @PutMapping("/service/{serviceId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar procedimientos base de un servicio")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Procedimientos actualizados"),
            @ApiResponse(responseCode = "400", description = "Solicitud invalida", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "404", description = "Servicio o procedimiento no encontrado", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class)))
    })
    public ResponseEntity<@NotNull List<ProcedureResponseDTO>> updateServiceProcedures(
            @Parameter(description = "ID del servicio") @PathVariable Long serviceId,
            @Valid @RequestBody UpdateServiceProceduresDTO dto
    ) {
        return ResponseEntity.ok(procedureService.updateServiceProcedures(serviceId, dto));
    }
}

