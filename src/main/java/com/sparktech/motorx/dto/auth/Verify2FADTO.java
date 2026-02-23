package com.sparktech.motorx.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record Verify2FADTO(
        @NotBlank(message = "El email es requerido")
        @Email(message = "Debe ser un email válido")
        String email,

        @NotBlank(message = "El código es requerido")
        @Pattern(regexp = "^\\d{6}$", message = "El código debe ser de 6 dígitos")
        String code
) {}

