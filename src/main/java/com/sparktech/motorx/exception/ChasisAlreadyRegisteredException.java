package com.sparktech.motorx.exception;

public class ChasisAlreadyRegisteredException extends RuntimeException{
    public ChasisAlreadyRegisteredException(String chassisNumber) {
        super("Ya existe un vehículo registrado con el número de chasis: " + chassisNumber);
    }
}
