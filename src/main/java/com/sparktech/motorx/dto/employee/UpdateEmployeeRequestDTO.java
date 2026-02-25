package com.sparktech.motorx.dto.employee;

import com.sparktech.motorx.entity.EmployeePosition;
import com.sparktech.motorx.entity.EmployeeState;
import jakarta.validation.constraints.NotNull;

/**
 * DTO para actualizar los datos de un empleado (cargo y estado).
 */
public record UpdateEmployeeRequestDTO(

        @NotNull(message = "El cargo es obligatorio")
        EmployeePosition position,

        @NotNull(message = "El estado es obligatorio")
        EmployeeState state
) {
}

