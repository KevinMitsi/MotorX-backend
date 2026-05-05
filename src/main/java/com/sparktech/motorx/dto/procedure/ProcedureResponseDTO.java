package com.sparktech.motorx.dto.procedure;

import java.time.LocalDateTime;

public record ProcedureResponseDTO(
        Long id,
        String name,
        String description,
        Boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

