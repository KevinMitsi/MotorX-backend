package com.sparktech.motorx.exception;

public class TechnicianNotAssignedException extends RuntimeException {
    public TechnicianNotAssignedException(Long appointmentId) {
        super("El tecnico autenticado no esta asignado a la cita: " + appointmentId);
    }
}

