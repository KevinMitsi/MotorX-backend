package com.sparktech.motorx.dto.vehicle;

import jakarta.validation.constraints.NotNull;

/**
 * DTO para que el administrador transfiera una moto de dueño.
 */
public record TransferVehicleOwnershipRequestDTO(
        @NotNull(message = "El ID del nuevo propietario es obligatorio")
        Long newOwnerId
) {
}

