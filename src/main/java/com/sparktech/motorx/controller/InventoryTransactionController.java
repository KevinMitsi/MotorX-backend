package com.sparktech.motorx.controller;

import com.sparktech.motorx.Services.IInventoryTransactionService;
import com.sparktech.motorx.dto.inventory.*;
import io.swagger.v3.oas.annotations.Operation;
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
    public ResponseEntity<@NotNull PurchaseTransactionResponseDTO> registerPurchase(@Valid @RequestBody CreatePurchaseTransactionDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(inventoryTransactionService.registerPurchase(dto));
    }

    @GetMapping("/purchases")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    @Operation(summary = "Listar compras")
    public ResponseEntity<@NotNull List<PurchaseTransactionResponseDTO>> getPurchases() {
        return ResponseEntity.ok(inventoryTransactionService.getPurchases());
    }

    @GetMapping("/purchases/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    @Operation(summary = "Detalle de compra")
    public ResponseEntity<@NotNull PurchaseTransactionResponseDTO> getPurchase(@PathVariable Long id) {
        return ResponseEntity.ok(inventoryTransactionService.getPurchaseById(id));
    }

    @PostMapping("/sales")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    @Operation(summary = "Registrar venta")
    public ResponseEntity<@NotNull SaleTransactionResponseDTO> registerSale(@Valid @RequestBody CreateSaleTransactionDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(inventoryTransactionService.registerSale(dto));
    }

    @GetMapping("/sales")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    @Operation(summary = "Listar ventas")
    public ResponseEntity<@NotNull List<SaleTransactionResponseDTO>> getSales() {
        return ResponseEntity.ok(inventoryTransactionService.getSales());
    }

    @GetMapping("/sales/today")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    @Operation(summary = "Resumen de ventas del dia")
    public ResponseEntity<@NotNull DailySalesSummaryDTO> getTodaySummary() {
        return ResponseEntity.ok(inventoryTransactionService.getTodaySalesSummary());
    }

    @GetMapping("/sales/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    @Operation(summary = "Detalle de venta")
    public ResponseEntity<@NotNull SaleTransactionResponseDTO> getSale(@PathVariable Long id) {
        return ResponseEntity.ok(inventoryTransactionService.getSaleById(id));
    }
}

