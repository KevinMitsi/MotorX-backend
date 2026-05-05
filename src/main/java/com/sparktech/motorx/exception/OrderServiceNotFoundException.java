package com.sparktech.motorx.exception;

public class OrderServiceNotFoundException extends RuntimeException {
    public OrderServiceNotFoundException(Long id) {
        super("No se encontro la orden de servicio con ID: " + id);
    }
}

