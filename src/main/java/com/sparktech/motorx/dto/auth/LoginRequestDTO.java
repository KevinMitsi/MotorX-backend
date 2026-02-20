package com.sparktech.motorx.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record LoginRequestDTO(
        @NotBlank @Email String email,
        @NotBlank String password
) {
}