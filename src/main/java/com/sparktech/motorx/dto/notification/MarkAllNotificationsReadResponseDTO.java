package com.sparktech.motorx.dto.notification;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "MarkAllNotificationsReadResponseDTO", description = "Resultado de marcar notificaciones como leidas")
public record MarkAllNotificationsReadResponseDTO(
        long updatedCount
) {
}

