package com.sparktech.motorx.exception;

public class AppointmentOutsideBusinessHoursException extends AppointmentException {
    public AppointmentOutsideBusinessHoursException(String message) {
        super(message);
    }
}