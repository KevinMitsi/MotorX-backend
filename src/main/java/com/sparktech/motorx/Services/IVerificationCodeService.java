package com.sparktech.motorx.Services;

import com.sparktech.motorx.entity.UserEntity;

public interface IVerificationCodeService {
    /**
     * Genera y envía un código de verificación al email del usuario
     *
     * @param user Usuario al que se enviará el código
     */
    void generateAndSendVerificationCode(UserEntity user);

    /**
     * Genera un código de verificación aleatorio de 6 dígitos
     * @return Código de verificación
     */
    String generateVerificationCode();
}

