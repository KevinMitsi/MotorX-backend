package com.sparktech.motorx.dto.vehicle;

import java.time.LocalDateTime;

/**
 * DTO de respuesta con la información completa de un vehículo.
 */
public record VehicleResponseDTO(
        Long id,
        String brand,
        String model,
        Integer yearOfManufacture,
        String licensePlate,
        Integer cylinderCapacity,
        String chassisNumber,
        Long ownerId,
        String ownerName,
        String ownerEmail,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
