package com.sparktech.motorx.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Service Order Entities - Unit Tests")
class ServiceOrderEntitiesTest {

    @Test
    @DisplayName("ProcedureEntity prePersist asigna createdAt y updatedAt")
    void procedurePrePersistShouldSetTimestamps() throws Exception {
        ProcedureEntity entity = new ProcedureEntity();

        Method prePersist = ProcedureEntity.class.getDeclaredMethod("prePersist");
        prePersist.setAccessible(true);
        prePersist.invoke(entity);

        assertThat(entity.getCreatedAt()).isNotNull();
        assertThat(entity.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("ProcedureEntity preUpdate actualiza updatedAt")
    void procedurePreUpdateShouldSetUpdatedAt() throws Exception {
        ProcedureEntity entity = new ProcedureEntity();

        Method preUpdate = ProcedureEntity.class.getDeclaredMethod("preUpdate");
        preUpdate.setAccessible(true);
        preUpdate.invoke(entity);

        assertThat(entity.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("OrderServiceEntity prePersist asigna totales y timestamps")
    void orderPrePersistShouldSetTotalsAndTimestamps() throws Exception {
        OrderServiceEntity order = new OrderServiceEntity();
        order.setStartDate(LocalDateTime.now());
        order.setTotalServices(null);
        order.setTotalSpareParts(null);
        order.setTotalToPay(null);

        Method prePersist = OrderServiceEntity.class.getDeclaredMethod("prePersist");
        prePersist.setAccessible(true);
        prePersist.invoke(order);

        assertThat(order.getTotalServices()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(order.getTotalSpareParts()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(order.getTotalToPay()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(order.getCreatedAt()).isNotNull();
        assertThat(order.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("OrderServiceEntity preUpdate actualiza updatedAt")
    void orderPreUpdateShouldSetUpdatedAt() throws Exception {
        OrderServiceEntity order = new OrderServiceEntity();
        order.setStartDate(LocalDateTime.now());

        Method preUpdate = OrderServiceEntity.class.getDeclaredMethod("preUpdate");
        preUpdate.setAccessible(true);
        preUpdate.invoke(order);

        assertThat(order.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("OrderServiceEntity valida endDate anterior a startDate")
    void orderShouldFailWhenEndDateBeforeStartDate() throws Exception {
        OrderServiceEntity order = new OrderServiceEntity();
        order.setStartDate(LocalDateTime.now());
        order.setEndDate(LocalDateTime.now().minusDays(1));

        Method prePersist = OrderServiceEntity.class.getDeclaredMethod("prePersist");
        prePersist.setAccessible(true);

        assertThatThrownBy(() -> prePersist.invoke(order))
                .hasRootCauseInstanceOf(IllegalArgumentException.class)
                .rootCause()
                .hasMessageContaining("End date cannot be before start date");
    }

    @Test
    @DisplayName("OrderProcedureId soporta igualdad por valores")
    void orderProcedureIdShouldSupportEquality() {
        OrderProcedureId first = new OrderProcedureId(1L, 2L);
        OrderProcedureId second = new OrderProcedureId(1L, 2L);

        assertThat(first).isEqualTo(second);
        assertThat(first).hasSameHashCodeAs(second);
    }

    @Test
    @DisplayName("OrderSpareId soporta igualdad por valores")
    void orderSpareIdShouldSupportEquality() {
        OrderSpareId first = new OrderSpareId(3L, 4L);
        OrderSpareId second = new OrderSpareId(3L, 4L);

        assertThat(first).isEqualTo(second);
        assertThat(first).hasSameHashCodeAs(second);
    }
}
