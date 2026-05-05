package com.sparktech.motorx.dto.order;

import com.sparktech.motorx.entity.OrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Order DTOs - Unit Tests")
class OrderDtoTest {

    @Test
    @DisplayName("records exponen correctamente sus propiedades")
    void recordsShouldExposeProperties() {
        AddProcedureToOrderDTO addProcedureToOrderDTO = new AddProcedureToOrderDTO(5L, new BigDecimal("50"));
        AddSpareToOrderDTO addSpareToOrderDTO = new AddSpareToOrderDTO(6L, 2);
        UpdateOrderProcedureCostDTO updateOrderProcedureCostDTO = new UpdateOrderProcedureCostDTO(new BigDecimal("70"));

        OrderProcedureResponseDTO procedureResponseDTO = new OrderProcedureResponseDTO(10L, "Cambio aceite", new BigDecimal("40"));
        OrderSpareResponseDTO spareResponseDTO = new OrderSpareResponseDTO(11L, "Filtro", 2, new BigDecimal("135"), new BigDecimal("270"));

        OrderResponseDTO orderResponseDTO = new OrderResponseDTO(
                1L,
                2L,
                3L,
                LocalDateTime.now(),
                null,
                new BigDecimal("40"),
                new BigDecimal("270"),
                new BigDecimal("310"),
                OrderStatus.IN_PROGRESS,
                List.of(procedureResponseDTO),
                List.of(spareResponseDTO)
        );

        assertThat(addProcedureToOrderDTO.procedureId()).isEqualTo(5L);
        assertThat(addSpareToOrderDTO.quantity()).isEqualTo(2);
        assertThat(updateOrderProcedureCostDTO.cost()).isEqualByComparingTo("70");
        assertThat(procedureResponseDTO.procedureName()).isEqualTo("Cambio aceite");
        assertThat(spareResponseDTO.lineTotal()).isEqualByComparingTo("270");
        assertThat(orderResponseDTO.totalToPay()).isEqualByComparingTo("310");
        assertThat(orderResponseDTO.procedures()).hasSize(1);
    }
}

