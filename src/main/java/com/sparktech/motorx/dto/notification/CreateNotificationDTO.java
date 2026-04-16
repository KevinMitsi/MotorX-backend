package com.sparktech.motorx.dto.notification;

import com.sparktech.motorx.entity.NotificationUrgency;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(name = "CreateNotificationDTO", description = "Datos para crear una notificacion para un usuario")
public record CreateNotificationDTO(
        @Schema(description = "ID del usuario destino", example = "5")
        @NotNull(message = "El ID del usuario es obligatorio")
        Long userId,

        @Schema(description = "Titulo corto de la notificacion", example = "Inventario actualizado")
        @NotBlank(message = "El titulo es obligatorio")
        @Size(max = 120, message = "El titulo no puede superar 120 caracteres")
        String title,

        @Schema(description = "Descripcion detallada", example = "Se registraron nuevos repuestos en bodega")
        @NotBlank(message = "La descripcion es obligatoria")
        @Size(max = 500, message = "La descripcion no puede superar 500 caracteres")
        String description,

        @Schema(description = "Nivel de urgencia", example = "MEDIUM")
        @NotNull(message = "La urgencia es obligatoria")
        NotificationUrgency urgency,

        @Schema(description = "Origen funcional del evento", example = "INVENTORY")
        @Size(max = 80, message = "La fuente no puede superar 80 caracteres")
        String source
) {
}

