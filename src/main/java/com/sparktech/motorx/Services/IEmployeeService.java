package com.sparktech.motorx.Services;

import com.sparktech.motorx.dto.employee.CreateEmployeeRequestDTO;
import com.sparktech.motorx.dto.employee.EmployeeResponseDTO;
import com.sparktech.motorx.dto.employee.UpdateEmployeeRequestDTO;
import com.sparktech.motorx.dto.vehicle.TransferVehicleOwnershipRequestDTO;
import com.sparktech.motorx.dto.vehicle.VehicleResponseDTO;

import java.util.List;

/**
 * Contrato del servicio administrativo de empleados y gestión de vehículos (admin).
 */
public interface IEmployeeService {

    /**
     * Crea un empleado junto con su usuario en la aplicación (rol EMPLOYEE).
     */
    EmployeeResponseDTO createEmployee(CreateEmployeeRequestDTO request);

    /**
     * Lista todos los empleados registrados.
     */
    List<EmployeeResponseDTO> getAllEmployees();

    /**
     * Obtiene el detalle de un empleado por su ID de empleado.
     */
    EmployeeResponseDTO getEmployeeById(Long employeeId);

    /**
     * Actualiza el cargo y estado de un empleado.
     */
    EmployeeResponseDTO updateEmployee(Long employeeId, UpdateEmployeeRequestDTO request);

    /**
     * Elimina un empleado y su usuario asociado del sistema.
     */
    void deleteEmployee(Long employeeId);

    /**
     * Transfiere la propiedad de una moto de su dueño actual al nuevo dueño indicado.
     * Valida que el nuevo dueño exista y sea un cliente activo.
     */
    VehicleResponseDTO transferVehicleOwnership(Long vehicleId, TransferVehicleOwnershipRequestDTO request);

    /**
     * Lista todos los vehículos registrados en el sistema (vista admin).
     */
    List<VehicleResponseDTO> getAllVehicles();

    /**
     * Obtiene el detalle de un vehículo por ID (vista admin).
     */
    VehicleResponseDTO getVehicleById(Long vehicleId);
}

