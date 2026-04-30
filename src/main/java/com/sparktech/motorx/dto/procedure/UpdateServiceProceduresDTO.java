package com.sparktech.motorx.dto.procedure;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record UpdateServiceProceduresDTO(
        @NotNull(message = "La lista de procedimientos es obligatoria")
        List<Long> procedureIds
) {
}

