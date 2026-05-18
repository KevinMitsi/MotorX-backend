package com.sparktech.motorx.dto.service;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdateServiceDTO(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150, message = "El nombre no puede exceder 150 caracteres")
        String name,
        @Size(max = 1000, message = "La descripcion no puede exceder 1000 caracteres")
        String description,
        @NotNull(message = "La duracion estimada es obligatoria")
        @Min(value = 1, message = "La duracion minima es 1 minuto")
        Integer estimatedDurationMinutes,
        @NotNull(message = "El precio base es obligatorio")
        @DecimalMin(value = "0.0", inclusive = true, message = "El precio no puede ser negativo")
        BigDecimal basePrice,
        Boolean active
) {
}

