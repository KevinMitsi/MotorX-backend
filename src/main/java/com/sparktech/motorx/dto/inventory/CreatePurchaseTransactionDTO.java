package com.sparktech.motorx.dto.inventory;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

@Schema(name = "CreatePurchaseTransactionDTO", description = "Solicitud para registrar una compra")
public record CreatePurchaseTransactionDTO(
        @NotBlank(message = "El proveedor es obligatorio")
        String supplier,
        @NotEmpty(message = "Debe incluir al menos un item")
        @Valid
        List<CreatePurchaseItemDTO> items
) {
}

