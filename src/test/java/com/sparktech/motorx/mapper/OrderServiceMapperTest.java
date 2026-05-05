package com.sparktech.motorx.mapper;

import com.sparktech.motorx.dto.order.OrderResponseDTO;
import com.sparktech.motorx.entity.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OrderServiceMapper - Unit Tests")
class OrderServiceMapperTest {

    private final OrderServiceMapper mapper = new OrderServiceMapper();

    @Test
    @DisplayName("toResponseDTO mapea procedimientos y repuestos con totales")
    void toResponseDTOShouldMapProceduresAndSpares() {
        AppointmentEntity appointment = new AppointmentEntity();
        appointment.setId(10L);

        EmployeeEntity employee = new EmployeeEntity();
        employee.setId(20L);

        ProcedureEntity procedure = new ProcedureEntity();
        procedure.setId(30L);
        procedure.setName("Cambio aceite");

        Spare spare = new Spare();
        spare.setId(40L);
        spare.setName("Filtro");

        OrderServiceEntity order = new OrderServiceEntity();
        order.setId(1L);
        order.setAppointment(appointment);
        order.setEmployee(employee);
        order.setStartDate(LocalDateTime.now());
        order.setEndDate(null);
        order.setTotalServices(new BigDecimal("50"));
        order.setTotalSpareParts(new BigDecimal("270"));
        order.setTotalToPay(new BigDecimal("320"));
        order.setStatus(OrderStatus.IN_PROGRESS);

        OrderProcedureEntity orderProcedure = new OrderProcedureEntity();
        orderProcedure.setOrder(order);
        orderProcedure.setProcedure(procedure);
        orderProcedure.setCost(new BigDecimal("50"));

        OrderSpareEntity orderSpare = new OrderSpareEntity();
        orderSpare.setOrder(order);
        orderSpare.setSpare(spare);
        orderSpare.setQuantity(2);
        orderSpare.setUnitPrice(new BigDecimal("135"));

        order.setProcedures(List.of(orderProcedure));
        order.setSpares(List.of(orderSpare));

        OrderResponseDTO dto = mapper.toResponseDTO(order);

        assertThat(dto.appointmentId()).isEqualTo(10L);
        assertThat(dto.employeeId()).isEqualTo(20L);
        assertThat(dto.procedures()).hasSize(1);
        assertThat(dto.spares()).hasSize(1);
        assertThat(dto.spares().getFirst().lineTotal()).isEqualByComparingTo("270");
    }
}

