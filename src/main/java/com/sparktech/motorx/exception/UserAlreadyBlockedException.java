package com.sparktech.motorx.exception;

public class UserAlreadyBlockedException extends RuntimeException {
    public UserAlreadyBlockedException(Long userId) {
        super("El usuario con ID " + userId + " ya se encuentra bloqueado.");
    }
}

