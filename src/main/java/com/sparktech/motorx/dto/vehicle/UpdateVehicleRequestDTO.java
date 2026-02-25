package com.sparktech.motorx.dto.vehicle;

import jakarta.validation.constraints.*;

/**
 * DTO para actualizar un vehículo.
 * La placa NO se puede cambiar (identificador único de la moto).
 * El número de chasis tampoco puede modificarse (dato del documento oficial).
 */
public record UpdateVehicleRequestDTO(

        @NotBlank(message = "La marca de la moto es obligatoria")
        @Size(max = 100)
        String brand,

        @NotBlank(message = "El modelo de la moto es obligatorio")
        @Size(max = 100)
        String model,

        @NotNull(message = "El cilindraje es obligatorio")
        @Min(value = 50, message = "El cilindraje mínimo es 50 cc")
        @Max(value = 9999, message = "El cilindraje máximo es 9999 cc")
        Integer cylinderCapacity
) {
}

