package com.sparktech.motorx.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sparktech.motorx.Services.IAuthService;
import com.sparktech.motorx.controller.error.GlobalControllerAdvice;
import com.sparktech.motorx.dto.auth.AuthResponseDTO;
import com.sparktech.motorx.dto.auth.LoginRequestDTO;
import com.sparktech.motorx.dto.auth.RegisterUserDTO;
import com.sparktech.motorx.dto.auth.Verify2FADTO;
import com.sparktech.motorx.dto.user.UserDTO;
import com.sparktech.motorx.entity.Role;
import com.sparktech.motorx.exception.InvalidPasswordException;
import com.sparktech.motorx.exception.UserNotFoundException;
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
import org.junit.jupiter.params.provider.Arguments;
import java.util.stream.Stream;
import java.time.LocalDateTime;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalControllerAdvice.class, AuthControllerTest.TestConfig.class})
@DisplayName("AuthController - Tests")
class AuthControllerTest {

    @Resource
    private MockMvc mockMvc;

    @Resource
    private IAuthService authService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        reset(authService);
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    // ---------------------------------------------------------------
    // Fixtures
    // ---------------------------------------------------------------

    private AuthResponseDTO buildAuthResponse(Role role) {
        return new AuthResponseDTO(
                "eyJhbGciOiJIUzI1NiJ9.token",
                1L,
                "usuario@mail.com",
                "Usuario Test",
                role
        );
    }

    private String json(Object o) throws Exception {
        return objectMapper.writeValueAsString(o);
    }

    // Providers for parameterized tests
    static Stream<Arguments> invalidLoginRequests() {
        return Stream.of(
                Arguments.of(null, "Pass123!"),           // email null -> @NotBlank
                Arguments.of("no-es-email", "Pass123!"),// malformed email -> @Email
                Arguments.of("user@mail.com", null),      // password null -> @NotBlank
                Arguments.of("user@mail.com", "   ")    // password blank -> @NotBlank
        );
    }

    static Stream<Arguments> invalidVerify2FARequests() {
        return Stream.of(
                Arguments.of("cliente@mail.com", "ABCDEF"),
                Arguments.of("cliente@mail.com", "12345"),
                Arguments.of("cliente@mail.com", "1234567"),
                Arguments.of(null, "123456"),
                Arguments.of("cliente@mail.com", null)
        );
    }

    // ---------------------------------------------------------------
    // POST /api/auth/login
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("POST /api/auth/login")
    class Login {

        @Test
        @DisplayName("200 - ADMIN recibe token directamente")
        void shouldReturnTokenDirectlyForAdmin() throws Exception {
            // Arrange
            LoginRequestDTO req = new LoginRequestDTO("admin@mail.com", "Admin123!");
            AuthResponseDTO response = buildAuthResponse(Role.ADMIN);
            when(authService.login(any(LoginRequestDTO.class))).thenReturn(response);

            // Act & Assert
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token", notNullValue()))
                    .andExpect(jsonPath("$.type", is("Bearer")))
                    .andExpect(jsonPath("$.role", is("ADMIN")))
                    .andExpect(jsonPath("$.email", is("usuario@mail.com")));

            verify(authService).login(any(LoginRequestDTO.class));
        }

        @Test
        @DisplayName("200 - CLIENT recibe respuesta de 2FA enviado")
        void shouldReturn2FAResponseForClient() throws Exception {
            // Arrange
            LoginRequestDTO req = new LoginRequestDTO("cliente@mail.com", "Pass123!");
            when(authService.login(any(LoginRequestDTO.class)))
                    .thenReturn("Código 2FA enviado a su email");

            // Act & Assert
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isOk());

            verify(authService).login(any(LoginRequestDTO.class));
        }

        @Test
        @DisplayName("401 - credenciales inválidas lanza InvalidPasswordException")
        void shouldReturn401WhenInvalidCredentials() throws Exception {
            // Arrange
            LoginRequestDTO req = new LoginRequestDTO("user@mail.com", "wrongpass");
            when(authService.login(any(LoginRequestDTO.class)))
                    .thenThrow(new InvalidPasswordException("Credenciales inválidas"));

            // Act & Assert
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isUnauthorized());
        }

        @ParameterizedTest
        @MethodSource("com.sparktech.motorx.controller.AuthControllerTest#invalidLoginRequests")
        @DisplayName("400 - validaciones de LoginRequestDTO fallan para varios inputs inválidos")
        void shouldReturn400ForInvalidLoginRequests(String email, String password) throws Exception {
            LoginRequestDTO req = new LoginRequestDTO(email, password);

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(authService);
        }

        @Test
        @DisplayName("400 - body vacío falla ambas validaciones")
        void shouldReturn400WhenBodyEmpty() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(authService);
        }
    }

    // ---------------------------------------------------------------
    // POST /api/auth/verify-2fa
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("POST /api/auth/verify-2fa")
    class Verify2FA {

        @Test
        @DisplayName("200 - código de 6 dígitos válido retorna token JWT")
        void shouldReturnTokenWhenCodeValid() throws Exception {
            // Arrange
            Verify2FADTO req = new Verify2FADTO("cliente@mail.com", "123456");
            AuthResponseDTO response = buildAuthResponse(Role.CLIENT);
            when(authService.verify2FA("cliente@mail.com", "123456")).thenReturn(response);

            // Act & Assert
            mockMvc.perform(post("/api/auth/verify-2fa")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token", notNullValue()))
                    .andExpect(jsonPath("$.type", is("Bearer")))
                    .andExpect(jsonPath("$.role", is("CLIENT")));

            verify(authService).verify2FA("cliente@mail.com", "123456");
        }

        @Test
        @DisplayName("401 - código con formato válido pero incorrecto según negocio")
        void shouldReturn401WhenCodeIncorrectPerBusiness() throws Exception {
            // Arrange — "654321" pasa @Pattern (6 dígitos) pero el service lo rechaza
            Verify2FADTO req = new Verify2FADTO("cliente@mail.com", "654321");
            when(authService.verify2FA("cliente@mail.com", "654321"))
                    .thenThrow(new InvalidPasswordException("Código incorrecto"));

            // Act & Assert
            mockMvc.perform(post("/api/auth/verify-2fa")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isUnauthorized());
        }

        @ParameterizedTest
        @MethodSource("com.sparktech.motorx.controller.AuthControllerTest#invalidVerify2FARequests")
        @DisplayName("400 - validaciones de Verify2FADTO fallan para varios inputs inválidos")
        void shouldReturn400ForInvalidVerify2FARequests(String email, String code) throws Exception {
            Verify2FADTO req = new Verify2FADTO(email, code);

            mockMvc.perform(post("/api/auth/verify-2fa")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(authService);
        }

        @Test
        @DisplayName("400 - body vacío falla ambas validaciones")
        void shouldReturn400WhenBodyEmpty() throws Exception {
            mockMvc.perform(post("/api/auth/verify-2fa")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(authService);
        }
    }

    // ---------------------------------------------------------------
    // POST /api/auth/register
    // RegisterUserDTO(name, dni, email, password, phone)
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("POST /api/auth/register")
    class Register {

        private RegisterUserDTO buildValidRegisterRequest() {
            return new RegisterUserDTO(
                    "María García",   // @NotBlank @Size(max=150)
                    "87654321",       // @NotBlank @Size(max=30)
                    "maria.garcia@mail.com", // @NotBlank @Email @Size(max=150)
                    "SecurePass123!", // @NotBlank @Size(min=6, max=100)
                    "+57-555-9876"   // @NotBlank @Pattern @Size(max=20)
            );
        }

        @Test
        @DisplayName("200 - usuario registrado y token retornado")
        void shouldRegisterUserAndReturnToken() throws Exception {
            // Arrange
            AuthResponseDTO response = buildAuthResponse(Role.CLIENT);
            when(authService.register(any(RegisterUserDTO.class))).thenReturn(response);

            // Act & Assert
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(buildValidRegisterRequest())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token", notNullValue()))
                    .andExpect(jsonPath("$.type", is("Bearer")))
                    .andExpect(jsonPath("$.role", is("CLIENT")));

            verify(authService).register(any(RegisterUserDTO.class));
        }

        @Test
        @DisplayName("400 - email ya registrado lanza excepción de negocio")
        void shouldReturn400WhenEmailAlreadyExists() throws Exception {
            when(authService.register(any(RegisterUserDTO.class)))
                    .thenThrow(new IllegalArgumentException("El email ya está registrado"));

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(buildValidRegisterRequest())))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("400 - DNI ya registrado lanza excepción de negocio")
        void shouldReturn400WhenDniAlreadyExists() throws Exception {
            when(authService.register(any(RegisterUserDTO.class)))
                    .thenThrow(new IllegalArgumentException("El DNI ya está registrado"));

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(buildValidRegisterRequest())))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("400 - @NotBlank falla con name nulo")
        void shouldReturn400WhenNameNull() throws Exception {
            RegisterUserDTO req = new RegisterUserDTO(
                    null, "87654321", "maria@mail.com", "Pass123!", "+57-555-9876"
            );

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(authService);
        }

        @Test
        @DisplayName("400 - @NotBlank falla con dni nulo")
        void shouldReturn400WhenDniNull() throws Exception {
            RegisterUserDTO req = new RegisterUserDTO(
                    "María García", null, "maria@mail.com", "Pass123!", "+57-555-9876"
            );

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(authService);
        }

        @Test
        @DisplayName("400 - @Email falla con email mal formado")
        void shouldReturn400WhenEmailMalformed() throws Exception {
            // Arrange — "no-es-email" no pasa @Email
            RegisterUserDTO req = new RegisterUserDTO(
                    "María García", "87654321", "no-es-email", "Pass123!", "+57-555-9876"
            );

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(authService);
        }

        @Test
        @DisplayName("400 - @Size(min=6) falla con password demasiado corta")
        void shouldReturn400WhenPasswordTooShort() throws Exception {
            // Arrange — "abc" tiene 3 chars, no pasa @Size(min=6)
            RegisterUserDTO req = new RegisterUserDTO(
                    "María García", "87654321", "maria@mail.com", "abc", "+57-555-9876"
            );

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(authService);
        }

        @Test
        @DisplayName("400 - @NotBlank falla con password nula")
        void shouldReturn400WhenPasswordNull() throws Exception {
            RegisterUserDTO req = new RegisterUserDTO(
                    "María García", "87654321", "maria@mail.com", null, "+57-555-9876"
            );

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(authService);
        }

        @Test
        @DisplayName("400 - @Pattern falla con teléfono con formato inválido (letras)")
        void shouldReturn400WhenPhoneInvalid() throws Exception {
            // Arrange — "abc" no pasa @Pattern(regexp = "^[0-9+()\\-\\s]{7,20}$")
            RegisterUserDTO req = new RegisterUserDTO(
                    "María García", "87654321", "maria@mail.com", "Pass123!", "abc"
            );

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(authService);
        }

        @Test
        @DisplayName("400 - @Pattern falla con teléfono demasiado corto (menos de 7 chars)")
        void shouldReturn400WhenPhoneTooShort() throws Exception {
            // Arrange — "555" tiene 3 chars, no pasa el mínimo de 7 del @Pattern
            RegisterUserDTO req = new RegisterUserDTO(
                    "María García", "87654321", "maria@mail.com", "Pass123!", "555"
            );

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(authService);
        }

        @Test
        @DisplayName("400 - body vacío falla todas las validaciones")
        void shouldReturn400WhenBodyEmpty() throws Exception {
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(authService);
        }
    }

    // ---------------------------------------------------------------
    // GET /api/auth/me
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("GET /api/auth/me")
    class GetCurrentUser {

        @Test
        @WithMockUser
        @DisplayName("200 - retorna datos del usuario autenticado")
        void shouldReturnCurrentUser() throws Exception {
            // Arrange
            UserDTO userDTO = new UserDTO(
                    1L,
                    "María García",
                    "87654321",
                    "maria@mail.com",
                    "secret-password",
                    "555-9876",
                    LocalDateTime.now(),
                    Role.CLIENT,
                    true,
                    false,
                    LocalDateTime.now()
            );
            when(authService.getCurrentUser()).thenReturn(userDTO);

            // Act & Assert
            mockMvc.perform(get("/api/auth/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.name", is("María García")))
                    .andExpect(jsonPath("$.email", is("maria@mail.com")))
                    .andExpect(jsonPath("$.role", is("CLIENT")));

            verify(authService).getCurrentUser();
        }

        @Test
        @WithMockUser
        @DisplayName("404 - usuario no encontrado en base de datos")
        void shouldReturn404WhenUserNotFound() throws Exception {
            // Arrange
            when(authService.getCurrentUser())
                    .thenThrow(new UserNotFoundException("Usuario no encontrado"));

            // Act & Assert
            mockMvc.perform(get("/api/auth/me"))
                    .andExpect(status().isNotFound());
        }
    }

    // ---------------------------------------------------------------
    // GET /api/auth/logout
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("GET /api/auth/logout")
    class Logout {

        @Test
        @WithMockUser
        @DisplayName("200 - logout exitoso retorna mensaje")
        void shouldLogoutSuccessfully() throws Exception {
            // Arrange
            doNothing().when(authService).logout();

            // Act & Assert
            mockMvc.perform(get("/api/auth/logout"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Logout exitoso"));

            verify(authService).logout();
        }
    }

    // ---------------------------------------------------------------
    // POST /api/auth/refresh
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("POST /api/auth/refresh")
    class RefreshToken {

        @Test
        @DisplayName("200 - token renovado exitosamente")
        void shouldRefreshTokenSuccessfully() throws Exception {
            // Arrange
            String refreshToken = "valid-refresh-token";
            AuthResponseDTO response = buildAuthResponse(Role.CLIENT);
            when(authService.refreshToken(refreshToken)).thenReturn(response);

            // Act & Assert
            mockMvc.perform(post("/api/auth/refresh")
                            .param("refreshToken", refreshToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token", notNullValue()))
                    .andExpect(jsonPath("$.type", is("Bearer")));

            verify(authService).refreshToken(refreshToken);
        }

        @Test
        @DisplayName("401 - refresh token inválido o expirado")
        void shouldReturn401WhenRefreshTokenInvalid() throws Exception {
            // Arrange
            when(authService.refreshToken("expired-token"))
                    .thenThrow(new InvalidPasswordException("Refresh token inválido o expirado"));

            // Act & Assert
            mockMvc.perform(post("/api/auth/refresh")
                            .param("refreshToken", "expired-token"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("400 - param refreshToken faltante")
        void shouldReturn400WhenRefreshTokenMissing() throws Exception {
            mockMvc.perform(post("/api/auth/refresh"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(authService);
        }
    }

    // ---------------------------------------------------------------
    // TestConfiguration
    // ---------------------------------------------------------------

    @TestConfiguration
    static class TestConfig {

        @Bean
        @Primary
        IAuthService authService() {
            return mock(IAuthService.class);
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
