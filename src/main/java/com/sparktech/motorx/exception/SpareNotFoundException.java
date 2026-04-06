package com.sparktech.motorx.exception;

public class SpareNotFoundException extends RuntimeException {
    public SpareNotFoundException(Long id) {
        super("No se encontro el repuesto con ID: " + id);
    }
}

