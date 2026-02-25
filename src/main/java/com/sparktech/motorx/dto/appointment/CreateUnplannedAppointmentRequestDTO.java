package com.sparktech.motorx.dto.appointment;
import com.sparktech.motorx.entity.AppointmentType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO exclusivo para que el administrador registre citas no planeadas.
 * Permite asignar técnico manualmente y registrar citas fuera de
 * los horarios de recepción estándar, llenando espacios disponibles.
 */
public record CreateUnplannedAppointmentRequestDTO(

        @NotNull(message = "El ID del vehículo es obligatorio")
        Long vehicleId,

        @NotNull(message = "El tipo de cita es obligatorio")
        AppointmentType appointmentType,

        @NotNull(message = "La fecha de la cita es obligatoria")
        LocalDate appointmentDate,

        @NotNull(message = "El horario de inicio es obligatorio")
        LocalTime startTime,

        @NotNull(message = "El kilometraje actual es obligatorio")
        @Min(value = 0, message = "El kilometraje no puede ser negativo")
        Integer currentMileage,

        // En citas no planeadas el admin puede asignar técnico directamente
        Long technicianId,

        String adminNotes
) {}