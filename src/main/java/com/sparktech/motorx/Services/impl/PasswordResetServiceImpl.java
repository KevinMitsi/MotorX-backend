package com.sparktech.motorx.Services.impl;

import com.sparktech.motorx.Services.IEmailNotificationService;
import com.sparktech.motorx.Services.IPasswordResetService;

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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
@Slf4j
@RequiredArgsConstructor
public class PasswordResetServiceImpl implements IPasswordResetService {

    private final JpaUserRepository userRepository;
    private final JpaPasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final IEmailNotificationService notificationService;

    private static final int TOKEN_EXPIRATION_MINUTES = 15;

    @Override
    @Transactional
    public void requestReset(PasswordResetRequestDTO dto) throws UserNotFoundException, RecoveryTokenException {
        log.info("Solicitud de recuperación para: {}", dto.email());

        // 1. Buscar usuario
        UserEntity user = userRepository.findByEmail(dto.email())
                .orElseThrow(() -> new UserNotFoundException("No existe un usuario con el correo: " + dto.email()));

        // 3. Invalidar tokens anteriores
        invalidatePreviousTokens(user);

        // 4. Generar código de recuperación
        String recoveryCode = generateRecoveryCode();

        // 5. Hashear el código para almacenarlo de forma segura
        String hashedCode = hashToken(recoveryCode);

        // 6. Guardar en BD
        PasswordResetTokenEntity tokenEntity = new PasswordResetTokenEntity();
        tokenEntity.setUser(user);
        tokenEntity.setTokenHash(hashedCode);
        tokenEntity.setExpiresAt(LocalDateTime.now().plusMinutes(TOKEN_EXPIRATION_MINUTES));
        tokenEntity.setUsed(false);
        tokenRepository.save(tokenEntity);

        log.info("Password reset token created for user ID: {} - Token expires at: {}",
                user.getId(), tokenEntity.getExpiresAt());

        // 7. Enviar email con el código de recuperación
        sendPasswordResetEmail(user, recoveryCode);

        log.info("Password reset email sent successfully to: {}", dto.email());
    }

    @Override
    @Transactional
    public void resetPassword(PasswordResetDTO dto) throws RecoveryTokenException {
        log.info("Attempting password reset with provided token");

        // 1. Hashear el código proporcionado para comparar
        String hashedProvidedCode = hashToken(dto.token());

        // 2. Buscar el token activo en la BD
        PasswordResetTokenEntity tokenEntity = tokenRepository.findByTokenHashAndUsedFalse(hashedProvidedCode)
                .orElseThrow(() -> new InvalidTokenException("El código de recuperación es inválido o ya fue usado"));

        // 3. Validar expiración
        if (tokenEntity.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidTokenException("El código de recuperación ha expirado. Solicita uno nuevo.");
        }

        // 4. Obtener el usuario asociado
        UserEntity user = tokenEntity.getUser();

        // 5. Actualizar contraseña
        user.setPassword(passwordEncoder.encode(dto.newPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        // 6. Marcar token como usado
        tokenEntity.setUsed(true);
        tokenRepository.save(tokenEntity);

        log.info("Password successfully reset for user ID: {}", user.getId());

        // 7. Enviar email de confirmación
        sendPasswordChangeConfirmationEmail(user);

        log.info("Password change confirmation email sent successfully to: {}", user.getEmail());
    }

    private void invalidatePreviousTokens(UserEntity user) {
        tokenRepository.findByUserAndUsedFalse(user)
                .forEach(token -> {
                    token.setUsed(true);
                    tokenRepository.save(token);
                    log.debug("Invalidated previous token for user ID: {}", user.getId());
                });
    }

    private String generateRecoveryCode() {
        SecureRandom random = new SecureRandom();
        int code = 100000 + random.nextInt(900000); // Genera número entre 100000 y 999999
        return String.valueOf(code);
    }

    /**
     * Crea un hash SHA-256 del código para almacenarlo de forma segura
     */
    private String hashToken(String token) throws RecoveryTokenException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            log.error("Error hashing token: {}", e.getMessage());
            throw new RecoveryTokenException("Error processing recovery token");
        }
    }

    /**
     * Envía el email con el código de recuperación de contraseña
     */
    private void sendPasswordResetEmail(UserEntity user, String recoveryCode) {
        try {
            String subject = "Recuperación de Contraseña - Jmmotoservicio";
            String body = String.format(
                    """
                    Hola %s,
                    
                    Has solicitado restablecer tu contraseña en Jmmotoservicio.
                    
                    Tu código de recuperación es: %s
                    
                    Este código expira en %d minutos.
                    
                    Si no solicitaste este cambio, puedes ignorar este correo.
                    
                    Saludos,
                    Equipo de Taller
                    """,
                    user.getName() != null ? user.getName() : "Usuario",
                    recoveryCode,
                    TOKEN_EXPIRATION_MINUTES
            );

            EmailDTO emailDTO = new EmailDTO(subject, body, user.getEmail());
            notificationService.sendMail(emailDTO);

            log.info("Password reset email sent to: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Error sending password reset email to: {}", user.getEmail(), e);
        }
    }

    /**
     * Envía el email de confirmación de cambio de contraseña
     */
    private void sendPasswordChangeConfirmationEmail(UserEntity user) {
        try {
            String subject = "Contraseña Cambiada Exitosamente - Jmmotoservicio";
            String body = String.format(
                    """
                    Hola %s,
                    
                    Tu contraseña ha sido cambiada exitosamente en la applicación.
                    
                    Fecha y hora del cambio: %s
                    
                    Si no realizaste este cambio, contacta inmediatamente con nuestro soporte.
                    
                    Saludos,
                    Equipo de Taller
                    """,
                    user.getName() != null ? user.getName() : "Usuario",
                    LocalDateTime.now()
            );

            EmailDTO emailDTO = new EmailDTO(subject, body, user.getEmail());
            notificationService.sendMail(emailDTO);

            log.info("Password change confirmation email sent to: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Error sending password change confirmation email to: {}", user.getEmail(), e);
        }
    }
}