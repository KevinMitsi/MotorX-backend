package com.sparktech.motorx.dto.reception;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(name = "ConfirmReceptionDTO", description = "Datos para confirmar la recepcion de una moto")
public record ConfirmReceptionDTO(
        @Schema(description = "Placa de la moto", example = "ABC12D")
        @NotBlank(message = "La placa es obligatoria")
        String licensePlate,

        @Schema(description = "Codigo de verificacion de 4 digitos", example = "1234")
        @NotBlank(message = "El codigo es obligatorio")
        @Pattern(regexp = "\\d{4}", message = "El codigo debe tener 4 digitos")
        String code
) {
}

