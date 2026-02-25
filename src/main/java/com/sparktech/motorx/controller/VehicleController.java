package com.sparktech.motorx.controller;

import com.sparktech.motorx.Services.IVehicleService;
import com.sparktech.motorx.dto.error.ResponseErrorDTO;
import com.sparktech.motorx.dto.vehicle.CreateVehicleRequestDTO;
import com.sparktech.motorx.dto.vehicle.UpdateVehicleRequestDTO;
import com.sparktech.motorx.dto.vehicle.VehicleResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/user/vehicles")
@RequiredArgsConstructor
@Tag(name = "User - Vehículos", description = "CRUD de vehículos del cliente autenticado. Cada vehículo incluye año de fabricación (1950–2026)")
@SecurityRequirement(name = "bearerAuth")
public class VehicleController {

    private final IVehicleService vehicleService;

    @PostMapping
    @Operation(
            summary = "Registrar un nuevo vehículo",
            description = "Agrega una moto a la lista del cliente autenticado. " +
                    "Campos obligatorios: marca, modelo, año de fabricación (1950–2026), " +
                    "placa en formato colombiano AAA111, cilindraje (50–9999 cc) y número de chasis. " +
                    "Si la placa ya pertenece a otro usuario, se indica que contacte al administrador. " +
                    "Si el número de chasis ya existe en el sistema, también se rechaza el registro."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Vehículo registrado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos, placa con formato incorrecto o año de fabricación fuera de rango",
                    content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "409", description = "La placa o el número de chasis ya están registrados en el sistema",
                    content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class)))
    })
    public ResponseEntity<@NotNull VehicleResponseDTO> addVehicle(
            @Valid @RequestBody CreateVehicleRequestDTO request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(vehicleService.addVehicle(request));
    }

    @GetMapping
    @Operation(summary = "Listar mis vehículos", description = "Devuelve todos los vehículos del cliente autenticado.")
    public ResponseEntity<@NotNull List<VehicleResponseDTO>> getMyVehicles() {
        return ResponseEntity.ok(vehicleService.getMyVehicles());
    }

    @GetMapping("/{vehicleId}")
    @Operation(summary = "Detalle de un vehículo", description = "Obtiene el detalle de un vehículo propio.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Vehículo encontrado"),
            @ApiResponse(responseCode = "403", description = "El vehículo no pertenece al usuario",
                    content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "404", description = "Vehículo no encontrado",
                    content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class)))
    })
    public ResponseEntity<@NotNull VehicleResponseDTO> getMyVehicleById(
            @PathVariable Long vehicleId
    ) {
        return ResponseEntity.ok(vehicleService.getMyVehicleById(vehicleId));
    }

    @PutMapping("/{vehicleId}")
    @Operation(
            summary = "Actualizar un vehículo",
            description = "Actualiza marca, modelo y cilindraje. " +
                    "La placa, el número de chasis y el año de fabricación NO son modificables " +
                    "ya que son datos del documento oficial del vehículo."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Vehículo actualizado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos",
                    content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "403", description = "El vehículo no pertenece al usuario",
                    content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "404", description = "Vehículo no encontrado",
                    content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class)))
    })
    public ResponseEntity<@NotNull VehicleResponseDTO> updateMyVehicle(
            @PathVariable Long vehicleId,
            @Valid @RequestBody UpdateVehicleRequestDTO request
    ) {
        return ResponseEntity.ok(vehicleService.updateMyVehicle(vehicleId, request));
    }

    @DeleteMapping("/{vehicleId}")
    @Operation(summary = "Eliminar un vehículo", description = "Elimina un vehículo de la lista del cliente.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Vehículo eliminado"),
            @ApiResponse(responseCode = "403", description = "El vehículo no pertenece al usuario",
                    content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "404", description = "Vehículo no encontrado",
                    content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class)))
    })
    public ResponseEntity<@NotNull Object> deleteMyVehicle(@PathVariable Long vehicleId) {
        vehicleService.deleteMyVehicle(vehicleId);
        return ResponseEntity.noContent().build();
    }
}

