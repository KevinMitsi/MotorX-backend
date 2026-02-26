package com.sparktech.motorx.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sparktech.motorx.Services.IPasswordResetService;
import com.sparktech.motorx.controller.error.GlobalControllerAdvice;
import com.sparktech.motorx.dto.auth.PasswordResetDTO;
import com.sparktech.motorx.dto.auth.PasswordResetRequestDTO;
import com.sparktech.motorx.exception.RecoveryTokenException;
import com.sparktech.motorx.security.CustomUserDetailsService;
import com.sparktech.motorx.security.JwtAuthenticationFilter;
import com.sparktech.motorx.security.JwtService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = PasswordResetController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalControllerAdvice.class, PasswordResetControllerTest.TestConfig.class})
@DisplayName("PasswordResetController - Tests")
class PasswordResetControllerTest {

    @Resource
    private MockMvc mockMvc;

    @Resource
    private IPasswordResetService passwordResetService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        reset(passwordResetService);
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    private String json(Object o) throws Exception {
        return objectMapper.writeValueAsString(o);
    }

    // Provider for invalid emails used by parameterized tests
    static Stream<String> invalidEmails() {
        return Stream.of(null, "   ", "no-es-email");
    }

    // Provider for invalid new passwords for reset password tests
    static Stream<String> invalidNewPasswords() {
        return Stream.of(
                null,                       // newPassword null -> @NotBlank
                "Sec1@",                   // too short (5 chars)
                "Secure1@Secure1@Secur",   // too long (21 chars)
                "secure1@",                // no uppercase
                "SecureAB@",               // no digit
                "Secure123"                // no special symbol
        );
    }

    // ---------------------------------------------------------------
    // POST /api/password-reset/request
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("POST /api/password-reset/request")
    class RequestReset {

        @Test
        @DisplayName("200 - email existente: código enviado exitosamente")
        void shouldReturn200WhenEmailExists() throws Exception {
            // Arrange
            PasswordResetRequestDTO req = new PasswordResetRequestDTO("usuario@mail.com");
            doNothing().when(passwordResetService).requestReset(any(PasswordResetRequestDTO.class));

            // Act & Assert
            mockMvc.perform(post("/api/password-reset/request")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isOk())
                    .andExpect(content().string("If the email exists, a recovery code has been sent."));

            verify(passwordResetService).requestReset(any(PasswordResetRequestDTO.class));
        }

        @Test
        @DisplayName("200 - email no existente: respuesta idéntica por seguridad (no revela si existe)")
        void shouldReturn200EvenWhenEmailNotFound() throws Exception {
            // Arrange — por diseño de seguridad, UserNotFoundException no debe exponer si el email existe
            PasswordResetRequestDTO req = new PasswordResetRequestDTO("noexiste@mail.com");

            mockMvc.perform(post("/api/password-reset/request")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("400 - RecoveryTokenException al generar el token")
        void shouldReturn400WhenRecoveryTokenFails() throws Exception {
            // Arrange
            PasswordResetRequestDTO req = new PasswordResetRequestDTO("usuario@mail.com");
            doThrow(new RecoveryTokenException("Error al generar token de recuperación"))
                    .when(passwordResetService).requestReset(any(PasswordResetRequestDTO.class));

            // Act & Assert
            mockMvc.perform(post("/api/password-reset/request")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isBadRequest());
        }

        @ParameterizedTest
        @MethodSource("com.sparktech.motorx.controller.PasswordResetControllerTest#invalidEmails")
        @DisplayName("400 - validaciones de email fallan para distintos inputs inválidos")
        void shouldReturn400ForInvalidEmails(String email) throws Exception {
            PasswordResetRequestDTO req = new PasswordResetRequestDTO(email);

            mockMvc.perform(post("/api/password-reset/request")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(passwordResetService);
        }

        @Test
        @DisplayName("400 - body vacío falla la validación")
        void shouldReturn400WhenBodyEmpty() throws Exception {
            mockMvc.perform(post("/api/password-reset/request")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(passwordResetService);
        }
    }

    // ---------------------------------------------------------------
    // PUT /api/password-reset
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("PUT /api/password-reset")
    class ResetPassword {

        // Contraseña válida: mínimo 8 chars, al menos 1 mayúscula, 1 dígito, 1 símbolo
        private static final String VALID_PASSWORD = "Secure1@";

        @Test
        @DisplayName("200 - contraseña restablecida exitosamente")
        void shouldResetPasswordSuccessfully() throws Exception {
            // Arrange
            PasswordResetDTO req = new PasswordResetDTO("valid-token-abc123", VALID_PASSWORD);
            doNothing().when(passwordResetService).resetPassword(any(PasswordResetDTO.class));

            // Act & Assert
            mockMvc.perform(put("/api/password-reset")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Password has been successfully reset."));

            verify(passwordResetService).resetPassword(any(PasswordResetDTO.class));
        }

        @Test
        @DisplayName("400 - RecoveryTokenException: token inválido")
        void shouldReturn400WhenTokenInvalid() throws Exception {
            // Arrange
            PasswordResetDTO req = new PasswordResetDTO("invalid-token", VALID_PASSWORD);
            doThrow(new RecoveryTokenException("Token inválido"))
                    .when(passwordResetService).resetPassword(any(PasswordResetDTO.class));

            // Act & Assert
            mockMvc.perform(put("/api/password-reset")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("400 - RecoveryTokenException: token expirado")
        void shouldReturn400WhenTokenExpired() throws Exception {
            // Arrange
            PasswordResetDTO req = new PasswordResetDTO("expired-token", VALID_PASSWORD);
            doThrow(new RecoveryTokenException("Token expirado"))
                    .when(passwordResetService).resetPassword(any(PasswordResetDTO.class));

            // Act & Assert
            mockMvc.perform(put("/api/password-reset")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("400 - @NotBlank falla con token nulo")
        void shouldReturn400WhenTokenNull() throws Exception {
            // Arrange — token es @NotBlank
            PasswordResetDTO req = new PasswordResetDTO(null, VALID_PASSWORD);

            // Act & Assert
            mockMvc.perform(put("/api/password-reset")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(passwordResetService);
        }

        @Test
        @DisplayName("400 - @NotBlank falla con token en blanco")
        void shouldReturn400WhenTokenBlank() throws Exception {
            // Arrange — "   " no pasa @NotBlank
            PasswordResetDTO req = new PasswordResetDTO("   ", VALID_PASSWORD);

            // Act & Assert
            mockMvc.perform(put("/api/password-reset")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(passwordResetService);
        }

        @ParameterizedTest
        @MethodSource("com.sparktech.motorx.controller.PasswordResetControllerTest#invalidNewPasswords")
        @DisplayName("400 - validaciones de newPassword fallan para distintos inputs inválidos")
        void shouldReturn400ForInvalidNewPasswords(String newPassword) throws Exception {
            PasswordResetDTO req = new PasswordResetDTO("valid-token-abc123", newPassword);

            mockMvc.perform(put("/api/password-reset")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(passwordResetService);
        }

        @Test
        @DisplayName("400 - body vacío falla ambas validaciones")
        void shouldReturn400WhenBodyEmpty() throws Exception {
            mockMvc.perform(put("/api/password-reset")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(passwordResetService);
        }
    }

    // ---------------------------------------------------------------
    // TestConfiguration
    // ---------------------------------------------------------------

    @TestConfiguration
    static class TestConfig {

        @Bean
        @Primary
        IPasswordResetService passwordResetService() {
            return mock(IPasswordResetService.class);
        }

        @Bean
        @Primary
        JwtService jwtService() {
            return mock(JwtService.class);
        }

        @Bean
        @Primary
        JwtAuthenticationFilter jwtAuthenticationFilter() {
            return mock(JwtAuthenticationFilter.class);
        }

        @Bean
        @Primary
        CustomUserDetailsService customUserDetailsService() {
            return mock(CustomUserDetailsService.class);
        }
    }
}
