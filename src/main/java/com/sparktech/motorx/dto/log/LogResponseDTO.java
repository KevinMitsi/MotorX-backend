package com.sparktech.motorx.dto.log;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(name = "LogResponseDTO", description = "Evento de auditoria persistido en el sistema")
public record LogResponseDTO(
        @Schema(description = "Identificador del log", example = "1")
        Long id,
        @Schema(description = "Modulo funcional que genera el evento", example = "AUTHENTICATION")
        String serviceName,
        @Schema(description = "Accion registrada", example = "LOGIN")
        String actionType,
        @Schema(description = "Resultado de la accion", example = "SUCCESS")
        String result,
        @Schema(description = "Correo del actor", example = "admin@motorx.com")
        String actorEmail,
        @Schema(description = "Id del actor autenticado", example = "5")
        Long actorUserId,
        @Schema(description = "Detalle legible del evento", example = "Inicio de sesion exitoso")
        String message,
        @Schema(description = "Fecha y hora de creacion", example = "2026-03-31T10:15:00")
        LocalDateTime createdAt
) {
}

