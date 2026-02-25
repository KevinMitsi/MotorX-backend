package com.sparktech.motorx.controller;

import com.sparktech.motorx.Services.IAuthService;
import com.sparktech.motorx.dto.auth.AuthResponseDTO;
import com.sparktech.motorx.dto.auth.LoginRequestDTO;
import com.sparktech.motorx.dto.auth.RegisterUserDTO;
import com.sparktech.motorx.dto.auth.Verify2FADTO;
import com.sparktech.motorx.dto.error.ResponseErrorDTO;
import com.sparktech.motorx.dto.user.UserDTO;
import com.sparktech.motorx.exception.InvalidPasswordException;
import com.sparktech.motorx.exception.UserNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Autenticación", description = "Registro, login, 2FA y gestión de sesión")
public class AuthController {

    private final IAuthService authService;

    /**
     * Login de usuario - Si es ADMIN devuelve AuthResponseDTO directamente.
     * Para otros roles genera y envía código 2FA.
     */
    @PostMapping("/login")
    @Operation(
            summary = "Iniciar sesión",
            description = "Autentica al usuario con email y contraseña. " +
                    "ADMIN obtiene token directamente; otros roles reciben un código 2FA por email."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login exitoso o código 2FA enviado"),
            @ApiResponse(responseCode = "401", description = "Credenciales inválidas",
                    content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos",
                    content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class)))
    })
    public ResponseEntity<@NotNull Object> login(@Valid @RequestBody LoginRequestDTO loginRequest) throws InvalidPasswordException {
        log.info("Petición de login recibida para: {}", loginRequest.email());
        Object response = authService.login(loginRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * Verificación de código 2FA
     */
    @PostMapping("/verify-2fa")
    @Operation(
            summary = "Verificar código 2FA",
            description = "Valida el código de verificación enviado por email y devuelve el token JWT."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Verificación exitosa, token JWT retornado"),
            @ApiResponse(responseCode = "400", description = "Código inválido o expirado",
                    content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "401", description = "Código incorrecto",
                    content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class)))
    })
    public ResponseEntity<@NotNull AuthResponseDTO> verify2FA(@Valid @RequestBody Verify2FADTO request) throws InvalidPasswordException {
        log.info("Petición de verificación 2FA recibida para: {}", request.email());
        AuthResponseDTO response = authService.verify2FA(request.email(), request.code());
        return ResponseEntity.ok(response);
    }

    /**
     * Registro de nuevo usuario
     */
    @PostMapping("/register")
    @Operation(
            summary = "Registrar nuevo usuario",
            description = "Crea una cuenta de cliente. El email y el DNI deben ser únicos en el sistema."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuario registrado, token JWT retornado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o email/DNI ya registrados",
                    content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class)))
    })
    public ResponseEntity<@NotNull AuthResponseDTO> register(@Valid @RequestBody RegisterUserDTO registerRequest) {
        log.info("Petición de registro recibida para: {}", registerRequest.email());
        AuthResponseDTO response = authService.register(registerRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * Obtener información del usuario actual autenticado
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Usuario actual", description = "Retorna los datos del usuario autenticado.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Datos del usuario"),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado",
                    content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class)))
    })
    public ResponseEntity<@NotNull UserDTO> getCurrentUser() throws UserNotFoundException {
        log.debug("Obteniendo información del usuario actual");
        UserDTO user = authService.getCurrentUser();
        return ResponseEntity.ok(user);
    }

    /**
     * Logout del usuario
     */
    @GetMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Cerrar sesión", description = "Invalida el token JWT del usuario autenticado.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Logout exitoso"),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class)))
    })
    public ResponseEntity<@NotNull String> logout() {
        log.info("Petición de logout recibida");
        authService.logout();
        return ResponseEntity.ok("Logout exitoso");
    }

    /**
     * Renovar token JWT
     */
    @PostMapping("/refresh")
    @Operation(
            summary = "Renovar token JWT",
            description = "Genera un nuevo access token a partir del refresh token."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token renovado exitosamente"),
            @ApiResponse(responseCode = "401", description = "Refresh token inválido o expirado",
                    content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class)))
    })
    public ResponseEntity<@NotNull AuthResponseDTO> refreshToken(@RequestParam String refreshToken) throws InvalidPasswordException {
        log.info("Petición de refresh token recibida");
        AuthResponseDTO response = authService.refreshToken(refreshToken);
        return ResponseEntity.ok(response);
    }

}