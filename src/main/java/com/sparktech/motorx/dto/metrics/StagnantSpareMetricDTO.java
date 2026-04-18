package com.sparktech.motorx.dto.metrics;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(name = "StagnantSpareMetricDTO", description = "Repuesto con baja rotacion o sin ventas recientes")
public record StagnantSpareMetricDTO(
        Long spareId,
        String spareName,
        String savCode,
        Integer currentStock,
        LocalDateTime lastSaleDate,
        Long daysWithoutSales,
        boolean neverSold
) {
}

