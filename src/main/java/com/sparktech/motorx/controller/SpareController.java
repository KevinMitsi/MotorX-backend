package com.sparktech.motorx.controller;

import com.sparktech.motorx.Services.ISpareService;
import com.sparktech.motorx.dto.error.ResponseErrorDTO;
import com.sparktech.motorx.dto.inventory.*;
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
@RequestMapping("/api/v1/spares")
@RequiredArgsConstructor
@Tag(name = "Inventario - Repuestos", description = "CRUD de repuestos")
@SecurityRequirement(name = "bearerAuth")
public class SpareController {

    private final ISpareService spareService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_WORKER')")
    @Operation(summary = "Crear repuesto")
    @ApiResponses( value= {
            @ApiResponse(responseCode = "201", description = "Repuesto creado"),
            @ApiResponse(responseCode = "400", description = "Solicitud invalida", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "500", description = "Error interno", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class)))
    })
    public ResponseEntity<@NotNull SpareResponseDTO> create(@Valid @RequestBody CreateSpareDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(spareService.createSpare(dto));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_WORKER','RECEPTIONIST')")
    @Operation(summary = "Listar o buscar repuestos")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Listado consultado exitosamente"),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class)))
    })
    public ResponseEntity<@NotNull List<SpareResponseDTO>> getAll(
            @Parameter(description = "Filtro opcional por nombre (coincidencia parcial)")
            @RequestParam(required = false) String name,
            @Parameter(description = "Filtro opcional por codigo SAV (coincidencia parcial)")
            @RequestParam(required = false) String savCode
    ) {
        if (name == null && savCode == null) {
            return ResponseEntity.ok(spareService.getAllSpares());
        }
        return ResponseEntity.ok(spareService.searchSpares(name, savCode));
    }

    @GetMapping("/below-threshold")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_WORKER','RECEPTIONIST')")
    @Operation(summary = "Listar repuestos bajo umbral de stock")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Listado consultado exitosamente"),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class)))
    })
    public ResponseEntity<@NotNull List<SpareResponseDTO>> getBelowThreshold() {
        return ResponseEntity.ok(spareService.getSparesBelowThreshold());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_WORKER','RECEPTIONIST')")
    @Operation(summary = "Consultar repuesto por ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Repuesto encontrado"),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "404", description = "Repuesto no encontrado", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class)))
    })
    public ResponseEntity<@NotNull SpareResponseDTO> getById(
            @Parameter(description = "ID del repuesto") @PathVariable Long id
    ) {
        return ResponseEntity.ok(spareService.getSpareById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_WORKER')")
    @Operation(summary = "Actualizar repuesto")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Repuesto actualizado"),
            @ApiResponse(responseCode = "400", description = "Solicitud invalida", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "404", description = "Repuesto no encontrado", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "409", description = "Codigo de repuesto duplicado", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class)))
    })
    public ResponseEntity<@NotNull SpareResponseDTO> update(
            @Parameter(description = "ID del repuesto") @PathVariable Long id,
            @Valid @RequestBody UpdateSpareDTO dto
    ) {
        return ResponseEntity.ok(spareService.updateSpare(id, dto));
    }

    @PatchMapping("/{id}/purchase-price")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE_WORKER')")
    @Operation(summary = "Actualizar precio de compra")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Precio actualizado"),
            @ApiResponse(responseCode = "400", description = "Solicitud invalida", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "404", description = "Repuesto no encontrado", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class)))
    })
    public ResponseEntity<@NotNull SpareResponseDTO> updatePrice(
            @Parameter(description = "ID del repuesto") @PathVariable Long id,
            @Valid @RequestBody UpdateSparePurchasePriceDTO dto
    ) {
        return ResponseEntity.ok(spareService.updatePurchasePrice(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Eliminar repuesto")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Repuesto eliminado"),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "404", description = "Repuesto no encontrado", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class)))
    })
    public ResponseEntity<@NotNull Void> delete(
            @Parameter(description = "ID del repuesto") @PathVariable Long id
    ) {
        spareService.deleteSpare(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/notify-restock")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Notificar surtido a usuarios de bodega")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Notificaciones enviadas"),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "404", description = "Repuesto no encontrado", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class)))
    })
    public ResponseEntity<@NotNull Long> notifyRestock(
            @Parameter(description = "ID del repuesto") @PathVariable Long id
    ) {
        return ResponseEntity.ok(spareService.notifyWarehouseWorkersToRestock(id));
    }
}

