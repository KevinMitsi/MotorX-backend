package com.sparktech.motorx.dto.auth;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import org.hibernate.validator.constraints.Length;

@Builder
public record PasswordResetDTO(
        @NotBlank String token,
        @NotBlank
        @Length(min = 8, max = 20)
        @Pattern(
                regexp = "^(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&._-])[A-Za-z\\d@$!%*?&._-]+$",
                message = "La contraseña debe contener al menos una mayúscula, un número y un símbolo (@$!%*?&._-)"
        ) String newPassword)
{
}
