package com.sparktech.motorx.dto.vehicle;

import jakarta.validation.constraints.*;

/**
 * DTO para crear un vehículo.
 * Reglas de negocio:
 * - Placa obligatoria, formato colombiano: 3 letras + 3 dígitos (AAA111)
 * - Marca, cilindraje, modelo y número de chasis son obligatorios
 * - Año de fabricación obligatorio, entre 1950 y el año actual
 */
public record CreateVehicleRequestDTO(

        @NotBlank(message = "La marca de la moto es obligatoria")
        @Size(max = 100, message = "La marca no puede superar 100 caracteres")
        String brand,

        @NotBlank(message = "El modelo de la moto es obligatorio")
        @Size(max = 100, message = "El modelo no puede superar 100 caracteres")
        String model,

        @NotNull(message = "El año de fabricación es obligatorio")
        @Min(value = 1950, message = "El año de fabricación no puede ser anterior a 1950")
        @Max(value = 2026, message = "El año de fabricación no puede ser posterior al año actual")
        Integer yearOfManufacture,

        @NotBlank(message = "El número de placa es obligatorio")
        @Pattern(
                regexp = "^[A-Z]{3}\\d{2}[A-Z]",
                message = "El formato de la placa debe ser AAA111 (3 letras mayúsculas + 3 dígitos)"
        )
        String licensePlate,

        @NotNull(message = "El cilindraje es obligatorio")
        @Min(value = 50, message = "El cilindraje mínimo es 50 cc")
        @Max(value = 9999, message = "El cilindraje máximo es 9999 cc")
        Integer cylinderCapacity,

        @NotBlank(message = "El número de chasis es obligatorio tal como aparece en la tarjeta de propiedad")
        @Size(max = 50, message = "El número de chasis no puede superar 50 caracteres")
        String chassisNumber
) {
}

