package com.sparktech.motorx.dto.inventory;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

@Schema(name = "CreateSpareDTO", description = "Datos para crear un repuesto")
public record CreateSpareDTO(
        @Schema(description = "Nombre del repuesto", example = "Filtro de aceite")
        @NotBlank(message = "El nombre es obligatorio")
        String name,

        @Schema(description = "Compatibilidad de motocicletas", example = "AKT NKD 125, Yamaha FZ")
        @NotBlank(message = "La compatibilidad es obligatoria")
        String compatibleMotorcycles,

        @Schema(description = "Codigo SAV", example = "SAV-001")
        @NotBlank(message = "El codigo SAV es obligatorio")
        String savCode,

        @Schema(description = "Codigo interno de repuesto", example = "REP-001")
        @NotBlank(message = "El codigo de repuesto es obligatorio")
        String spareCode,

        @Schema(description = "Precio de compra con IVA", example = "120000")
        @NotNull(message = "El precio de compra es obligatorio")
        @DecimalMin(value = "0.0", inclusive = true, message = "El precio no puede ser negativo")
        BigDecimal purchasePriceWithVat,

        @Schema(description = "Indica si el repuesto es aceite", example = "false")
        @NotNull(message = "El tipo de repuesto es obligatorio")
        Boolean isOil,

        @Schema(description = "Proveedor", example = "Motopartes SAS")
        @NotBlank(message = "El proveedor es obligatorio")
        String supplier,

        @Schema(description = "Cantidad disponible", example = "10")
        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = 0, message = "La cantidad no puede ser negativa")
        Integer quantity,

        @Schema(description = "Umbral minimo de stock para alertas", example = "5")
        @NotNull(message = "El umbral de stock es obligatorio")
        @Min(value = 0, message = "El umbral de stock no puede ser negativo")
        Integer stockThreshold,

        @Schema(description = "Ubicacion de bodega PA-ES-NI-SL", example = "02-04-01-03")
        @NotBlank(message = "La ubicacion de bodega es obligatoria")
        @Pattern(regexp = "\\d{2}-\\d{2}-\\d{2}-\\d{2}", message = "La ubicacion debe tener formato 00-00-00-00")
        String warehouseLocation
) {
}

