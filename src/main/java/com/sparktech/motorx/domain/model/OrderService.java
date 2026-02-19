package com.sparktech.motorx.domain.model;

import com.sparktech.motorx.domain.enums.OrderStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderService {
    private Long id;
    private Appointment appointment;
    private Employee employee;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private BigDecimal totalServices;
    private BigDecimal totalSpareParts;
    private BigDecimal totalToPay;
    private OrderStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}

