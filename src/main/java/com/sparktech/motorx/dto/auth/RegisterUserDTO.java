package com.sparktech.motorx.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record RegisterUserDTO(
        @NotBlank @Size(max = 150) String name,
        @NotBlank @Size(max = 30) String dni,
        @NotBlank @Email @Size(max = 150) String email,
        @NotBlank @Size(min = 6, max = 100) String password,
        @NotBlank @Pattern(regexp = "^[0-9+()\\-\\s]{7,20}$") @Size(max = 20) String phone
) {
}
