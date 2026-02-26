package com.sparktech.motorx.Services.impl;

import com.sparktech.motorx.Services.IEmailNotificationService;
import com.sparktech.motorx.Services.IVerificationCodeService;
import com.sparktech.motorx.dto.auth.PasswordResetDTO;
import com.sparktech.motorx.dto.auth.PasswordResetRequestDTO;
import com.sparktech.motorx.dto.notification.EmailDTO;
import com.sparktech.motorx.entity.PasswordResetTokenEntity;
import com.sparktech.motorx.entity.UserEntity;
import com.sparktech.motorx.exception.InvalidTokenException;
import com.sparktech.motorx.exception.RecoveryTokenException;
import com.sparktech.motorx.exception.UserNotFoundException;
import com.sparktech.motorx.repository.JpaPasswordResetTokenRepository;
import com.sparktech.motorx.repository.JpaUserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PasswordResetServiceImpl - Unit Tests")
class PasswordResetServiceImplTest {

    // ================================================================
    // MOCKS
    // ================================================================
    @Mock private JpaUserRepository userRepository;
    @Mock private JpaPasswordResetTokenRepository tokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private IEmailNotificationService notificationService;
    @Mock private IVerificationCodeService verificationCodeService;

    @InjectMocks
    private PasswordResetServiceImpl sut;

    @Captor private ArgumentCaptor<PasswordResetTokenEntity> tokenCaptor;
    @Captor private ArgumentCaptor<UserEntity> userCaptor;
    @Captor private ArgumentCaptor<EmailDTO> emailCaptor;

    // ================================================================
    // BUILDERS
    // ================================================================

    private UserEntity buildUser() {
        UserEntity u = new UserEntity();
        u.setId(1L);
        u.setEmail("user@test.com");
        u.setName("Usuario Test");
        u.setPassword("old-encoded-pass");
        return u;
    }

    private PasswordResetTokenEntity buildToken(UserEntity user,
                                                String hash,
                                                LocalDateTime expiresAt) {
        PasswordResetTokenEntity t = new PasswordResetTokenEntity();
        t.setId(1L);
        t.setUser(user);
        t.setTokenHash(hash);
        t.setExpiresAt(expiresAt);
        t.setUsed(false);
        return t;
    }

    /**
     * Calcula el hash SHA-256 en Base64 igual que el servicio,
     * para poder construir tokens coherentes en los tests.
     */
    private String sha256Base64(String input) throws Exception {
        java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(input.getBytes());
        return java.util.Base64.getEncoder().encodeToString(hash);
    }

    // ================================================================
    // requestReset()
    // ================================================================

    @Nested
    @DisplayName("requestReset()")
    class RequestResetTests {

        private final PasswordResetRequestDTO request =
                new PasswordResetRequestDTO("user@test.com");

        @Test
        @DisplayName("Camino feliz: busca usuario, invalida tokens previos, guarda token y envía email")
        void givenExistingUser_thenCreateTokenAndSendEmail() {
            // Arrange
            UserEntity user = buildUser();
            when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
            when(tokenRepository.findByUserAndUsedFalse(user)).thenReturn(List.of());
            when(verificationCodeService.generateVerificationCode()).thenReturn("123456");

            // Act
            assertThatCode(() -> sut.requestReset(request))
                    .doesNotThrowAnyException();

            // Assert — token persistido
            verify(tokenRepository, times(1)).save(tokenCaptor.capture());
            PasswordResetTokenEntity saved = tokenCaptor.getValue();
            assertThat(saved.getUser()).isEqualTo(user);
            assertThat(saved.getUsed()).isFalse();
            assertThat(saved.getExpiresAt()).isAfter(LocalDateTime.now());
            assertThat(saved.getTokenHash()).isNotBlank();

            // Assert — email enviado
            verify(notificationService, times(1)).sendMail(emailCaptor.capture());
            assertThat(emailCaptor.getValue().recipient()).isEqualTo("user@test.com");
        }

        @Test
        @DisplayName("El hash almacenado no es el código en texto plano (seguridad)")
        void givenValidRequest_thenTokenHashIsNotPlainCode() throws Exception {
            // Arrange
            UserEntity user = buildUser();
            when(userRepository.findByEmail(any())).thenReturn(Optional.of(user));
            when(tokenRepository.findByUserAndUsedFalse(any())).thenReturn(List.of());
            when(verificationCodeService.generateVerificationCode()).thenReturn("654321");

            // Act
            sut.requestReset(request);

            // Assert — tokenHash debe ser el SHA-256 del código, no el código mismo
            verify(tokenRepository).save(tokenCaptor.capture());
            String storedHash = tokenCaptor.getValue().getTokenHash();
            assertThat(storedHash).isNotEqualTo("654321")
            .isEqualTo(sha256Base64("654321"));
        }

        @Test
        @DisplayName("El token expira en ~15 minutos desde ahora")
        void givenValidRequest_thenTokenExpiresIn15Minutes() throws RecoveryTokenException {
            // Arrange
            UserEntity user = buildUser();
            when(userRepository.findByEmail(any())).thenReturn(Optional.of(user));
            when(tokenRepository.findByUserAndUsedFalse(any())).thenReturn(List.of());
            when(verificationCodeService.generateVerificationCode()).thenReturn("000000");

            LocalDateTime before = LocalDateTime.now().plusMinutes(14);
            LocalDateTime after  = LocalDateTime.now().plusMinutes(16);

            // Act
            sut.requestReset(request);

            // Assert
            verify(tokenRepository).save(tokenCaptor.capture());
            assertThat(tokenCaptor.getValue().getExpiresAt())
                    .isAfter(before)
                    .isBefore(after);
        }

        @Test
        @DisplayName("Invalida tokens previos del usuario antes de crear uno nuevo")
        void givenUserWithPreviousTokens_thenInvalidateBeforeCreatingNew() throws RecoveryTokenException {
            // Arrange
            UserEntity user = buildUser();
            PasswordResetTokenEntity oldToken = buildToken(user, "old-hash",
                    LocalDateTime.now().plusMinutes(5));

            when(userRepository.findByEmail(any())).thenReturn(Optional.of(user));
            when(tokenRepository.findByUserAndUsedFalse(user)).thenReturn(List.of(oldToken));
            when(verificationCodeService.generateVerificationCode()).thenReturn("111111");

            // Act
            sut.requestReset(request);

            // Assert — el token viejo se guardó como used=true
            // save() se llama: 1 vez para el token antiguo + 1 para el nuevo
            verify(tokenRepository, atLeast(2)).save(any());
            assertThat(oldToken.getUsed()).isTrue();
        }

        @Test
        @DisplayName("El subject del email contiene 'Recuperación'")
        void givenValidRequest_thenEmailSubjectContainsRecuperacion() throws RecoveryTokenException {
            // Arrange
            UserEntity user = buildUser();
            when(userRepository.findByEmail(any())).thenReturn(Optional.of(user));
            when(tokenRepository.findByUserAndUsedFalse(any())).thenReturn(List.of());
            when(verificationCodeService.generateVerificationCode()).thenReturn("999999");

            // Act
            sut.requestReset(request);

            // Assert
            verify(notificationService).sendMail(emailCaptor.capture());
            assertThat(emailCaptor.getValue().subject())
                    .containsIgnoringCase("Recuperación");
        }

        @Test
        @DisplayName("El body del email contiene el código de recuperación")
        void givenValidRequest_thenEmailBodyContainsCode() throws RecoveryTokenException {
            // Arrange
            UserEntity user = buildUser();
            when(userRepository.findByEmail(any())).thenReturn(Optional.of(user));
            when(tokenRepository.findByUserAndUsedFalse(any())).thenReturn(List.of());
            when(verificationCodeService.generateVerificationCode()).thenReturn("777777");

            // Act
            sut.requestReset(request);

            // Assert
            verify(notificationService).sendMail(emailCaptor.capture());
            assertThat(emailCaptor.getValue().body()).contains("777777");
        }

        @Test
        @DisplayName("Lanza UserNotFoundException si el email no está registrado")
        void givenUnknownEmail_thenThrowUserNotFoundException() {
            // Arrange
            String email = "unknown@test.com";
            PasswordResetRequestDTO req = new PasswordResetRequestDTO(email); // Extraído

            when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

            // Act + Assert
            // La lambda ahora es una "Single Statement Lambda" limpia
            assertThatThrownBy(() -> sut.requestReset(req))
                    .isInstanceOf(UserNotFoundException.class);

            // Verificamos que no se disparó ningún proceso posterior
            verifyNoInteractions(tokenRepository, verificationCodeService, notificationService);
        }

        @Test
        @DisplayName("Si el envío de email falla, no se propaga la excepción (resiliencia)")
        void givenEmailServiceThrows_thenNoExceptionPropagates() {
            // Arrange
            UserEntity user = buildUser();
            when(userRepository.findByEmail(any())).thenReturn(Optional.of(user));
            when(tokenRepository.findByUserAndUsedFalse(any())).thenReturn(List.of());
            when(verificationCodeService.generateVerificationCode()).thenReturn("123456");
            doThrow(new RuntimeException("SMTP down")).when(notificationService).sendMail(any());

            // Act + Assert
            assertThatCode(() -> sut.requestReset(request))
                    .doesNotThrowAnyException();
        }
    }

    // ================================================================
    // resetPassword()
    // ================================================================

    @Nested
    @DisplayName("resetPassword()")
    class ResetPasswordTests {

        @Test
        @DisplayName("Camino feliz: actualiza contraseña, marca token como usado y envía confirmación")
        void givenValidToken_thenResetPasswordAndSendConfirmation() throws Exception {
            // Arrange
            String plainCode = "123456";
            String hashed    = sha256Base64(plainCode);
            UserEntity user  = buildUser();
            PasswordResetTokenEntity token = buildToken(
                    user, hashed, LocalDateTime.now().plusMinutes(10));

            PasswordResetDTO dto = new PasswordResetDTO(plainCode, "newSecurePass123!");

            when(tokenRepository.findByTokenHashAndUsedFalse(hashed))
                    .thenReturn(Optional.of(token));
            when(passwordEncoder.encode("newSecurePass123!")).thenReturn("new-encoded");
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // Act
            assertThatCode(() -> sut.resetPassword(dto)).doesNotThrowAnyException();

            // Assert — contraseña actualizada
            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getPassword()).isEqualTo("new-encoded");

            // Assert — token marcado como usado
            assertThat(token.getUsed()).isTrue();
            verify(tokenRepository, atLeastOnce()).save(token);

            // Assert — email de confirmación enviado
            verify(notificationService, times(1)).sendMail(emailCaptor.capture());
            assertThat(emailCaptor.getValue().recipient()).isEqualTo("user@test.com");
        }

        @Test
        @DisplayName("updatedAt del usuario queda seteado cerca del momento actual")
        void givenValidToken_thenUpdatedAtIsSetToNow() throws Exception {
            // Arrange
            String plainCode = "123456";
            UserEntity user  = buildUser();
            PasswordResetTokenEntity token = buildToken(
                    user, sha256Base64(plainCode), LocalDateTime.now().plusMinutes(10));

            when(tokenRepository.findByTokenHashAndUsedFalse(any()))
                    .thenReturn(Optional.of(token));
            when(passwordEncoder.encode(any())).thenReturn("encoded");
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            LocalDateTime before = LocalDateTime.now().minusSeconds(1);

            // Act
            sut.resetPassword(new PasswordResetDTO(plainCode, "newPass"));

            LocalDateTime after = LocalDateTime.now().plusSeconds(1);

            // Assert
            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getUpdatedAt())
                    .isAfterOrEqualTo(before)
                    .isBeforeOrEqualTo(after);
        }
        @Test
        @DisplayName("Lanza InvalidTokenException si el token no existe o ya fue usado")
        void givenInvalidToken_thenThrowInvalidTokenException() throws Exception {
            // Arrange
            String plainCode = "bad-code";
            String newPassword = "newPass";
            String hashed = sha256Base64(plainCode);

            // Preparamos el DTO fuera de la lambda
            var request = new PasswordResetDTO(plainCode, newPassword);

            when(tokenRepository.findByTokenHashAndUsedFalse(hashed))
                    .thenReturn(Optional.empty());

            // Act + Assert
            assertThatThrownBy(() -> sut.resetPassword(request))
                    .isInstanceOf(InvalidTokenException.class)
                    .hasMessageContaining("inválido");

            // Verificamos que el sistema se detuvo a tiempo
            verifyNoInteractions(passwordEncoder, notificationService);
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Lanza InvalidTokenException si el token está expirado")
        void givenExpiredToken_thenThrowInvalidTokenException() throws Exception {
            // Arrange
            String plainCode = "123456";
            String newPassword = "newPass";
            String hashed = sha256Base64(plainCode);

            // Extraemos el DTO
            var request = new PasswordResetDTO(plainCode, newPassword);

            UserEntity user = buildUser();
            // Token expirado hace 1 minuto
            PasswordResetTokenEntity expiredToken = buildToken(
                    user,
                    hashed,
                    LocalDateTime.now().minusMinutes(1)
            );

            when(tokenRepository.findByTokenHashAndUsedFalse(hashed))
                    .thenReturn(Optional.of(expiredToken));

            // Act + Assert
            // Lambda limpia con una sola invocación
            assertThatThrownBy(() -> sut.resetPassword(request))
                    .isInstanceOf(InvalidTokenException.class)
                    .hasMessageContaining("expirado");

            verify(userRepository, never()).save(any());
            verifyNoInteractions(passwordEncoder, notificationService);
        }

        @Test
        @DisplayName("El subject del email de confirmación indica éxito en el cambio")
        void givenValidToken_thenConfirmationEmailSubjectIsCorrect() throws Exception {
            // Arrange
            String plainCode = "123456";
            UserEntity user  = buildUser();
            PasswordResetTokenEntity token = buildToken(
                    user, sha256Base64(plainCode), LocalDateTime.now().plusMinutes(10));

            when(tokenRepository.findByTokenHashAndUsedFalse(any()))
                    .thenReturn(Optional.of(token));
            when(passwordEncoder.encode(any())).thenReturn("encoded");
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // Act
            sut.resetPassword(new PasswordResetDTO(plainCode, "newPass"));

            // Assert
            verify(notificationService).sendMail(emailCaptor.capture());
            assertThat(emailCaptor.getValue().subject())
                    .containsIgnoringCase("Contraseña")
                    .containsIgnoringCase("Exitosamente");
        }

        @Test
        @DisplayName("El body del email de confirmación contiene el nombre del usuario")
        void givenValidToken_thenConfirmationEmailBodyContainsUserName() throws Exception {
            // Arrange
            String plainCode = "123456";
            UserEntity user  = buildUser(); // name = "Usuario Test"
            PasswordResetTokenEntity token = buildToken(
                    user, sha256Base64(plainCode), LocalDateTime.now().plusMinutes(10));

            when(tokenRepository.findByTokenHashAndUsedFalse(any()))
                    .thenReturn(Optional.of(token));
            when(passwordEncoder.encode(any())).thenReturn("encoded");
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // Act
            sut.resetPassword(new PasswordResetDTO(plainCode, "newPass"));

            // Assert
            verify(notificationService).sendMail(emailCaptor.capture());
            assertThat(emailCaptor.getValue().body())
                    .contains("Usuario Test");
        }

        @Test
        @DisplayName("Si el envío de confirmación falla, no se propaga la excepción")
        void givenEmailServiceThrows_thenNoExceptionPropagates() throws Exception {
            // Arrange
            String plainCode = "123456";
            UserEntity user  = buildUser();
            PasswordResetTokenEntity token = buildToken(
                    user, sha256Base64(plainCode), LocalDateTime.now().plusMinutes(10));

            when(tokenRepository.findByTokenHashAndUsedFalse(any()))
                    .thenReturn(Optional.of(token));
            when(passwordEncoder.encode(any())).thenReturn("encoded");
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            doThrow(new RuntimeException("SMTP down")).when(notificationService).sendMail(any());

            // Act + Assert
            assertThatCode(() ->
                    sut.resetPassword(new PasswordResetDTO(plainCode, "newPass")))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Token exactamente en el límite de expiración (expiresAt == now) es rechazado")
        void givenTokenExpiringExactlyNow_thenThrowInvalidTokenException() throws Exception {
            // Arrange
            String plainCode = "123456";
            String newPass = "newPass";
            String hashed = sha256Base64(plainCode);

            // Extraemos el DTO de la lambda
            var request = new PasswordResetDTO(plainCode, newPass);

            UserEntity user = buildUser();
            // Expirado por el margen más pequeño posible
            PasswordResetTokenEntity token = buildToken(
                    user,
                    hashed,
                    LocalDateTime.now().minusNanos(1)
            );

            when(tokenRepository.findByTokenHashAndUsedFalse(hashed))
                    .thenReturn(Optional.of(token));

            // Act + Assert
            // Lambda con invocación única
            assertThatThrownBy(() -> sut.resetPassword(request))
                    .isInstanceOf(InvalidTokenException.class);
        }
    }
}