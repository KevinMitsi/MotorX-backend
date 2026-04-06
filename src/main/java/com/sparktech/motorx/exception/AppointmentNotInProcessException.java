package com.sparktech.motorx.exception;

public class AppointmentNotInProcessException extends RuntimeException {
    public AppointmentNotInProcessException(Long appointmentId) {
        super("La cita " + appointmentId + " no esta en estado IN_PROGRESS.");
    }
}

