package com.sparktech.motorx.mapper;

import com.sparktech.motorx.dto.inventory.SaleItemResponseDTO;
import com.sparktech.motorx.dto.inventory.SaleTransactionResponseDTO;
import com.sparktech.motorx.entity.SaleTransaction;
import com.sparktech.motorx.entity.SaleTransactionItem;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.math.BigDecimal;
import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface SaleTransactionMapper {

    default SaleTransactionResponseDTO toResponseDTO(SaleTransaction transaction) {
        List<SaleItemResponseDTO> items = transaction.getItems().stream()
                .map(this::toItemDTO)
                .toList();

        BigDecimal total = items.stream()
                .map(SaleItemResponseDTO::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new SaleTransactionResponseDTO(
                transaction.getId(),
                transaction.getTransactionDate(),
                transaction.getAppointment() != null ? transaction.getAppointment().getId() : null,
                transaction.getCreatedBy().getId(),
                transaction.getCreatedBy().getEmail(),
                total,
                items
        );
    }

    default SaleItemResponseDTO toItemDTO(SaleTransactionItem item) {
        BigDecimal lineTotal = item.getSalePriceAtMoment().multiply(BigDecimal.valueOf(item.getQuantity()));
        return new SaleItemResponseDTO(
                item.getId(),
                item.getSpare().getId(),
                item.getSpare().getName(),
                item.getQuantity(),
                item.getSalePriceAtMoment(),
                lineTotal
        );
    }
}

