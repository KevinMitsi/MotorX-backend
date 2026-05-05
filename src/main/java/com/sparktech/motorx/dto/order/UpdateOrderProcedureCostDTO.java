package com.sparktech.motorx.dto.order;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record UpdateOrderProcedureCostDTO(
        @NotNull(message = "El costo es obligatorio")
        @DecimalMin(value = "0.0", inclusive = true, message = "El costo no puede ser negativo")
        BigDecimal cost
) {
}

