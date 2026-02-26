package com.sparktech.motorx.Services.impl;

import com.sparktech.motorx.Services.ICurrentUserService;
import com.sparktech.motorx.Services.IUserService;
import com.sparktech.motorx.Services.IVerificationCodeCacheService;
import com.sparktech.motorx.Services.IVerificationCodeService;
import com.sparktech.motorx.dto.auth.AuthResponseDTO;
import com.sparktech.motorx.dto.auth.LoginRequestDTO;
import com.sparktech.motorx.dto.auth.RegisterUserDTO;
import com.sparktech.motorx.dto.user.UserDTO;
import com.sparktech.motorx.entity.Role;
import com.sparktech.motorx.entity.UserEntity;
import com.sparktech.motorx.exception.BlockedAccountException;
import com.sparktech.motorx.exception.InvalidPasswordException;
import com.sparktech.motorx.mapper.UserEntityMapper;
import com.sparktech.motorx.security.CustomUserDetailsService;
import com.sparktech.motorx.security.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl - Unit Tests")
class AuthServiceImplTest {

    // ================================================================
    // MOCKS
    // ================================================================
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtService jwtService;
    @Mock private IUserService userService;
    @Mock private ICurrentUserService currentUserService;
    @Mock private CustomUserDetailsService userDetailsService;
    @Mock private UserEntityMapper userMapper;
    @Mock private IVerificationCodeService verificationCodeService;
    @Mock private IVerificationCodeCacheService cacheService;

    @InjectMocks
    private AuthServiceImpl sut;

    // ================================================================
    // BUILDERS
    // ================================================================

    private UserEntity buildUser(Long id, String email, Role role,
                                 boolean locked, boolean enabled) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setEmail(email);
        user.setName("Usuario Test");
        user.setRole(role);
        user.setAccountLocked(locked);
        user.setEnabled(enabled);
        return user;
    }

    /**
     * Configura el "happy path" de authenticateAndBuildAuthResult:
     * authenticationManager → Authentication → UserDetails → UserEntity → token
     */
    private void stubAuthentication(String email,
                                    UserEntity user, String token) {
        Authentication auth = mock(Authentication.class);
        UserDetails userDetails = mock(UserDetails.class);

        when(authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, "pass123")))
                .thenReturn(auth);
        when(auth.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn(email);
        when(userDetailsService.getUserEntityByEmail(email)).thenReturn(user);
        when(jwtService.generateToken(userDetails)).thenReturn(token);
    }

    // ================================================================
    // login()
    // ================================================================

    @Nested
    @DisplayName("login()")
    class LoginTests {

        private final LoginRequestDTO request =
                new LoginRequestDTO("user@test.com", "pass123");

        @Test
        @DisplayName("Admin activo: retorna AuthResponseDTO directamente sin 2FA")
        void givenAdminUser_thenReturnTokenDirectly() throws InvalidPasswordException {
            // Arrange
            UserEntity admin = buildUser(1L, "user@test.com", Role.ADMIN, false, true);
            stubAuthentication("user@test.com", admin, "jwt-token");

            // Act
            Object result = sut.login(request);

            // Assert
            assertThat(result).isInstanceOf(AuthResponseDTO.class);
            AuthResponseDTO dto = (AuthResponseDTO) result;
            assertThat(dto.token()).isEqualTo("jwt-token");
            assertThat(dto.email()).isEqualTo("user@test.com");
            assertThat(dto.role()).isEqualTo(Role.ADMIN);

            // 2FA NO debe invocarse para admin
            verifyNoInteractions(verificationCodeService);
        }

        @Test
        @DisplayName("Usuario CLIENT activo: envía código 2FA y retorna mensaje String")
        void givenClientUser_thenSend2FAAndReturnMessage() throws InvalidPasswordException {
            // Arrange
            UserEntity client = buildUser(2L, "user@test.com", Role.CLIENT, false, true);
            stubAuthentication("user@test.com", client, "jwt-token");

            // Act
            Object result = sut.login(request);

            // Assert
            assertThat(result).isInstanceOf(String.class);
            assertThat((String) result).contains("Código de verificación");

            verify(verificationCodeService, times(1))
                    .generateAndSendVerificationCode(client);
        }

        @Test
        @DisplayName("Cuenta bloqueada (accountLocked=true): lanza BlockedAccountException")
        void givenLockedAccount_thenThrowBlockedAccountException() {
            // Arrange
            UserEntity locked = buildUser(3L, "user@test.com", Role.CLIENT, true, true);
            stubAuthentication("user@test.com", locked, "jwt-token");

            // Act + Assert
            assertThatThrownBy(() -> sut.login(request))
                    .isInstanceOf(BlockedAccountException.class);

            verifyNoInteractions(verificationCodeService);
        }

        @Test
        @DisplayName("Cuenta deshabilitada (enabled=false): lanza BlockedAccountException")
        void givenDisabledAccount_thenThrowBlockedAccountException() {
            // Arrange
            UserEntity disabled = buildUser(4L, "user@test.com", Role.CLIENT, false, false);
            stubAuthentication("user@test.com", disabled, "jwt-token");

            // Act + Assert
            assertThatThrownBy(() -> sut.login(request))
                    .isInstanceOf(BlockedAccountException.class);
        }

        @Test
        @DisplayName("Credenciales incorrectas: lanza InvalidPasswordException")
        void givenBadCredentials_thenThrowInvalidPasswordException() {
            // Arrange
            when(authenticationManager.authenticate(any()))
                    .thenThrow(new BadCredentialsException("bad creds"));

            // Act + Assert
            assertThatThrownBy(() -> sut.login(request))
                    .isInstanceOf(InvalidPasswordException.class)
                    .hasMessageContaining("Credenciales inválidas");

            verifyNoInteractions(verificationCodeService, userDetailsService);
        }

        @Test
        @DisplayName("Admin bloqueado: lanza BlockedAccountException (bloqueo tiene prioridad sobre rol)")
        void givenLockedAdmin_thenThrowBlockedAccountException() {
            // Arrange
            UserEntity lockedAdmin = buildUser(5L, "user@test.com", Role.ADMIN, true, true);
            stubAuthentication("user@test.com", lockedAdmin, "jwt-token");

            // Act + Assert
            assertThatThrownBy(() -> sut.login(request))
                    .isInstanceOf(BlockedAccountException.class);
        }
    }

    // ================================================================
    // register()
    // ================================================================

    @Nested
    @DisplayName("register()")
    class RegisterTests {

        private final RegisterUserDTO request = new RegisterUserDTO(
                "Nuevo Usuario", "123456789", "nuevo@test.com",
                "pass123", "3001234567"
        );

        @Test
        @DisplayName("Registro exitoso: llama a userService, autentica y retorna AuthResponseDTO")
        void givenValidRequest_thenRegisterAuthenticateAndReturnDTO() {
            // Arrange
            UserEntity user = buildUser(10L, "nuevo@test.com", Role.CLIENT, false, true);
            stubAuthentication("nuevo@test.com", user, "new-token");

            // Act
            AuthResponseDTO result = sut.register(request);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.token()).isEqualTo("new-token");
            assertThat(result.email()).isEqualTo("nuevo@test.com");
            assertThat(result.userId()).isEqualTo(10L);

            // userService.register() debe haberse llamado exactamente una vez
            verify(userService, times(1)).register(request);
        }

        @Test
        @DisplayName("El DTO retornado contiene nombre y rol del usuario registrado")
        void givenValidRequest_thenDTOContainsNameAndRole() {
            // Arrange
            UserEntity user = buildUser(10L, "nuevo@test.com", Role.CLIENT, false, true);
            stubAuthentication("nuevo@test.com", user, "token");

            // Act
            AuthResponseDTO result = sut.register(request);

            // Assert
            assertThat(result.name()).isEqualTo("Usuario Test");
            assertThat(result.role()).isEqualTo(Role.CLIENT);
        }

        @Test
        @DisplayName("Si userService.register() lanza excepción, no se autentica")
        void givenUserServiceThrows_thenExceptionPropagates() {
            // Arrange
            doThrow(new RuntimeException("Email ya registrado"))
                    .when(userService).register(any());

            // Act + Assert
            assertThatThrownBy(() -> sut.register(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Email ya registrado");

            // authenticationManager nunca debe llamarse si el registro falla
            verifyNoInteractions(authenticationManager);
        }
    }

    // ================================================================
    // getCurrentUser()
    // ================================================================

    @Nested
    @DisplayName("getCurrentUser()")
    class GetCurrentUserTests {

        @Test
        @DisplayName("Retorna UserDTO del usuario autenticado correctamente")
        void givenAuthenticatedUser_thenReturnUserDTO() {
            // Arrange
            UserEntity user = buildUser(1L, "auth@test.com", Role.CLIENT, false, true);
            UserDTO expectedDTO = mock(UserDTO.class);
            when(currentUserService.getAuthenticatedUser()).thenReturn(user);
            when(userMapper.toUserDTO(user)).thenReturn(expectedDTO);

            // Act
            UserDTO result = sut.getCurrentUser();

            // Assert
            assertThat(result).isEqualTo(expectedDTO);
            verify(currentUserService, times(1)).getAuthenticatedUser();
            verify(userMapper, times(1)).toUserDTO(user);
        }
    }

    // ================================================================
    // logout()
    // ================================================================

    @Nested
    @DisplayName("logout()")
    class LogoutTests {

        @Test
        @DisplayName("logout() limpia el SecurityContext sin lanzar excepción")
        void givenAuthenticatedSession_thenClearSecurityContext() {
            // Act + Assert
            assertThatCode(() -> sut.logout())
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("SecurityContextHolder queda limpio tras logout()")
        void givenAuthenticatedSession_thenSecurityContextIsNull() {
            // Arrange — simular contexto con autenticación previa
            SecurityContextHolder.getContext().setAuthentication(
                    mock(Authentication.class)
            );

            // Act
            sut.logout();

            // Assert
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }
    }

    // ================================================================
    // refreshToken()
    // ================================================================

    @Nested
    @DisplayName("refreshToken()")
    class RefreshTokenTests {

        @Test
        @DisplayName("Token válido: retorna nuevo AuthResponseDTO con nuevo token")
        void givenValidRefreshToken_thenReturnNewToken() throws InvalidPasswordException {
            // Arrange
            String refreshToken = "valid-refresh-token";
            String newToken = "new-jwt-token";
            String email = "user@test.com";
            UserEntity user = buildUser(1L, email, Role.CLIENT, false, true);
            UserDetails userDetails = mock(UserDetails.class);

            when(jwtService.extractUsername(refreshToken)).thenReturn(email);
            when(userDetailsService.loadUserByUsername(email)).thenReturn(userDetails);
            when(jwtService.isTokenValid(refreshToken, userDetails)).thenReturn(true);
            when(jwtService.generateToken(userDetails)).thenReturn(newToken);
            when(userDetailsService.getUserEntityByEmail(email)).thenReturn(user);

            // Act
            AuthResponseDTO result = sut.refreshToken(refreshToken);

            // Assert
            assertThat(result.token()).isEqualTo(newToken);
            assertThat(result.email()).isEqualTo(email);
            assertThat(result.userId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Email null extraído del token: lanza InvalidPasswordException")
        void givenTokenWithNullEmail_thenThrowInvalidPasswordException() {
            // Arrange
            when(jwtService.extractUsername(anyString())).thenReturn(null);

            // Act + Assert
            assertThatThrownBy(() -> sut.refreshToken("bad-token"))
                    .isInstanceOf(InvalidPasswordException.class)
                    .hasMessageContaining("inválido");
        }

        @Test
        @DisplayName("Token no válido según JwtService: lanza InvalidPasswordException")
        void givenInvalidToken_thenThrowInvalidPasswordException() {
            // Arrange
            String token = "expired-token";
            UserDetails userDetails = mock(UserDetails.class);

            when(jwtService.extractUsername(token)).thenReturn("user@test.com");
            when(userDetailsService.loadUserByUsername("user@test.com"))
                    .thenReturn(userDetails);
            when(jwtService.isTokenValid(token, userDetails)).thenReturn(false);

            // Act + Assert
            assertThatThrownBy(() -> sut.refreshToken(token))
                    .isInstanceOf(InvalidPasswordException.class);
        }

        @Test
        @DisplayName("Excepción inesperada durante refresh: relanza como InvalidPasswordException")
        void givenUnexpectedException_thenWrapInInvalidPasswordException() {
            // Arrange
            when(jwtService.extractUsername(anyString()))
                    .thenThrow(new RuntimeException("Error interno"));

            // Act + Assert
            assertThatThrownBy(() -> sut.refreshToken("any-token"))
                    .isInstanceOf(InvalidPasswordException.class)
                    .hasMessageContaining("renovación inválido");
        }
    }

    // ================================================================
    // verify2FA()
    // ================================================================

    @Nested
    @DisplayName("verify2FA()")
    class Verify2FATests {

        private static final String EMAIL = "user@test.com";
        private static final String VALID_CODE = "123456";

        @Test
        @DisplayName("Código válido: retorna AuthResponseDTO con token generado")
        void givenValidCode_thenReturnAuthResponseDTO() throws InvalidPasswordException {
            // Arrange
            UserEntity user = buildUser(1L, EMAIL, Role.CLIENT, false, true);
            UserDetails userDetails = mock(UserDetails.class);
            String token = "2fa-token";

            when(cacheService.validateCode(EMAIL, VALID_CODE)).thenReturn(true);
            when(userDetailsService.getUserEntityByEmail(EMAIL)).thenReturn(user);
            when(userDetailsService.loadUserByUsername(EMAIL)).thenReturn(userDetails);
            when(jwtService.generateToken(userDetails)).thenReturn(token);

            // Act
            AuthResponseDTO result = sut.verify2FA(EMAIL, VALID_CODE);

            // Assert
            assertThat(result.token()).isEqualTo(token);
            assertThat(result.email()).isEqualTo(EMAIL);
            assertThat(result.userId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("El DTO de 2FA contiene nombre y rol del usuario")
        void givenValidCode_thenDTOContainsUserData() throws InvalidPasswordException {
            // Arrange
            UserEntity user = buildUser(1L, EMAIL, Role.CLIENT, false, true);
            UserDetails userDetails = mock(UserDetails.class);

            when(cacheService.validateCode(EMAIL, VALID_CODE)).thenReturn(true);
            when(userDetailsService.getUserEntityByEmail(EMAIL)).thenReturn(user);
            when(userDetailsService.loadUserByUsername(EMAIL)).thenReturn(userDetails);
            when(jwtService.generateToken(userDetails)).thenReturn("token");

            // Act
            AuthResponseDTO result = sut.verify2FA(EMAIL, VALID_CODE);

            // Assert
            assertThat(result.name()).isEqualTo("Usuario Test");
            assertThat(result.role()).isEqualTo(Role.CLIENT);
        }

        @Test
        @DisplayName("Código inválido o expirado: lanza InvalidPasswordException")
        void givenInvalidCode_thenThrowInvalidPasswordException() {
            // Arrange
            when(cacheService.validateCode(EMAIL, "000000")).thenReturn(false);

            // Act + Assert
            assertThatThrownBy(() -> sut.verify2FA(EMAIL, "000000"))
                    .isInstanceOf(InvalidPasswordException.class)
                    .hasMessageContaining("inválido o expirado");

            // No debe intentar generar token ni buscar usuario
            verifyNoInteractions(jwtService, userDetailsService);
        }
    }
}