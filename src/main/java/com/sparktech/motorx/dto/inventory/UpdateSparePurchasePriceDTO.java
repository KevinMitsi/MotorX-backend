package com.sparktech.motorx.dto.inventory;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Schema(name = "UpdateSparePurchasePriceDTO", description = "Actualiza solo el precio de compra de un repuesto")
public record UpdateSparePurchasePriceDTO(
        @Schema(description = "Nuevo precio de compra con IVA", example = "145000")
        @NotNull(message = "El precio de compra es obligatorio")
        @DecimalMin(value = "0.0", inclusive = true, message = "El precio no puede ser negativo")
        BigDecimal purchasePriceWithVat
) {
}

