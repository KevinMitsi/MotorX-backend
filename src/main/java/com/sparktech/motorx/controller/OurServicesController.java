package com.sparktech.motorx.controller;

import com.sparktech.motorx.Services.IOurServicesService;
import com.sparktech.motorx.dto.error.ResponseErrorDTO;
import com.sparktech.motorx.dto.procedure.ProcedureResponseDTO;
import com.sparktech.motorx.dto.procedure.UpdateServiceProceduresDTO;
import com.sparktech.motorx.dto.service.CreateServiceDTO;
import com.sparktech.motorx.dto.service.ServiceResponseDTO;
import com.sparktech.motorx.dto.service.UpdateServiceDTO;
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
@RequestMapping("/api/v1/services")
@RequiredArgsConstructor
@Tag(name = "Servicios", description = "Catalogo de servicios del taller")
@SecurityRequirement(name = "bearerAuth")
public class OurServicesController {

    private final IOurServicesService servicesService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear servicio")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Servicio creado"),
            @ApiResponse(responseCode = "400", description = "Solicitud invalida", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "409", description = "Nombre duplicado", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class)))
    })
    public ResponseEntity<@NotNull ServiceResponseDTO> create(@Valid @RequestBody CreateServiceDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicesService.create(dto));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar servicios")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Listado consultado"),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class)))
    })
    public ResponseEntity<@NotNull List<ServiceResponseDTO>> getAll() {
        return ResponseEntity.ok(servicesService.getAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Consultar servicio por ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Servicio encontrado"),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "404", description = "No encontrado", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class)))
    })
    public ResponseEntity<@NotNull ServiceResponseDTO> getById(
            @Parameter(description = "ID del servicio") @PathVariable Long id
    ) {
        return ResponseEntity.ok(servicesService.getById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar servicio")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Servicio actualizado"),
            @ApiResponse(responseCode = "400", description = "Solicitud invalida", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "404", description = "No encontrado", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "409", description = "Nombre duplicado", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class)))
    })
    public ResponseEntity<@NotNull ServiceResponseDTO> update(
            @Parameter(description = "ID del servicio") @PathVariable Long id,
            @Valid @RequestBody UpdateServiceDTO dto
    ) {
        return ResponseEntity.ok(servicesService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Eliminar servicio")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Servicio eliminado"),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "404", description = "No encontrado", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class)))
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID del servicio") @PathVariable Long id
    ) {
        servicesService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/procedures")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar procedimientos base de un servicio")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Listado consultado"),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "404", description = "Servicio no encontrado", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class)))
    })
    public ResponseEntity<@NotNull List<ProcedureResponseDTO>> getBaseProcedures(
            @Parameter(description = "ID del servicio") @PathVariable Long id
    ) {
        return ResponseEntity.ok(servicesService.getBaseProcedures(id));
    }

    @PutMapping("/{id}/procedures")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar procedimientos base de un servicio")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Procedimientos actualizados"),
            @ApiResponse(responseCode = "400", description = "Solicitud invalida", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "404", description = "Servicio o procedimiento no encontrado", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class)))
    })
    public ResponseEntity<@NotNull List<ProcedureResponseDTO>> updateBaseProcedures(
            @Parameter(description = "ID del servicio") @PathVariable Long id,
            @Valid @RequestBody UpdateServiceProceduresDTO dto
    ) {
        return ResponseEntity.ok(servicesService.updateBaseProcedures(id, dto));
    }
}

