package com.sparktech.motorx.controller;

import com.sparktech.motorx.Services.IPasswordResetService;
import com.sparktech.motorx.dto.auth.PasswordResetDTO;
import com.sparktech.motorx.dto.auth.PasswordResetRequestDTO;
import com.sparktech.motorx.dto.error.ResponseErrorDTO;
import com.sparktech.motorx.exception.RecoveryTokenException;
import com.sparktech.motorx.exception.UserNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/password-reset")
@RequiredArgsConstructor
@Tag(name = "Recuperación de contraseña", description = "Solicitud y confirmación de reseteo de contraseña vía email")
public class PasswordResetController {

    private final IPasswordResetService passwordResetService;

    @PostMapping("/request")
    @Operation(
            summary = "Solicitar reseteo de contraseña",
            description = "Envía un código de recuperación al email indicado si existe en el sistema. " +
                    "Por seguridad, la respuesta es siempre 200 independientemente de si el email existe."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Solicitud procesada (código enviado si el email existe)"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos",
                    content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class)))
    })
    public ResponseEntity<@NotNull String> requestReset(@RequestBody @Valid PasswordResetRequestDTO dto) throws UserNotFoundException, RecoveryTokenException {
        passwordResetService.requestReset(dto);
        return ResponseEntity.ok("If the email exists, a recovery code has been sent.");
    }

    @PutMapping
    @Operation(
            summary = "Confirmar reseteo de contraseña",
            description = "Valida el token de recuperación y establece la nueva contraseña."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Contraseña restablecida exitosamente"),
            @ApiResponse(responseCode = "400", description = "Token inválido, expirado o datos incorrectos",
                    content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class)))
    })
    public ResponseEntity<@NotNull String> resetPassword(@RequestBody @Valid PasswordResetDTO dto) throws RecoveryTokenException {
        passwordResetService.resetPassword(dto);
        return ResponseEntity.ok("Password has been successfully reset.");
    }
}