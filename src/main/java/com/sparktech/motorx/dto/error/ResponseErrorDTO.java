package com.sparktech.motorx.dto.error;

/**
 * DTO estándar para respuestas de error en toda la aplicación.
 *
 * @param code    Código HTTP del error (ej. 400, 404, 409...).
 * @param message Mensaje descriptivo del tipo de error.
 * @param details Detalles adicionales: puede ser un Map con campos de validación
 *                o cualquier objeto con información contextual.
 */
public record ResponseErrorDTO(
        Integer code,
        String message,
        Object details
) {}

