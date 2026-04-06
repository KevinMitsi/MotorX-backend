package com.sparktech.motorx.mapper;

import com.sparktech.motorx.dto.inventory.PurchaseTransactionResponseDTO;
import com.sparktech.motorx.dto.inventory.SaleTransactionResponseDTO;
import com.sparktech.motorx.entity.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Inventory Mappers - Unit Tests")
class InventoryMappersTest {

    private final PurchaseTransactionMapper purchaseMapper = new PurchaseTransactionMapper() { };
    private final SaleTransactionMapper saleMapper = new SaleTransactionMapper() { };

    @Test
    @DisplayName("PurchaseTransactionMapper calcula total e items")
    void purchaseMapperShouldMapAndCalculateTotals() {
        UserEntity createdBy = new UserEntity();
        createdBy.setId(7L);
        createdBy.setEmail("warehouse@test.com");

        Spare spare = new Spare();
        spare.setId(10L);
        spare.setName("Filtro");

        PurchaseTransaction tx = new PurchaseTransaction();
        tx.setId(1L);
        tx.setSupplier("Proveedor");
        tx.setTransactionDate(LocalDateTime.now());
        tx.setCreatedBy(createdBy);

        PurchaseTransactionItem item = new PurchaseTransactionItem();
        item.setId(100L);
        item.setSpare(spare);
        item.setQuantity(2);
        item.setPurchasePriceWithVat(new BigDecimal("20"));
        tx.setItems(List.of(item));

        PurchaseTransactionResponseDTO result = purchaseMapper.toResponseDTO(tx);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.createdByUserId()).isEqualTo(7L);
        assertThat(result.items()).hasSize(1);
        assertThat(result.totalAmount()).isEqualByComparingTo("40");
        assertThat(result.items().getFirst().lineTotal()).isEqualByComparingTo("40");
    }

    @Test
    @DisplayName("SaleTransactionMapper mapea con appointmentId y total")
    void saleMapperShouldMapWithAppointment() {
        UserEntity createdBy = new UserEntity();
        createdBy.setId(8L);
        createdBy.setEmail("reception@test.com");

        AppointmentEntity appointment = new AppointmentEntity();
        appointment.setId(50L);

        Spare spare = new Spare();
        spare.setId(11L);
        spare.setName("Aceite");

        SaleTransaction tx = new SaleTransaction();
        tx.setId(2L);
        tx.setTransactionDate(LocalDateTime.now());
        tx.setCreatedBy(createdBy);
        tx.setAppointment(appointment);

        SaleTransactionItem item = new SaleTransactionItem();
        item.setId(101L);
        item.setSpare(spare);
        item.setQuantity(3);
        item.setSalePriceAtMoment(new BigDecimal("15"));
        tx.setItems(List.of(item));

        SaleTransactionResponseDTO result = saleMapper.toResponseDTO(tx);

        assertThat(result.id()).isEqualTo(2L);
        assertThat(result.appointmentId()).isEqualTo(50L);
        assertThat(result.totalAmount()).isEqualByComparingTo("45");
    }

    @Test
    @DisplayName("SaleTransactionMapper soporta appointment nulo")
    void saleMapperShouldMapWithNullAppointment() {
        UserEntity createdBy = new UserEntity();
        createdBy.setId(8L);
        createdBy.setEmail("reception@test.com");

        Spare spare = new Spare();
        spare.setId(11L);
        spare.setName("Aceite");

        SaleTransaction tx = new SaleTransaction();
        tx.setId(3L);
        tx.setTransactionDate(LocalDateTime.now());
        tx.setCreatedBy(createdBy);
        tx.setAppointment(null);

        SaleTransactionItem item = new SaleTransactionItem();
        item.setId(102L);
        item.setSpare(spare);
        item.setQuantity(1);
        item.setSalePriceAtMoment(new BigDecimal("25"));
        tx.setItems(List.of(item));

        SaleTransactionResponseDTO result = saleMapper.toResponseDTO(tx);

        assertThat(result.appointmentId()).isNull();
        assertThat(result.totalAmount()).isEqualByComparingTo("25");
    }
}

