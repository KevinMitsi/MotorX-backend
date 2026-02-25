package com.sparktech.motorx.dto.notification;

import java.time.LocalDate;
import java.time.LocalTime;

public record AppointmentNotificationDTO(
        String clientEmail,
        String clientName,
        LocalDate appointmentDate,
        LocalTime startTime,
        String appointmentType,
        String vehicleBrand,
        String vehicleModel,
        String licensePlate,
        String technicianName
) {}

