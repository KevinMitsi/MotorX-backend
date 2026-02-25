package com.sparktech.motorx.dto.employee;

import com.sparktech.motorx.entity.EmployeePosition;
import com.sparktech.motorx.entity.EmployeeState;

import java.time.LocalDateTime;

/**
 * DTO de respuesta con la información completa de un empleado.
 */
public record EmployeeResponseDTO(
        Long employeeId,
        EmployeePosition position,
        EmployeeState state,
        LocalDateTime hireDate,
        Long userId,
        String userName,
        String userEmail,
        String userDni,
        String userPhone,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

