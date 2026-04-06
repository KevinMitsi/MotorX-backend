package com.sparktech.motorx.exception;

public class InvalidWarehouseLocationException extends RuntimeException {
    public InvalidWarehouseLocationException(String location) {
        super("La ubicacion de bodega es invalida: " + location);
    }
}

