package com.sparktech.motorx.Services.impl;

import com.sparktech.motorx.Services.IEmailNotificationService;
import com.sparktech.motorx.Services.IVerificationCodeCacheService;
import com.sparktech.motorx.Services.IVerificationCodeService;
import com.sparktech.motorx.dto.notification.EmailDTO;
import com.sparktech.motorx.entity.UserEntity;
import com.sparktech.motorx.exception.VerificationCodeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
@Slf4j
@RequiredArgsConstructor
public class VerificationCodeServiceImpl implements IVerificationCodeService {

    private final IEmailNotificationService emailNotificationService;
    private final IVerificationCodeCacheService cacheService;

    private static final int CODE_EXPIRATION_MINUTES = 10;

    @Override
    public void generateAndSendVerificationCode(UserEntity user) {
        String code = generateVerificationCode();

        // Guardar código en caché
        cacheService.saveCode(user.getEmail(), code, CODE_EXPIRATION_MINUTES);

        // Enviar email
        sendVerificationEmail(user, code);

    }

    @Override
    public String generateVerificationCode() {
        SecureRandom random = new SecureRandom();
        int code = 100000 + random.nextInt(900000); // Genera número entre 100000 y 999999
        return String.valueOf(code);
    }

    /**
     * Envía el email con el código de verificación
     */
    private void sendVerificationEmail(UserEntity user, String code) {
        try {
            String subject = "Código de Verificación - Jmmotoservicio";
            EmailDTO emailDTO = getEmailDTO(user, code, subject);
            emailNotificationService.sendMail(emailDTO);

            log.info("Código de verificación enviado a: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Error enviando código de verificación a: {}", user.getEmail(), e);
            throw new VerificationCodeException("Error al enviar el código de verificación", e);
        }
    }

    private static @NotNull EmailDTO getEmailDTO(UserEntity user, String code, String subject) {
        String body = String.format(
                """
                Hola %s, hemos detectado un intento de inicio de sesión,
                
                Tu código de verificación es: %s
                
                Este código expira en 10 minutos.
                
                Si no solicitaste este código, cambia instántenamente tu contraseña.
                """,
                user.getName() != null ? user.getName() : "Usuario",
                code
        );

        return new EmailDTO(subject, body, user.getEmail());
    }
}

