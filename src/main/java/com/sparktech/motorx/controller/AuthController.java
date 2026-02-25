package com.sparktech.motorx.controller;


import com.sparktech.motorx.Services.IAuthService;
import com.sparktech.motorx.dto.auth.AuthResponseDTO;
import com.sparktech.motorx.dto.auth.LoginRequestDTO;
import com.sparktech.motorx.dto.auth.RegisterUserDTO;
import com.sparktech.motorx.dto.auth.Verify2FADTO;
import com.sparktech.motorx.dto.user.UserDTO;
import com.sparktech.motorx.exception.InvalidPasswordException;
import com.sparktech.motorx.exception.UserNotFoundException;
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
public class AuthController {

    private final IAuthService authService;

    /**
     * Login de usuario - Si es ADMIN devuelve AuthResponseDTO directamente.
     * Para otros roles genera y envía código 2FA.
     */
    @PostMapping("/login")
    public ResponseEntity<@NotNull Object> login(@Valid @RequestBody LoginRequestDTO loginRequest) throws InvalidPasswordException {
        log.info("Petición de login recibida para: {}", loginRequest.email());
        Object response = authService.login(loginRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * Verificación de código 2FA
     */
    @PostMapping("/verify-2fa")
    public ResponseEntity<@NotNull AuthResponseDTO> verify2FA(@Valid @RequestBody Verify2FADTO request) throws InvalidPasswordException {
        log.info("Petición de verificación 2FA recibida para: {}", request.email());
        AuthResponseDTO response = authService.verify2FA(request.email(), request.code());
        return ResponseEntity.ok(response);
    }

    /**
     * Registro de nuevo usuario
     */
    @PostMapping("/register")
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
    public ResponseEntity<@NotNull String> logout() {
        log.info("Petición de logout recibida");
        authService.logout();
        return ResponseEntity.ok("Logout exitoso");
    }

    /**
     * Renovar token JWT
     */
    @PostMapping("/refresh")
    public ResponseEntity<@NotNull AuthResponseDTO> refreshToken(@RequestParam String refreshToken) throws InvalidPasswordException {
        log.info("Petición de refresh token recibida");
        AuthResponseDTO response = authService.refreshToken(refreshToken);
        return ResponseEntity.ok(response);
    }

}