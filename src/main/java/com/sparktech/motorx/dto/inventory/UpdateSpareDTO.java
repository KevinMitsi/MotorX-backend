package com.sparktech.motorx.dto.inventory;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

@Schema(name = "UpdateSpareDTO", description = "Datos para actualizar un repuesto")
public record UpdateSpareDTO(
        @NotBlank(message = "El nombre es obligatorio")
        String name,
        @NotBlank(message = "La compatibilidad es obligatoria")
        String compatibleMotorcycles,
        @NotBlank(message = "El codigo SAV es obligatorio")
        String savCode,
        @NotBlank(message = "El codigo de repuesto es obligatorio")
        String spareCode,
        @NotNull(message = "El precio de compra es obligatorio")
        @DecimalMin(value = "0.0", inclusive = true, message = "El precio no puede ser negativo")
        BigDecimal purchasePriceWithVat,
        @NotNull(message = "El tipo de repuesto es obligatorio")
        Boolean isOil,
        @NotBlank(message = "El proveedor es obligatorio")
        String supplier,
        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = 0, message = "La cantidad no puede ser negativa")
        Integer quantity,
        @NotNull(message = "El umbral de stock es obligatorio")
        @Min(value = 0, message = "El umbral de stock no puede ser negativo")
        Integer stockThreshold,
        @NotBlank(message = "La ubicacion de bodega es obligatoria")
        @Pattern(regexp = "\\d{2}-\\d{2}-\\d{2}-\\d{2}", message = "La ubicacion debe tener formato 00-00-00-00")
        String warehouseLocation
) {
}

