package com.sparktech.motorx.dto.inventory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record SaleTransactionResponseDTO(
        Long id,
        LocalDateTime transactionDate,
        Long appointmentId,
        Long createdByUserId,
        String createdByEmail,
        BigDecimal totalAmount,
        List<SaleItemResponseDTO> items
) {
}

