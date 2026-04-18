package com.sparktech.motorx.dto.metrics;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(name = "InventoryProfitMetricsDTO", description = "Totales de venta y ganancia estimada del inventario en un periodo")
public record InventoryProfitMetricsDTO(
        LocalDate startDate,
        LocalDate endDate,
        long totalUnitsSold,
        BigDecimal grossSalesAmount,
        BigDecimal estimatedProfitAmount
) {
}

