package com.sparktech.motorx.dto.order;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record TechnicianDailyOrderDTO(
        Long appointmentId,
        Long orderId,
        String licensePlate,
        String brand,
        String model,
        LocalDate appointmentDate,
        LocalTime startTime,
        LocalDateTime processStartedAt
) {
}

