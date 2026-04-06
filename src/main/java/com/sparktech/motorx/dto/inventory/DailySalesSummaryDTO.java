package com.sparktech.motorx.dto.inventory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DailySalesSummaryDTO(
        LocalDate date,
        BigDecimal totalSales,
        Integer transactionCount,
        List<SaleTransactionResponseDTO> sales
) {
}

