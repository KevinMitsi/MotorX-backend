package com.sparktech.motorx.Services.impl;

import com.sparktech.motorx.Services.IEmployeeService;
import com.sparktech.motorx.dto.employee.CreateEmployeeRequestDTO;
import com.sparktech.motorx.dto.employee.EmployeeResponseDTO;
import com.sparktech.motorx.dto.employee.UpdateEmployeeRequestDTO;
import com.sparktech.motorx.dto.vehicle.TransferVehicleOwnershipRequestDTO;
import com.sparktech.motorx.dto.vehicle.VehicleResponseDTO;
import com.sparktech.motorx.entity.EmployeeEntity;
import com.sparktech.motorx.entity.EmployeeState;
import com.sparktech.motorx.entity.Role;
import com.sparktech.motorx.entity.UserEntity;
import com.sparktech.motorx.entity.VehicleEntity;
import com.sparktech.motorx.exception.EmployeeNotFoundException;
import com.sparktech.motorx.exception.VehicleNotFoundException;
import com.sparktech.motorx.mapper.EmployeeMapper;
import com.sparktech.motorx.mapper.VehicleMapper;
import com.sparktech.motorx.repository.JpaEmployeeRepository;
import com.sparktech.motorx.repository.JpaUserRepository;
import com.sparktech.motorx.repository.JpaVehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements IEmployeeService {

    private final JpaEmployeeRepository employeeRepository;
    private final JpaUserRepository userRepository;
    private final JpaVehicleRepository vehicleRepository;
    private final EmployeeMapper employeeMapper;
    private final VehicleMapper vehicleMapper;
    private final PasswordEncoder passwordEncoder;

    // ---------------------------------------------------------------
    // CRUD DE EMPLEADOS
    // ---------------------------------------------------------------

    @Override
    @Transactional
    public EmployeeResponseDTO createEmployee(CreateEmployeeRequestDTO request) {
        // Validar unicidad de email y DNI
        if (userRepository.existsByEmail(request.user().email())) {
            throw new IllegalArgumentException(
                    "Ya existe un usuario registrado con el email: " + request.user().email());
        }
        if (userRepository.existsByDni(request.user().dni())) {
            throw new IllegalArgumentException(
                    "Ya existe un usuario registrado con el DNI: " + request.user().dni());
        }

        // Crear el usuario con rol EMPLOYEE
        UserEntity user = new UserEntity();
        user.setName(request.user().name());
        user.setDni(request.user().dni());
        user.setEmail(request.user().email());
        user.setPassword(passwordEncoder.encode(request.user().password()));
        user.setPhone(request.user().phone());
        user.setRole(Role.EMPLOYEE);
        user.setEnabled(true);
        user.setAccountLocked(false);
        UserEntity savedUser = userRepository.save(user);

        // Crear el empleado asociado al usuario
        EmployeeEntity employee = new EmployeeEntity();
        employee.setPosition(request.position());
        employee.setState(EmployeeState.AVAILABLE);
        employee.setUser(savedUser);

        return employeeMapper.toResponseDTO(employeeRepository.save(employee));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeResponseDTO> getAllEmployees() {
        return employeeRepository.findAll()
                .stream()
                .map(employeeMapper::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeResponseDTO getEmployeeById(Long employeeId) {
        EmployeeEntity employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new EmployeeNotFoundException(employeeId));
        return employeeMapper.toResponseDTO(employee);
    }

    @Override
    @Transactional
    public EmployeeResponseDTO updateEmployee(Long employeeId, UpdateEmployeeRequestDTO request) {
        EmployeeEntity employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new EmployeeNotFoundException(employeeId));

        employee.setPosition(request.position());
        employee.setState(request.state());

        return employeeMapper.toResponseDTO(employeeRepository.save(employee));
    }

    @Override
    @Transactional
    public void deleteEmployee(Long employeeId) {
        EmployeeEntity employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new EmployeeNotFoundException(employeeId));

        // Eliminamos primero el empleado; el usuario se elimina por
        // ON DELETE CASCADE definido en la FK fk_employees_user del esquema SQL.
        employeeRepository.delete(employee);
    }

    // ---------------------------------------------------------------
    // GESTIÓN DE VEHÍCULOS (ADMIN)
    // ---------------------------------------------------------------

    @Override
    @Transactional
    public VehicleResponseDTO transferVehicleOwnership(Long vehicleId,
                                                        TransferVehicleOwnershipRequestDTO request) {
        // Buscar el vehículo
        VehicleEntity vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new VehicleNotFoundException(vehicleId));

        // Buscar el nuevo propietario y validar que sea un cliente activo
        UserEntity newOwner = userRepository.findById(request.newOwnerId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró el usuario con ID: " + request.newOwnerId()));

        if (newOwner.getRole() != Role.CLIENT) {
            throw new IllegalArgumentException(
                    "El nuevo propietario debe tener rol CLIENT. " +
                    "El usuario indicado tiene rol: " + newOwner.getRole());
        }

        if (!newOwner.isEnabled()) {
            throw new IllegalArgumentException(
                    "El nuevo propietario tiene la cuenta deshabilitada.");
        }

        if (vehicle.getOwner().getId().equals(newOwner.getId())) {
            throw new IllegalArgumentException(
                    "El vehículo ya pertenece al usuario indicado.");
        }

        // Verificar que el nuevo dueño no tenga ya una moto con la misma placa
        // (no debería ocurrir por constraint único, pero validación extra)
        boolean newOwnerAlreadyHasPlate = vehicleRepository.findByOwnerId(newOwner.getId())
                .stream()
                .anyMatch(v -> v.getLicensePlate().equals(vehicle.getLicensePlate()));
        if (newOwnerAlreadyHasPlate) {
            throw new IllegalArgumentException(
                    "El nuevo propietario ya tiene registrada una moto con la placa: "
                            + vehicle.getLicensePlate());
        }

        // Realizar la transferencia
        vehicle.setOwner(newOwner);
        return vehicleMapper.toResponseDTO(vehicleRepository.save(vehicle));
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleResponseDTO> getAllVehicles() {
        return vehicleRepository.findAll()
                .stream()
                .map(vehicleMapper::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public VehicleResponseDTO getVehicleById(Long vehicleId) {
        VehicleEntity vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new VehicleNotFoundException(vehicleId));
        return vehicleMapper.toResponseDTO(vehicle);
    }
}

