package com.sparktech.motorx.dto.order;

import java.math.BigDecimal;

public record OrderProcedureResponseDTO(
        Long procedureId,
        String procedureName,
        BigDecimal cost
) {
}

