package com.sparktech.motorx.dto.inventory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PurchaseTransactionResponseDTO(
        Long id,
        String supplier,
        LocalDateTime transactionDate,
        Long createdByUserId,
        String createdByEmail,
        BigDecimal totalAmount,
        List<PurchaseItemResponseDTO> items
) {
}

