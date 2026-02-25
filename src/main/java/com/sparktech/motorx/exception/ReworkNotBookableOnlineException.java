package com.sparktech.motorx.exception;
public class ReworkNotBookableOnlineException extends AppointmentException {
    public ReworkNotBookableOnlineException() {
        super("Los reprocesos no pueden agendarse en línea. Por favor contáctenos directamente.");
    }
}