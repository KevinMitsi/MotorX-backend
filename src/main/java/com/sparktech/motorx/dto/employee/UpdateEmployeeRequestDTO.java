package com.sparktech.motorx.dto.employee;

import com.sparktech.motorx.entity.EmployeeState;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO para actualizar los datos de un empleado (cargo y estado).
 */
public record UpdateEmployeeRequestDTO(

        @NotBlank(message = "El cargo es obligatorio")
        @Size(max = 100)
        String position,

        @NotNull(message = "El estado es obligatorio")
        EmployeeState state
) {
}

