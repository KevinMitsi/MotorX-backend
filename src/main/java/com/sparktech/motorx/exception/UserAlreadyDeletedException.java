package com.sparktech.motorx.exception;

public class UserAlreadyDeletedException extends RuntimeException {
    public UserAlreadyDeletedException(Long userId) {
        super("El usuario con ID " + userId + " ya ha sido eliminado.");
    }
}

