package com.sparktech.motorx.dto.order;

import java.math.BigDecimal;

public record OrderSpareResponseDTO(
        Long spareId,
        String spareName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal
) {
}

