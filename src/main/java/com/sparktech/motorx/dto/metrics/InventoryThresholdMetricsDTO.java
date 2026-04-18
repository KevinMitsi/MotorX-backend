package com.sparktech.motorx.dto.metrics;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "InventoryThresholdMetricsDTO", description = "Porcentaje de repuestos que estan bajo su umbral configurado")
public record InventoryThresholdMetricsDTO(
        long sparesBelowThreshold,
        long sparesWithThreshold,
        double belowThresholdPercent
) {
}

