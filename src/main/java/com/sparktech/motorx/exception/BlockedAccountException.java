package com.sparktech.motorx.exception;

public class BlockedAccountException extends RuntimeException{
    public BlockedAccountException(String email) {
        super("La cuenta con correo " + email + " está bloqueada. Por favor, contacte al administrador.");
    }
}
