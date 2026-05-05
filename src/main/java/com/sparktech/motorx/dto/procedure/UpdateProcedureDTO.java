package com.sparktech.motorx.dto.procedure;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProcedureDTO(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150, message = "El nombre no puede exceder 150 caracteres")
        String name,
        @Size(max = 1000, message = "La descripcion no puede exceder 1000 caracteres")
        String description,
        Boolean active
) {
}

