package com.sparktech.motorx.dto.inventory;

import java.math.BigDecimal;

public record PurchaseItemResponseDTO(
        Long id,
        Long spareId,
        String spareName,
        Integer quantity,
        BigDecimal purchasePriceWithVat,
        BigDecimal lineTotal
) {
}

