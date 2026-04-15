package com.sparktech.motorx.dto.inventory;

import com.sparktech.motorx.dto.reception.ConfirmReceptionDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Inventory DTOs - Unit Tests")
class InventoryDtoTest {

    @Test
    @DisplayName("records exponen correctamente sus propiedades")
    void recordsShouldExposeProperties() {
        CreateSpareDTO createSpareDTO = new CreateSpareDTO("Filtro", "AKT", "SAV-1", "REP-1", BigDecimal.TEN, false, "Prov", 3, 2, "01-01-01-01");
        UpdateSpareDTO updateSpareDTO = new UpdateSpareDTO("Filtro2", "Yamaha", "SAV-2", "REP-2", BigDecimal.ONE, true, "Prov2", 5, 2, "02-02-02-02");
        UpdateSparePurchasePriceDTO priceDTO = new UpdateSparePurchasePriceDTO(new BigDecimal("12.5"));

        CreatePurchaseItemDTO purchaseItemDTO = new CreatePurchaseItemDTO(1L, 2, BigDecimal.TEN);
        CreatePurchaseTransactionDTO purchaseTxDTO = new CreatePurchaseTransactionDTO("Proveedor", List.of(purchaseItemDTO));

        CreateSaleItemDTO saleItemDTO = new CreateSaleItemDTO(2L, 1);
        CreateSaleTransactionDTO saleTxDTO = new CreateSaleTransactionDTO(5L, List.of(saleItemDTO));

        PurchaseItemResponseDTO purchaseItemResponseDTO = new PurchaseItemResponseDTO(1L, 2L, "Filtro", 2, BigDecimal.TEN, new BigDecimal("20"));
        PurchaseTransactionResponseDTO purchaseResponseDTO = new PurchaseTransactionResponseDTO(1L, "Proveedor", LocalDateTime.now(), 1L, "w@test.com", new BigDecimal("20"), List.of(purchaseItemResponseDTO));

        SaleItemResponseDTO saleItemResponseDTO = new SaleItemResponseDTO(1L, 2L, "Filtro", 1, new BigDecimal("13.5"), new BigDecimal("13.5"));
        SaleTransactionResponseDTO saleResponseDTO = new SaleTransactionResponseDTO(1L, LocalDateTime.now(), 5L, 1L, "r@test.com", new BigDecimal("13.5"), List.of(saleItemResponseDTO));

        DailySalesSummaryDTO dailySalesSummaryDTO = new DailySalesSummaryDTO(LocalDate.now(), new BigDecimal("33.5"), 1, List.of(saleResponseDTO));
        SpareResponseDTO spareResponseDTO = new SpareResponseDTO(1L, "Filtro", "AKT", "SAV-1", "REP-1", BigDecimal.TEN, new BigDecimal("13.5"), false, "Prov", 4, 2, "01-01-01-01");
        ConfirmReceptionDTO confirmReceptionDTO = new ConfirmReceptionDTO("ABC123", "1234");

        assertThat(createSpareDTO.name()).isEqualTo("Filtro");
        assertThat(updateSpareDTO.isOil()).isTrue();
        assertThat(priceDTO.purchasePriceWithVat()).isEqualByComparingTo("12.5");
        assertThat(purchaseTxDTO.items()).hasSize(1);
        assertThat(saleTxDTO.appointmentId()).isEqualTo(5L);
        assertThat(purchaseResponseDTO.totalAmount()).isEqualByComparingTo("20");
        assertThat(saleResponseDTO.totalAmount()).isEqualByComparingTo("13.5");
        assertThat(dailySalesSummaryDTO.transactionCount()).isEqualTo(1);
        assertThat(spareResponseDTO.salePrice()).isEqualByComparingTo("13.5");
        assertThat(confirmReceptionDTO.code()).isEqualTo("1234");
    }
}

