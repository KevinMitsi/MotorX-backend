package com.sparktech.motorx.Services.impl;

import com.sparktech.motorx.Services.IVerificationCodeCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class VerificationCodeCacheServiceImpl implements IVerificationCodeCacheService {

    // Estructura: email -> (código, expiración)
    private final Map<String, CodeEntry> codeCache = new ConcurrentHashMap<>();

    @Override
    public void saveCode(String email, String code, int expirationMinutes) {
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(expirationMinutes);
        codeCache.put(email.toLowerCase(), new CodeEntry(code, expiresAt));
        log.debug("Código guardado para: {} - Expira a las: {}", email, expiresAt);

        // Limpiar códigos expirados en segundo plano
        cleanupExpiredCodes();
    }

    @Override
    public String getCode(String email) {
        CodeEntry entry = codeCache.get(email.toLowerCase());

        if (entry == null) {
            log.debug("No hay código almacenado para: {}", email);
            return null;
        }

        if (entry.isExpired()) {
            log.debug("Código expirado para: {}", email);
            codeCache.remove(email.toLowerCase());
            return null;
        }

        return entry.code;
    }

    @Override
    public void deleteCode(String email) {
        codeCache.remove(email.toLowerCase());
        log.debug("Código eliminado para: {}", email);
    }

    @Override
    public boolean validateCode(String email, String code) {
        String storedCode = getCode(email);

        if (storedCode == null) {
            log.warn("No hay código válido para: {}", email);
            return false;
        }

        boolean isValid = storedCode.equals(code);

        if (isValid) {
            log.info("Código validado correctamente para: {}", email);
            deleteCode(email); // Eliminar después de usar
        } else {
            log.warn("Código inválido para: {}", email);
        }

        return isValid;
    }

    /**
     * Limpia los códigos expirados del caché
     */
    private void cleanupExpiredCodes() {
        codeCache.entrySet().removeIf(entry -> {
            boolean expired = entry.getValue().isExpired();
            if (expired) {
                log.debug("Limpiando código expirado para: {}", entry.getKey());
            }
            return expired;
        });
    }

    /**
     * Clase interna para almacenar código y su expiración
     */
    private record CodeEntry(String code, LocalDateTime expiresAt) {
        boolean isExpired() {
            return LocalDateTime.now().isAfter(expiresAt);
        }
    }
}

