package com.sparktech.motorx.controller;

import com.sparktech.motorx.Services.INotificationService;
import com.sparktech.motorx.dto.error.ResponseErrorDTO;
import com.sparktech.motorx.dto.notification.CreateNotificationDTO;
import com.sparktech.motorx.dto.notification.MarkAllNotificationsReadResponseDTO;
import com.sparktech.motorx.dto.notification.NotificationResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notificaciones", description = "Gestion de notificaciones internas por usuario")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final INotificationService notificationService;

    @PostMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear notificacion para un usuario", description = "Permite al administrador registrar una notificacion en la bandeja de un usuario.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Notificacion creada exitosamente",
                    content = @Content(schema = @Schema(implementation = NotificationResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Solicitud invalida",
                    content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos de administrador",
                    content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "404", description = "Usuario destino no encontrado",
                    content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class)))
    })
    public ResponseEntity<@NotNull NotificationResponseDTO> createNotification(@Valid @RequestBody CreateNotificationDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(notificationService.createNotification(dto));
    }

    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Listar mis notificaciones", description = "Lista las notificaciones del usuario autenticado. Puede filtrarse solo no leidas.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Consulta exitosa"),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class)))
    })
    public ResponseEntity<@NotNull List<NotificationResponseDTO>> getMyNotifications(
            @Parameter(description = "Si es true solo retorna notificaciones no leidas")
            @RequestParam(defaultValue = "false") boolean onlyUnread
    ) {
        return ResponseEntity.ok(notificationService.getMyNotifications(onlyUnread));
    }

    @PatchMapping("/my/{notificationId}/read")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Marcar notificacion como leida", description = "Marca como leida una notificacion de la bandeja del usuario autenticado.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Notificacion actualizada",
                    content = @Content(schema = @Schema(implementation = NotificationResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "404", description = "Notificacion no encontrada",
                    content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class)))
    })
    public ResponseEntity<@NotNull NotificationResponseDTO> markAsRead(
            @Parameter(description = "ID de la notificacion")
            @PathVariable Long notificationId
    ) {
        return ResponseEntity.ok(notificationService.markAsRead(notificationId));
    }

    @PatchMapping("/my/read-all")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Marcar todas como leidas", description = "Marca como leidas todas las notificaciones pendientes del usuario autenticado.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Notificaciones actualizadas",
                    content = @Content(schema = @Schema(implementation = MarkAllNotificationsReadResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class)))
    })
    public ResponseEntity<@NotNull MarkAllNotificationsReadResponseDTO> markAllAsRead() {
        return ResponseEntity.ok(new MarkAllNotificationsReadResponseDTO(notificationService.markAllAsRead()));
    }

    @GetMapping("/admin/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar notificaciones por usuario", description = "Permite al administrador consultar la bandeja completa de un usuario específico.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Consulta exitosa"),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos de administrador",
                    content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado",
                    content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class)))
    })
    public ResponseEntity<@NotNull List<NotificationResponseDTO>> getNotificationsByUser(
            @Parameter(description = "ID del usuario") @PathVariable Long userId
    ) {
        return ResponseEntity.ok(notificationService.getNotificationsByUserId(userId));
    }
}

