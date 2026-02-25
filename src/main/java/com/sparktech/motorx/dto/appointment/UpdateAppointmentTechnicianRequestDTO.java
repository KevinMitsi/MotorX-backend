package com.sparktech.motorx.dto.appointment;

import jakarta.validation.constraints.NotNull;

/**
 * DTO para que el admin cambie el técnico asignado a una cita.
 * Según las reglas de negocio: el admin puede cambiar el técnico
 * (NO el horario) siempre que el técnico destino tenga ese slot libre.
 */
public record UpdateAppointmentTechnicianRequestDTO(

        @NotNull(message = "El ID del nuevo técnico es obligatorio")
        Long newTechnicianId,

        // El admin elige si notificar o no al cliente por correo
        boolean notifyClient
) {}
