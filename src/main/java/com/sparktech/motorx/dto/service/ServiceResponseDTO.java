package com.sparktech.motorx.dto.service;

import com.sparktech.motorx.dto.procedure.ProcedureResponseDTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ServiceResponseDTO(
        Long id,
        String name,
        String description,
        Integer estimatedDurationMinutes,
        BigDecimal basePrice,
        Boolean active,
        List<ProcedureResponseDTO> baseProcedures,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

