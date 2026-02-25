package com.sparktech.motorx.dto.appointment;
import com.sparktech.motorx.entity.AppointmentType;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * DTO de respuesta que devuelve los horarios disponibles para agendar
 * una cita de un tipo específico en una fecha determinada.
 * Un slot es disponible si al menos un técnico tiene ese espacio libre.
 */
public record AvailableSlotsResponseDTO(
        LocalDate date,
        AppointmentType appointmentType,
        List<AvailableSlotDTO> availableSlots
) {
    public record AvailableSlotDTO(
            LocalTime startTime,
            LocalTime endTime,
            // Cuántos técnicos tienen ese slot libre (para info del sistema)
            int availableTechnicians
    ) {}
}


