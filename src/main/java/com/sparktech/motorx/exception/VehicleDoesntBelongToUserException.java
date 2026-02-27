package com.sparktech.motorx.exception;

public class VehicleDoesntBelongToUserException extends Exception{

    public VehicleDoesntBelongToUserException(String message) {
        super(message);
    }
}
