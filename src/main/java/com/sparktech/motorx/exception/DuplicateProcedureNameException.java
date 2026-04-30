package com.sparktech.motorx.exception;

public class DuplicateProcedureNameException extends RuntimeException {
    public DuplicateProcedureNameException(String name) {
        super("El procedimiento ya existe con nombre: " + name);
    }
}

