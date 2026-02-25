package com.sparktech.motorx.mapper;

import com.sparktech.motorx.dto.vehicle.VehicleResponseDTO;
import com.sparktech.motorx.entity.VehicleEntity;
import org.springframework.stereotype.Component;

@Component
public class VehicleMapper {

    public VehicleResponseDTO toResponseDTO(VehicleEntity entity) {
        return new VehicleResponseDTO(
                entity.getId(),
                entity.getBrand(),
                entity.getModel(),
                entity.getYearOfManufacture(),
                entity.getLicensePlate(),
                entity.getCylinderCapacity(),
                entity.getChassisNumber(),
                entity.getOwner().getId(),
                entity.getOwner().getName(),
                entity.getOwner().getEmail(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}

