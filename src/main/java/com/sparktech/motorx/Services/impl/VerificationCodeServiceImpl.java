package com.sparktech.motorx.Services.impl;

import com.sparktech.motorx.Services.IEmailNotificationService;
import com.sparktech.motorx.Services.IVerificationCodeCacheService;
import com.sparktech.motorx.Services.IVerificationCodeService;
import com.sparktech.motorx.entity.UserEntity;
import com.sparktech.motorx.exception.VerificationCodeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

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
            emailNotificationService.sendTemplatedMail(
                    user.getEmail(),
                    subject,
                    "two-factor-code.html",
                    getTwoFactorPlaceholders(user, code)
            );

            log.info("Código de verificación enviado a: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Error enviando código de verificación a: {}", user.getEmail(), e);
            throw new VerificationCodeException("Error al enviar el código de verificación", e);
        }
    }

    private static @NotNull Map<String, String> getTwoFactorPlaceholders(UserEntity user, String code) {
        String userName = user.getName() != null ? user.getName() : "Usuario";
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("USER_NAME", userName);
        placeholders.put("VERIFICATION_CODE", code);
        placeholders.put("EXPIRATION_MINUTES", String.valueOf(CODE_EXPIRATION_MINUTES));
        placeholders.put("FALLBACK_BODY", String.format(
                "Hola %s, tu código de verificación es %s. Expira en %d minutos.",
                userName,
                code,
                CODE_EXPIRATION_MINUTES
        ));
        return placeholders;
    }
}

