package com.sparktech.motorx.controller;

import com.sparktech.motorx.Services.IEmployeeService;
import com.sparktech.motorx.dto.vehicle.TransferVehicleOwnershipRequestDTO;
import com.sparktech.motorx.dto.vehicle.VehicleResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/vehicles")
@RequiredArgsConstructor
@Tag(name = "Admin - Vehículos", description = "Gestión administrativa de vehículos: consulta y transferencia de propiedad")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminVehicleController {

    private final IEmployeeService employeeService;

    @GetMapping
    @Operation(summary = "Listar todos los vehículos", description = "Devuelve todos los vehículos registrados en el sistema.")
    public ResponseEntity<@NotNull List<VehicleResponseDTO>> getAllVehicles() {
        return ResponseEntity.ok(employeeService.getAllVehicles());
    }

    @GetMapping("/{vehicleId}")
    @Operation(summary = "Detalle de un vehículo", description = "Obtiene la información completa de un vehículo.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Vehículo encontrado"),
            @ApiResponse(responseCode = "404", description = "Vehículo no encontrado")
    })
    public ResponseEntity<@NotNull VehicleResponseDTO> getVehicleById(@PathVariable Long vehicleId) {
        return ResponseEntity.ok(employeeService.getVehicleById(vehicleId));
    }

    @PatchMapping("/{vehicleId}/transfer-ownership")
    @Operation(
            summary = "Transferir propiedad de un vehículo",
            description = "Cambia el dueño de una moto. Se elimina del dueño original y se asigna al nuevo. " +
                    "El nuevo dueño debe ser un cliente activo. " +
                    "Se valida que el nuevo dueño no tenga ya la misma placa registrada."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Propiedad transferida exitosamente"),
            @ApiResponse(responseCode = "400", description = "El nuevo dueño no es válido o ya tiene el vehículo"),
            @ApiResponse(responseCode = "404", description = "Vehículo o usuario no encontrado")
    })
    public ResponseEntity<@NotNull VehicleResponseDTO> transferOwnership(
            @PathVariable Long vehicleId,
            @Valid @RequestBody TransferVehicleOwnershipRequestDTO request
    ) {
        return ResponseEntity.ok(employeeService.transferVehicleOwnership(vehicleId, request));
    }
}

