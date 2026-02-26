package com.sparktech.motorx.Services.impl;

import com.sparktech.motorx.Services.IEmailNotificationService;
import com.sparktech.motorx.Services.IVerificationCodeCacheService;
import com.sparktech.motorx.dto.notification.EmailDTO;
import com.sparktech.motorx.entity.UserEntity;
import com.sparktech.motorx.exception.VerificationCodeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("VerificationCodeServiceImpl - Unit Tests")
class VerificationCodeServiceImplTest {

    private IEmailNotificationService emailNotificationService;
    private IVerificationCodeCacheService cacheService;
    private VerificationCodeServiceImpl sut;

    @BeforeEach
    void setUp() {
        emailNotificationService = mock(IEmailNotificationService.class);
        cacheService = mock(IVerificationCodeCacheService.class);
        sut = new VerificationCodeServiceImpl(emailNotificationService, cacheService);
    }

    @Test
    @DisplayName("generateVerificationCode genera un código numérico de 6 dígitos en rango 100000-999999")
    void generateVerificationCode_formatAndRange() {
        String code = sut.generateVerificationCode();

        // Formato: exactamente 6 dígitos
        assertThat(code).matches("\\d{6}");

        // Rango numérico
        int value = Integer.parseInt(code);
        assertThat(value).isBetween(100000, 999999);
    }

    @Test
    @DisplayName("generateAndSendVerificationCode guarda el código en cache y envía un EmailDTO con el código y destinatario correctos")
    void generateAndSendVerificationCode_savesAndSendsEmail() {
        // Arrange
        UserEntity user = new UserEntity();
        user.setEmail("test.user@example.com");
        user.setName("Test User");

        // Act
        sut.generateAndSendVerificationCode(user);

        // Assert: se guardó en cache con expiración 10
        // capturamos el código pasado al cache
        verify(cacheService, times(1)).saveCode(eq("test.user@example.com"), any(String.class), eq(10));

        // Verificamos que se intentó enviar un EmailDTO hacia el email del usuario
        // Como EmailDTO es un record, podemos capturarlo con ArgumentCaptor o verificar llamada con any
        verify(emailNotificationService, times(1)).sendMail(any(EmailDTO.class));

        // Adicional: comprobar que el EmailDTO enviado contiene el email y el código almacenado
        // Para eso, usamos un Answer para capturar el EmailDTO pasado
        // (volver a mockear para capturar el argumento)
        reset(emailNotificationService);
        doAnswer(invocation -> {
            EmailDTO dto = invocation.getArgument(0);
            assertThat(dto.recipient()).isEqualTo("test.user@example.com");
            assertThat(dto.subject()).containsIgnoringCase("Código de Verificación");
            // El body debe incluir el nombre del usuario y el código (6 dígitos)
            assertThat(dto.body()).contains("Test User");
            assertThat(dto.body()).matches("(?s).*\\d{6}.*");
            return null;
        }).when(emailNotificationService).sendMail(any(EmailDTO.class));

        // Llamar de nuevo para activar el Answer y ejecutar las aserciones
        sut.generateAndSendVerificationCode(user);

        verify(emailNotificationService, times(1)).sendMail(any(EmailDTO.class));
    }

    @Test
    @DisplayName("generateAndSendVerificationCode lanza VerificationCodeException si el envío de email falla")
    void generateAndSendVerificationCode_whenSendFails_thenThrowsVerificationCodeException() {
        // Arrange
        UserEntity user = new UserEntity();
        user.setEmail("fail.user@example.com");
        user.setName("Fail User");

        doThrow(new RuntimeException("SMTP down")).when(emailNotificationService).sendMail(any(EmailDTO.class));

        // Act & Assert
        assertThatThrownBy(() -> sut.generateAndSendVerificationCode(user))
                .isInstanceOf(VerificationCodeException.class)
                .hasMessageContaining("Error al enviar el código de verificación");

        // Aun así, el código debe haberse guardado en cache antes del fallo
        verify(cacheService, times(1)).saveCode(eq("fail.user@example.com"), any(String.class), eq(10));
        verify(emailNotificationService, times(1)).sendMail(any(EmailDTO.class));
    }
}

