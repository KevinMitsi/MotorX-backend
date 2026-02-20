package com.sparktech.motorx.dto.user;
import jakarta.validation.constraints.*;


public record UpdateUserRequestDTO(
        @NotBlank(message = "name is required")
        @Size(max = 100)
         String firstName,
        @NotBlank
        @Pattern(regexp = "^\\d{7,15}$", message = "Phone number must contain only digits")
        String phone
) { }