package com.sparktech.motorx.dto.employee;

import com.sparktech.motorx.entity.EmployeePosition;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import com.sparktech.motorx.dto.auth.RegisterUserDTO;

/**
 * DTO para crear un empleado junto con su usuario de la aplicación.
 */
public record CreateEmployeeRequestDTO(

        @NotNull(message = "El cargo del empleado es obligatorio")
        EmployeePosition position,

        @NotNull(message = "Los datos del usuario son obligatorios")
        @Valid
        RegisterUserDTO user
) {
}

