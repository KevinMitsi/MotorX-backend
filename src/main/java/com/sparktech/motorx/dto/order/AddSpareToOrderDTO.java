package com.sparktech.motorx.dto.order;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AddSpareToOrderDTO(
        @NotNull(message = "El ID del repuesto es obligatorio")
        Long spareId,
        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = 1, message = "La cantidad debe ser mayor a cero")
        Integer quantity
) {
}

