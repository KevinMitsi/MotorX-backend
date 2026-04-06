package com.sparktech.motorx.dto.inventory;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CreateSaleTransactionDTO(
        Long appointmentId,
        @NotEmpty(message = "Debe incluir al menos un item")
        @Valid
        List<CreateSaleItemDTO> items
) {
}

