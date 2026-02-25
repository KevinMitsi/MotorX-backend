package com.sparktech.motorx.exception;

/**
 * Se lanza cuando un vehículo con la placa indicada ya existe en el sistema
 * y pertenece a otro usuario. El cliente debe contactar al administrador
 * si adquirió la moto para gestionar el cambio de dueño.
 */
public class VehicleAlreadyOwnedException extends RuntimeException {

    private static final String MESSAGE_TEMPLATE =
            "La moto con placa '%s' ya está registrada en el sistema y pertenece a otro usuario. " +
            "Si usted compró este vehículo, comuníquese con el administrador para gestionar " +
            "el cambio de dueño.";

    public VehicleAlreadyOwnedException(String licensePlate) {
        super(String.format(MESSAGE_TEMPLATE, licensePlate));
    }
}

