package com.sparktech.motorx.dto.inventory;

import java.math.BigDecimal;

public record SaleItemResponseDTO(
        Long id,
        Long spareId,
        String spareName,
        Integer quantity,
        BigDecimal salePriceAtMoment,
        BigDecimal lineTotal
) {
}

