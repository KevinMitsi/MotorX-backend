package com.sparktech.motorx.dto.order;

import com.sparktech.motorx.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponseDTO(
        Long id,
        Long appointmentId,
        Long employeeId,
        LocalDateTime startDate,
        LocalDateTime endDate,
        BigDecimal totalServices,
        BigDecimal totalSpareParts,
        BigDecimal totalToPay,
        OrderStatus status,
        List<OrderProcedureResponseDTO> procedures,
        List<OrderSpareResponseDTO> spares
) {
}

