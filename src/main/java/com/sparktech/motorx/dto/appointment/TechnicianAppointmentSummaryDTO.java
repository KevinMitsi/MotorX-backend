package com.sparktech.motorx.dto.appointment;

import com.sparktech.motorx.entity.AppointmentStatus;
import com.sparktech.motorx.entity.AppointmentType;

import java.time.LocalDate;
import java.time.LocalTime;

public record TechnicianAppointmentSummaryDTO(
        Long appointmentId,
        AppointmentType appointmentType,
        AppointmentStatus status,
        LocalDate appointmentDate,
        LocalTime startTime,
        LocalTime endTime,
        Long vehicleId,
        String vehiclePlate,
        String vehicleBrand,
        String vehicleModel,
        Integer currentMileage,
        String clientNotes,
        String clientFullName,
        Long technicianId,
        String technicianFullName
) {
}

