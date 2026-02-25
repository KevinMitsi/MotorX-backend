package com.sparktech.motorx.controller;

import com.sparktech.motorx.Services.IVehicleService;
import com.sparktech.motorx.dto.vehicle.CreateVehicleRequestDTO;
import com.sparktech.motorx.dto.vehicle.UpdateVehicleRequestDTO;
import com.sparktech.motorx.dto.vehicle.VehicleResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/user/vehicles")
@RequiredArgsConstructor
@Tag(name = "User - Vehículos", description = "CRUD de vehículos del cliente autenticado")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('CLIENT')")
public class VehicleController {

    private final IVehicleService vehicleService;

    @PostMapping
    @Operation(
            summary = "Registrar un nuevo vehículo",
            description = "Agrega una moto a la lista del cliente autenticado. " +
                    "La placa debe tener formato colombiano AAA111. " +
                    "Si la placa ya pertenece a otro usuario, se indica que contacte al administrador."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Vehículo registrado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o placa con formato incorrecto"),
            @ApiResponse(responseCode = "409", description = "La placa ya está registrada y pertenece a otro usuario")
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
            @ApiResponse(responseCode = "403", description = "El vehículo no pertenece al usuario"),
            @ApiResponse(responseCode = "404", description = "Vehículo no encontrado")
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
                    "La placa y el número de chasis NO son modificables."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Vehículo actualizado"),
            @ApiResponse(responseCode = "403", description = "El vehículo no pertenece al usuario"),
            @ApiResponse(responseCode = "404", description = "Vehículo no encontrado")
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
            @ApiResponse(responseCode = "403", description = "El vehículo no pertenece al usuario"),
            @ApiResponse(responseCode = "404", description = "Vehículo no encontrado")
    })
    public ResponseEntity<@NotNull Object> deleteMyVehicle(@PathVariable Long vehicleId) {
        vehicleService.deleteMyVehicle(vehicleId);
        return ResponseEntity.noContent().build();
    }
}

