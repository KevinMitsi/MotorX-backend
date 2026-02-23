package com.sparktech.motorx.Services;

public interface IVerificationCodeCacheService {
    /**
     * Guarda un código de verificación con tiempo de expiración
     * @param email Email del usuario
     * @param code Código de verificación
     * @param expirationMinutes Minutos hasta que expire
     */
    void saveCode(String email, String code, int expirationMinutes);

    /**
     * Obtiene el código almacenado para un email
     * @param email Email del usuario
     * @return Código si existe y no ha expirado, null en caso contrario
     */
    String getCode(String email);

    /**
     * Elimina el código almacenado para un email
     * @param email Email del usuario
     */
    void deleteCode(String email);

    /**
     * Valida un código contra el almacenado
     * @param email Email del usuario
     * @param code Código a validar
     * @return true si el código coincide y no ha expirado
     */
    boolean validateCode(String email, String code);
}

