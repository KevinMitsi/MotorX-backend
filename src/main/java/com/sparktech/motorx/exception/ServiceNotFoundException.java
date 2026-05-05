package com.sparktech.motorx.exception;

public class ServiceNotFoundException extends RuntimeException {
    public ServiceNotFoundException(Long id) {
        super("No se encontro el servicio con ID: " + id);
    }
}

