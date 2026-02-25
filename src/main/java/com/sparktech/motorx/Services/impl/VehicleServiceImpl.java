package com.sparktech.motorx.Services.impl;

import com.sparktech.motorx.Services.IVehicleService;
import com.sparktech.motorx.dto.vehicle.CreateVehicleRequestDTO;
import com.sparktech.motorx.dto.vehicle.UpdateVehicleRequestDTO;
import com.sparktech.motorx.dto.vehicle.VehicleResponseDTO;
import com.sparktech.motorx.entity.UserEntity;
import com.sparktech.motorx.entity.VehicleEntity;
import com.sparktech.motorx.exception.VehicleAlreadyOwnedException;
import com.sparktech.motorx.exception.VehicleNotFoundException;
import com.sparktech.motorx.mapper.VehicleMapper;
import com.sparktech.motorx.repository.JpaUserRepository;
import com.sparktech.motorx.repository.JpaVehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VehicleServiceImpl implements IVehicleService {

    private final JpaVehicleRepository vehicleRepository;
    private final JpaUserRepository userRepository;
    private final VehicleMapper vehicleMapper;

    // ---------------------------------------------------------------
    // CRUD DE VEHÍCULOS DEL CLIENTE AUTENTICADO
    // ---------------------------------------------------------------

    @Override
    @Transactional
    public VehicleResponseDTO addVehicle(CreateVehicleRequestDTO request) {
        UserEntity currentUser = getAuthenticatedUser();

        // REGLA 1: La placa debe tener formato colombiano AAA111
        // (validada por @Pattern en el DTO, pero doble check a nivel de servicio)
        String plate = request.licensePlate().toUpperCase().trim();

        // REGLA 2: Si la placa ya existe en el sistema, puede pertenecer a:
        //   a) El mismo usuario (ya la tiene registrada) → error de duplicado en su lista
        //   b) Otro usuario → advertir que contacte al administrador para cambio de dueño
        if (vehicleRepository.existsByLicensePlate(plate)) {
            VehicleEntity existing = vehicleRepository.findByLicensePlate(plate)
                    .orElseThrow(() -> new VehicleNotFoundException(plate));
            if (existing.getOwner().getId().equals(currentUser.getId())) {
                throw new IllegalArgumentException(
                        "Ya tienes registrada una moto con la placa: " + plate);
            }
            // La moto pertenece a otro usuario
            throw new VehicleAlreadyOwnedException(plate);
        }

        // REGLA 3: El número de chasis también debe ser único en el sistema
        if (vehicleRepository.existsByChassisNumber(request.chassisNumber().trim())) {
            throw new IllegalArgumentException(
                    "Ya existe un vehículo registrado con el número de chasis: "
                            + request.chassisNumber());
        }

        VehicleEntity vehicle = new VehicleEntity();
        vehicle.setBrand(request.brand().trim());
        vehicle.setModel(request.model().trim());
        vehicle.setLicensePlate(plate);
        vehicle.setCylinderCapacity(request.cylinderCapacity());
        vehicle.setChassisNumber(request.chassisNumber().trim());
        vehicle.setOwner(currentUser);

        return vehicleMapper.toResponseDTO(vehicleRepository.save(vehicle));
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleResponseDTO> getMyVehicles() {
        UserEntity currentUser = getAuthenticatedUser();
        return vehicleRepository.findByOwnerIdOrderByCreatedAtDesc(currentUser.getId())
                .stream()
                .map(vehicleMapper::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public VehicleResponseDTO getMyVehicleById(Long vehicleId) {
        UserEntity currentUser = getAuthenticatedUser();
        VehicleEntity vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new VehicleNotFoundException(vehicleId));

        validateOwnership(vehicle, currentUser);
        return vehicleMapper.toResponseDTO(vehicle);
    }

    @Override
    @Transactional
    public VehicleResponseDTO updateMyVehicle(Long vehicleId, UpdateVehicleRequestDTO request) {
        UserEntity currentUser = getAuthenticatedUser();
        VehicleEntity vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new VehicleNotFoundException(vehicleId));

        validateOwnership(vehicle, currentUser);

        // Solo se permite actualizar marca, modelo y cilindraje
        // La placa y el número de chasis son inmutables (identificadores oficiales)
        vehicle.setBrand(request.brand().trim());
        vehicle.setModel(request.model().trim());
        vehicle.setCylinderCapacity(request.cylinderCapacity());

        return vehicleMapper.toResponseDTO(vehicleRepository.save(vehicle));
    }

    @Override
    @Transactional
    public void deleteMyVehicle(Long vehicleId) {
        UserEntity currentUser = getAuthenticatedUser();
        VehicleEntity vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new VehicleNotFoundException(vehicleId));

        validateOwnership(vehicle, currentUser);
        vehicleRepository.delete(vehicle);
    }

    // ---------------------------------------------------------------
    // HELPERS PRIVADOS
    // ---------------------------------------------------------------

    private void validateOwnership(VehicleEntity vehicle, UserEntity user) {
        if (!vehicle.getOwner().getId().equals(user.getId())) {
            throw new SecurityException(
                    "No tienes permiso para acceder al vehículo con ID: " + vehicle.getId());
        }
    }

    private UserEntity getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("No hay usuario autenticado.");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserEntity userEntity) {
            return userEntity;
        }
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Usuario autenticado no encontrado: " + email));
    }
}

