package com.sparktech.motorx.dto.appointment;

import com.sparktech.motorx.entity.AppointmentStatus;
import com.sparktech.motorx.entity.AppointmentType;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

/**
 * DTO de respuesta con la información completa de una cita.
 * Se devuelve al crear, consultar o modificar una cita.
 */
public record AppointmentResponseDTO(
        Long id,
        AppointmentType appointmentType,
        AppointmentStatus status,
        LocalDate appointmentDate,
        LocalTime startTime,
        LocalTime endTime,

        // Info del vehículo
        Long vehicleId,
        String vehiclePlate,
        String vehicleBrand,
        String vehicleModel,

        // Info del cliente
        Long clientId,
        String clientFullName,
        String clientEmail,

        // Técnico asignado (puede ser null si aún no se asignó)
        Long technicianId,
        String technicianFullName,

        String clientNotes,
        String adminNotes,

        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}