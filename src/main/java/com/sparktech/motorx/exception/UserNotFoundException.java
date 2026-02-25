package com.sparktech.motorx.exception;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String message) {
        super(message);
    }

    public UserNotFoundException(Long userId) {
        super("No se encontró el usuario con ID: " + userId);
    }
}
