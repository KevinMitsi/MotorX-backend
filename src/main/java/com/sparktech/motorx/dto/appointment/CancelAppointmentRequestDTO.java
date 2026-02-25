package com.sparktech.motorx.dto.appointment;
import jakarta.validation.constraints.NotNull;

/**
 * DTO para que el admin cancele una cita con opción de notificar al cliente.
 */
public record CancelAppointmentRequestDTO(

        @NotNull(message = "El motivo de cancelación es obligatorio")
        String reason,

        boolean notifyClient
) {}


