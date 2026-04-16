package com.sparktech.motorx.exception;

public class NotificationNotFoundException extends RuntimeException {
    public NotificationNotFoundException(Long id) {
        super("No se encontro la notificacion con ID: " + id);
    }
}

