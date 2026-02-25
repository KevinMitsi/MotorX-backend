package com.sparktech.motorx.mapper;

import com.sparktech.motorx.dto.appointment.AppointmentResponseDTO;
import com.sparktech.motorx.entity.AppointmentEntity;
import com.sparktech.motorx.entity.EmployeeEntity;
import com.sparktech.motorx.entity.VehicleEntity;
import com.sparktech.motorx.entity.UserEntity;
import org.springframework.stereotype.Component;

@Component
public class AppointmentMapper {

    /**
     * Convierte un AppointmentEntity en su DTO de respuesta.
     */
    public AppointmentResponseDTO toResponseDTO(AppointmentEntity entity) {
        VehicleEntity vehicle = entity.getVehicle();
        UserEntity owner = vehicle.getOwner();
        EmployeeEntity technician = entity.getTechnician();

        String technicianFullName = technician != null
                ? technician.getUser().getName()
                : null;
        Long technicianId = technician != null ? technician.getId() : null;

        return new AppointmentResponseDTO(
                entity.getId(),
                entity.getAppointmentType(),
                entity.getStatus(),
                entity.getAppointmentDate(),
                entity.getStartTime(),
                entity.getEndTime(),
                vehicle.getId(),
                vehicle.getLicensePlate(),
                vehicle.getBrand(),
                vehicle.getModel(),
                owner.getId(),
                owner.getName(),
                owner.getEmail(),
                technicianId,
                technicianFullName,
                entity.getClientNotes(),
                entity.getAdminNotes(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}

