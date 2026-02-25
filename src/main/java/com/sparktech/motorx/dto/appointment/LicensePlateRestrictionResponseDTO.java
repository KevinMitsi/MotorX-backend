package com.sparktech.motorx.dto.appointment;


import java.time.LocalDate;

/**
 * DTO de respuesta cuando el vehículo tiene pico y placa el día solicitado.
 * El sistema impide el agendamiento y ofrece al cliente la opción de llamar
 * en caso de que sea urgente, advirtiendo que no es 100% seguro que puedan atenderlo.
 */
public record LicensePlateRestrictionResponseDTO(
        String vehiclePlate,
        LocalDate restrictedDate,
        String message,
        String urgentContactMessage,
        String phoneNumber,
        String businessHours
) {
    public static LicensePlateRestrictionResponseDTO of(String plate, LocalDate date) {
        return new LicensePlateRestrictionResponseDTO(
                plate,
                date,
                "La moto con placa " + plate + " tiene restricción de movilidad (pico y placa) " +
                        "el " + date + ". No es posible agendar la cita para ese día.",
                "Si tu cita es urgente, puedes llamarnos dentro del horario de atención. " +
                        "Ten en cuenta que no podemos garantizar la disponibilidad.",
                "+57 300 000 0000", // Reemplazar con número real
                "Lunes a Viernes 7:00 AM - 5:30 PM (excepto 12:00 - 1:00 PM)"
        );
    }
}
