package com.sparktech.motorx.controller;

import com.sparktech.motorx.Services.IInventoryTransactionService;
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
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
@Tag(name = "Inventario - Transacciones", description = "Entradas y salidas de inventario")
@SecurityRequirement(name = "bearerAuth")
public class InventoryTransactionController {

    private final IInventoryTransactionService inventoryTransactionService;

    @PostMapping("/purchases")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    @Operation(summary = "Registrar compra")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Compra registrada"),
            @ApiResponse(responseCode = "400", description = "Solicitud invalida", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "404", description = "Repuesto no encontrado", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class)))
    })
    public ResponseEntity<@NotNull PurchaseTransactionResponseDTO> registerPurchase(@Valid @RequestBody CreatePurchaseTransactionDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(inventoryTransactionService.registerPurchase(dto));
    }

    @GetMapping("/purchases")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    @Operation(summary = "Listar compras")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Listado consultado"),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class)))
    })
    public ResponseEntity<@NotNull List<PurchaseTransactionResponseDTO>> getPurchases() {
        return ResponseEntity.ok(inventoryTransactionService.getPurchases());
    }

    @GetMapping("/purchases/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    @Operation(summary = "Detalle de compra")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transaccion encontrada"),
            @ApiResponse(responseCode = "400", description = "Argumento invalido", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class)))
    })
    public ResponseEntity<@NotNull PurchaseTransactionResponseDTO> getPurchase(
            @Parameter(description = "ID de la compra") @PathVariable Long id
    ) {
        return ResponseEntity.ok(inventoryTransactionService.getPurchaseById(id));
    }

    @PostMapping("/sales")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    @Operation(summary = "Registrar venta")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Venta registrada"),
            @ApiResponse(responseCode = "400", description = "Solicitud invalida", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "422", description = "Stock insuficiente o cita no elegible", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class)))
    })
    public ResponseEntity<@NotNull SaleTransactionResponseDTO> registerSale(@Valid @RequestBody CreateSaleTransactionDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(inventoryTransactionService.registerSale(dto));
    }

    @GetMapping("/sales")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    @Operation(summary = "Listar ventas")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Listado consultado"),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class)))
    })
    public ResponseEntity<@NotNull List<SaleTransactionResponseDTO>> getSales() {
        return ResponseEntity.ok(inventoryTransactionService.getSales());
    }

    @GetMapping("/sales/today")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    @Operation(summary = "Resumen de ventas del dia")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Resumen calculado"),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class)))
    })
    public ResponseEntity<@NotNull DailySalesSummaryDTO> getTodaySummary() {
        return ResponseEntity.ok(inventoryTransactionService.getTodaySalesSummary());
    }

    @GetMapping("/sales/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    @Operation(summary = "Detalle de venta")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transaccion encontrada"),
            @ApiResponse(responseCode = "400", description = "Argumento invalido", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos", content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class)))
    })
    public ResponseEntity<@NotNull SaleTransactionResponseDTO> getSale(
            @Parameter(description = "ID de la venta") @PathVariable Long id
    ) {
        return ResponseEntity.ok(inventoryTransactionService.getSaleById(id));
    }
}

