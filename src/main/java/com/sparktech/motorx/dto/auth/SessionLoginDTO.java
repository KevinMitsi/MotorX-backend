package com.sparktech.motorx.dto.auth;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import lombok.Builder;

@Builder
public record SessionLoginDTO (
        @NotBlank @Email String email,
        @NotBlank String password
){
}