package com.sparktech.motorx.dto.employee;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import com.sparktech.motorx.dto.auth.RegisterUserDTO;

/**
 * DTO para crear un empleado junto con su usuario de la aplicación.
 */
public record CreateEmployeeRequestDTO(

        @NotBlank(message = "El cargo del empleado es obligatorio")
        @Size(max = 100)
        String position,

        @NotNull(message = "Los datos del usuario son obligatorios")
        @Valid
        RegisterUserDTO user
) {
}

