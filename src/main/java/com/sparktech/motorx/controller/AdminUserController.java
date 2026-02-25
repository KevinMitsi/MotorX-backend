package com.sparktech.motorx.controller;

import com.sparktech.motorx.Services.IAdminUserService;
import com.sparktech.motorx.dto.error.ResponseErrorDTO;
import com.sparktech.motorx.dto.user.AdminUserResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@Tag(name = "Admin - Usuarios", description = "Gestión administrativa de usuarios: listar, consultar, bloquear y eliminar (soft delete)")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final IAdminUserService adminUserService;

    // ---------------------------------------------------------------
    // CONSULTA
    // ---------------------------------------------------------------

    @GetMapping
    @Operation(
            summary = "Listar todos los usuarios",
            description = "Devuelve la lista completa de usuarios registrados en el sistema, " +
                    "incluyendo los eliminados lógicamente (deletedAt != null)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de usuarios obtenida exitosamente")
    })
    public ResponseEntity<@NotNull List<AdminUserResponseDTO>> getAllUsers() {
        return ResponseEntity.ok(adminUserService.getAllUsers());
    }

    @GetMapping("/{userId}")
    @Operation(
            summary = "Detalle de un usuario",
            description = "Obtiene la información completa de un usuario por su ID."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuario encontrado"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado",
                    content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class)))
    })
    public ResponseEntity<@NotNull AdminUserResponseDTO> getUserById(@PathVariable Long userId) {
        return ResponseEntity.ok(adminUserService.getUserById(userId));
    }

    // ---------------------------------------------------------------
    // BLOQUEO / DESBLOQUEO
    // ---------------------------------------------------------------

    @PatchMapping("/{userId}/block")
    @Operation(
            summary = "Bloquear usuario",
            description = "Bloquea la cuenta del usuario impidiendo que pueda iniciar sesión. " +
                    "Lanza conflicto si la cuenta ya estaba bloqueada."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuario bloqueado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado",
                    content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "409", description = "El usuario ya se encuentra bloqueado",
                    content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class)))
    })
    public ResponseEntity<@NotNull AdminUserResponseDTO> blockUser(@PathVariable Long userId) {
        return ResponseEntity.ok(adminUserService.blockUser(userId));
    }

    @PatchMapping("/{userId}/unblock")
    @Operation(
            summary = "Desbloquear usuario",
            description = "Reactiva la cuenta de un usuario previamente bloqueado, " +
                    "permitiéndole volver a iniciar sesión."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuario desbloqueado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado",
                    content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class)))
    })
    public ResponseEntity<@NotNull AdminUserResponseDTO> unblockUser(@PathVariable Long userId) {
        return ResponseEntity.ok(adminUserService.unblockUser(userId));
    }

    // ---------------------------------------------------------------
    // SOFT DELETE
    // ---------------------------------------------------------------

    @DeleteMapping("/{userId}")
    @Operation(
            summary = "Eliminar usuario (soft delete)",
            description = "Realiza una eliminación lógica del usuario: establece la fecha de eliminación " +
                    "(deletedAt), desactiva la cuenta y la bloquea. No borra el registro de la base de datos " +
                    "para preservar el historial de citas y vehículos asociados."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Usuario eliminado lógicamente"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado",
                    content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "409", description = "El usuario ya ha sido eliminado",
                    content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class)))
    })
    public ResponseEntity<@NotNull Object> deleteUser(@PathVariable Long userId) {
        adminUserService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }
}
