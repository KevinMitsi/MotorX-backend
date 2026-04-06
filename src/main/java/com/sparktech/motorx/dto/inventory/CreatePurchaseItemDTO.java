package com.sparktech.motorx.dto.inventory;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Schema(name = "CreatePurchaseItemDTO", description = "Item para registrar compra de inventario")
public record CreatePurchaseItemDTO(
        @NotNull(message = "El ID del repuesto es obligatorio")
        Long spareId,
        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = 1, message = "La cantidad debe ser mayor a cero")
        Integer quantity,
        @NotNull(message = "El precio de compra es obligatorio")
        @DecimalMin(value = "0.0", inclusive = true, message = "El precio no puede ser negativo")
        BigDecimal purchasePriceWithVat
) {
}

