package com.sparktech.motorx.Services;

import com.sparktech.motorx.dto.vehicle.CreateVehicleRequestDTO;
import com.sparktech.motorx.dto.vehicle.UpdateVehicleRequestDTO;
import com.sparktech.motorx.dto.vehicle.VehicleResponseDTO;
import com.sparktech.motorx.exception.VehicleDoesntBelongToUserException;

import java.util.List;

/**
 * Contrato del servicio de vehículos para clientes.
 * Cada usuario puede tener N vehículos, sin repetir placas en su lista.
 * La placa es única globalmente: si ya existe, pertenece a otro dueño.
 */
public interface IVehicleService {

    /**
     * Registra un nuevo vehículo para el usuario autenticado.
     * Valida formato de placa colombiana (AAA111) y unicidad global.
     * Si la placa ya existe y pertenece a otro usuario, lanza VehicleAlreadyOwnedException.
     */
    VehicleResponseDTO addVehicle(CreateVehicleRequestDTO request);

    /**
     * Lista todos los vehículos del usuario autenticado.
     */
    List<VehicleResponseDTO> getMyVehicles();

    /**
     * Obtiene el detalle de un vehículo específico del usuario autenticado.
     */
    VehicleResponseDTO getMyVehicleById(Long vehicleId) throws VehicleDoesntBelongToUserException;

    /**
     * Actualiza marca, modelo y cilindraje de un vehículo del usuario autenticado.
     * La placa y el número de chasis no son modificables.
     */
    VehicleResponseDTO updateMyVehicle(Long vehicleId, UpdateVehicleRequestDTO request) throws VehicleDoesntBelongToUserException;

    /**
     * Elimina un vehículo del usuario autenticado.
     */
    void deleteMyVehicle(Long vehicleId) throws VehicleDoesntBelongToUserException;
}

