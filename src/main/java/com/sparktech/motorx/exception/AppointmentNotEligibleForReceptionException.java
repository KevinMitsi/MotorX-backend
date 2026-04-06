package com.sparktech.motorx.exception;

public class AppointmentNotEligibleForReceptionException extends RuntimeException {
    public AppointmentNotEligibleForReceptionException(Long appointmentId) {
        super("La cita " + appointmentId + " no esta en estado valido para iniciar recepcion.");
    }
}

