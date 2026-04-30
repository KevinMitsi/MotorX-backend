package com.sparktech.motorx.exception;

public class ProcedureNotFoundException extends RuntimeException {
    public ProcedureNotFoundException(Long id) {
        super("No se encontro el procedimiento con ID: " + id);
    }
}

