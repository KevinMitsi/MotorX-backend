package com.sparktech.motorx.exception;

public class VehicleNotFoundException extends RuntimeException {
    public VehicleNotFoundException(Long vehicleId) {
        super("No se encontró el vehículo con ID: " + vehicleId);
    }

    public VehicleNotFoundException(String licensePlate) {
        super("No se encontró el vehículo con placa: " + licensePlate);
    }
}

