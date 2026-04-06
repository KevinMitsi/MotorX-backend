package com.sparktech.motorx.dto.inventory;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(name = "SpareResponseDTO", description = "Respuesta de repuesto con precio de venta calculado")
public record SpareResponseDTO(
        Long id,
        String name,
        String compatibleMotorcycles,
        String savCode,
        String spareCode,
        BigDecimal purchasePriceWithVat,
        BigDecimal salePrice,
        Boolean isOil,
        String supplier,
        Integer quantity,
        String warehouseLocation
) {
}

