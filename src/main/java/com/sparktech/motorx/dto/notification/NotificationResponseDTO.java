package com.sparktech.motorx.dto.notification;

import com.sparktech.motorx.entity.NotificationUrgency;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(name = "NotificationResponseDTO", description = "Notificacion persistida de un usuario")
public record NotificationResponseDTO(
        Long id,
        Long userId,
        String title,
        String description,
        NotificationUrgency urgency,
        Boolean isRead,
        String source,
        LocalDateTime createdAt,
        LocalDateTime readAt
) {
}

