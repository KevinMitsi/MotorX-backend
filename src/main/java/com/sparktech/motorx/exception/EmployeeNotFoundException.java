package com.sparktech.motorx.exception;

public class EmployeeNotFoundException extends RuntimeException {
    public EmployeeNotFoundException(Long employeeId) {
        super("No se encontró el empleado con ID: " + employeeId);
    }
}

