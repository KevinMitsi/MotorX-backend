package com.sparktech.motorx.domain.enums;

public enum EventType {

        USER_REGISTER_ATTEMPT,
        USER_REGISTER_SUCCESS,
        USER_REGISTER_FAILED,

        PASSWORD_RESET_REQUEST,
        PASSWORD_RESET_SUCCESS,

        APPOINTMENT_CREATED,
        APPOINTMENT_CONFLICT,
        APPOINTMENT_CANCELLED,

        LOGIN_SUCCESS,
        LOGIN_FAILED
}
