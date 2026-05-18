package com.sparktech.motorx.exception;

public class DuplicateServiceNameException extends RuntimeException {
    public DuplicateServiceNameException(String name) {
        super("Ya existe un servicio con el nombre: " + name);
    }
}

