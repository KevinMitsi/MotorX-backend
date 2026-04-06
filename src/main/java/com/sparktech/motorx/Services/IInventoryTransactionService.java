package com.sparktech.motorx.Services;

import com.sparktech.motorx.dto.inventory.*;

import java.util.List;

public interface IInventoryTransactionService {
    PurchaseTransactionResponseDTO registerPurchase(CreatePurchaseTransactionDTO dto);

    List<PurchaseTransactionResponseDTO> getPurchases();

    PurchaseTransactionResponseDTO getPurchaseById(Long id);

    SaleTransactionResponseDTO registerSale(CreateSaleTransactionDTO dto);

    List<SaleTransactionResponseDTO> getSales();

    SaleTransactionResponseDTO getSaleById(Long id);

    DailySalesSummaryDTO getTodaySalesSummary();
}

