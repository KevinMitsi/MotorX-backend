package com.sparktech.motorx.mapper;

import com.sparktech.motorx.dto.inventory.PurchaseItemResponseDTO;
import com.sparktech.motorx.dto.inventory.PurchaseTransactionResponseDTO;
import com.sparktech.motorx.entity.PurchaseTransaction;
import com.sparktech.motorx.entity.PurchaseTransactionItem;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.math.BigDecimal;
import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface PurchaseTransactionMapper {

    default PurchaseTransactionResponseDTO toResponseDTO(PurchaseTransaction transaction) {
        List<PurchaseItemResponseDTO> items = transaction.getItems().stream()
                .map(this::toItemDTO)
                .toList();

        BigDecimal total = items.stream()
                .map(PurchaseItemResponseDTO::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new PurchaseTransactionResponseDTO(
                transaction.getId(),
                transaction.getSupplier(),
                transaction.getTransactionDate(),
                transaction.getCreatedBy().getId(),
                transaction.getCreatedBy().getEmail(),
                total,
                items
        );
    }

    default PurchaseItemResponseDTO toItemDTO(PurchaseTransactionItem item) {
        BigDecimal lineTotal = item.getPurchasePriceWithVat().multiply(BigDecimal.valueOf(item.getQuantity()));
        return new PurchaseItemResponseDTO(
                item.getId(),
                item.getSpare().getId(),
                item.getSpare().getName(),
                item.getQuantity(),
                item.getPurchasePriceWithVat(),
                lineTotal
        );
    }
}

