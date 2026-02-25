package com.sparktech.motorx.exception;

public class AppointmentNotFoundException extends AppointmentException {
    public AppointmentNotFoundException(Long id) {
        super("No se encontró la cita con ID: " + id);
    }
}