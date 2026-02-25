package com.sparktech.motorx.dto.appointment;

import com.sparktech.motorx.entity.AppointmentType;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

/**
 * DTO que el cliente envía para solicitar una nueva cita.
 * El sistema valida disponibilidad, pico y placa, tipo de moto,
 * y asigna técnico automáticamente.
 */
public record CreateAppointmentRequestDTO(

        @NotNull(message = "El ID del vehículo es obligatorio")
        Long vehicleId,

        @NotNull(message = "El tipo de cita es obligatorio")
        AppointmentType appointmentType,

        @NotNull(message = "La fecha de la cita es obligatoria")
        @Future(message = "La fecha debe ser en el futuro")
        LocalDate appointmentDate,

        @NotNull(message = "El horario de inicio es obligatorio")
        LocalTime startTime,

        @NotNull(message = "El kilometraje actual es obligatorio")
        @Min(value = 0, message = "El kilometraje no puede ser negativo")
        Integer currentMileage,

        // Campo opcional: notas del cliente sobre el problema
        Set<String> clientNotes
) {}