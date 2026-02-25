package com.sparktech.motorx.dto.appointment;


/**
 * DTO especial que se devuelve cuando el usuario intenta agendar un REPROCESO.
 * El sistema no permite agendar este tipo directamente; redirige al cliente
 * a comunicarse con el taller dentro del horario de atención.
 */
public record ReworkRedirectResponseDTO(
        String message,
        String whatsappLink,
        String phoneNumber,
        String businessHours
) {
    public static ReworkRedirectResponseDTO defaultResponse() {
        return new ReworkRedirectResponseDTO(
                "Los reprocesos requieren atención personalizada. " +
                        "Por favor contáctanos directamente para agendar tu cita.",
                "https://wa.me/573108402499", // Reemplazar con número real
                "+57 310 8402499",           // Reemplazar con número real
                "Lunes a Viernes 7:00 AM - 5:30 PM (excepto 12:00 - 1:00 PM)"
        );
    }
}