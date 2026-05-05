package com.sparktech.motorx.mapper;

import com.sparktech.motorx.dto.order.OrderProcedureResponseDTO;
import com.sparktech.motorx.dto.order.OrderResponseDTO;
import com.sparktech.motorx.dto.order.OrderSpareResponseDTO;
import com.sparktech.motorx.entity.OrderProcedureEntity;
import com.sparktech.motorx.entity.OrderServiceEntity;
import com.sparktech.motorx.entity.OrderSpareEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class OrderServiceMapper {

    public OrderResponseDTO toResponseDTO(OrderServiceEntity order) {
        List<OrderProcedureResponseDTO> procedures = order.getProcedures().stream()
                .map(this::toProcedureDTO)
                .toList();

        List<OrderSpareResponseDTO> spares = order.getSpares().stream()
                .map(this::toSpareDTO)
                .toList();

        return new OrderResponseDTO(
                order.getId(),
                order.getAppointment().getId(),
                order.getEmployee().getId(),
                order.getStartDate(),
                order.getEndDate(),
                order.getTotalServices(),
                order.getTotalSpareParts(),
                order.getTotalToPay(),
                order.getStatus(),
                procedures,
                spares
        );
    }

    private OrderProcedureResponseDTO toProcedureDTO(OrderProcedureEntity entity) {
        return new OrderProcedureResponseDTO(
                entity.getProcedure().getId(),
                entity.getProcedure().getName(),
                entity.getCost()
        );
    }

    private OrderSpareResponseDTO toSpareDTO(OrderSpareEntity entity) {
        BigDecimal lineTotal = entity.getUnitPrice().multiply(BigDecimal.valueOf(entity.getQuantity()));
        return new OrderSpareResponseDTO(
                entity.getSpare().getId(),
                entity.getSpare().getName(),
                entity.getQuantity(),
                entity.getUnitPrice(),
                lineTotal
        );
    }
}

