package com.sparktech.motorx.dto.metrics;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "TopSellingSpareMetricDTO", description = "Repuesto con mayor rotacion de venta")
public record TopSellingSpareMetricDTO(
        Long spareId,
        String spareName,
        String savCode,
        long unitsSold
) {
}

