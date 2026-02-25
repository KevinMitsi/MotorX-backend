package com.sparktech.motorx.Services.impl;

import com.sparktech.motorx.Services.IAppointmentService;
import com.sparktech.motorx.Services.IUserService;
import com.sparktech.motorx.dto.appointment.*;
import com.sparktech.motorx.dto.auth.RegisterUserDTO;
import com.sparktech.motorx.dto.user.UpdateUserRequestDTO;
import com.sparktech.motorx.entity.AppointmentStatus;
import com.sparktech.motorx.entity.AppointmentType;
import com.sparktech.motorx.entity.Role;
import com.sparktech.motorx.entity.UserEntity;
import com.sparktech.motorx.entity.VehicleEntity;
import com.sparktech.motorx.exception.AppointmentException;
import com.sparktech.motorx.exception.AppointmentNotFoundException;
import com.sparktech.motorx.repository.JpaAppointmentRepository;
import com.sparktech.motorx.repository.JpaUserRepository;
import com.sparktech.motorx.repository.JpaVehicleRepository;
import com.sparktech.motorx.mapper.AppointmentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements IUserService {

    private final JpaUserRepository jpaUserRepository;
    private final JpaVehicleRepository vehicleRepository;
    private final JpaAppointmentRepository appointmentRepository;
    private final IAppointmentService appointmentService;
    private final AppointmentMapper appointmentMapper;
    private final PasswordEncoder passwordEncoder;

    // ---------------------------------------------------------------
    // REGISTRO Y PERFIL
    // ---------------------------------------------------------------

    @Override
    @Transactional
    public void register(RegisterUserDTO request) {
        if (jpaUserRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("El email ya está registrado");
        }
        if (jpaUserRepository.existsByDni(request.dni())) {
            throw new IllegalArgumentException("El DNI ya está registrado");
        }

        UserEntity user = new UserEntity();
        user.setName(request.name());
        user.setDni(request.dni());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setPhone(request.phone());
        user.setRole(Role.CLIENT);
        user.setEnabled(true);
        user.setAccountLocked(false);

        jpaUserRepository.save(user);
    }

    @Override
    @Transactional
    public void updateUserDTO(Long userId, UpdateUserRequestDTO userUpdate) {
        UserEntity user = jpaUserRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con ID: " + userId));

        user.setName(userUpdate.firstName());
        user.setPhone(userUpdate.phone());

        jpaUserRepository.save(user);
    }

    // ---------------------------------------------------------------
    // CONSULTA DE DISPONIBILIDAD Y PICO Y PLACA
    // ---------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public AvailableSlotsResponseDTO getAvailableSlots(LocalDate date, AppointmentType type) {
        return appointmentService.getAvailableSlots(date, type);
    }

    @Override
    @Transactional(readOnly = true)
    public LicensePlateRestrictionResponseDTO checkLicensePlateRestriction(Long vehicleId, LocalDate date) {
        UserEntity currentUser = getAuthenticatedUser();

        VehicleEntity vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new AppointmentException(
                        "No se encontró el vehículo con ID: " + vehicleId));

        if (!vehicle.getOwner().getId().equals(currentUser.getId())) {
            throw new AppointmentException("El vehículo no pertenece al usuario autenticado.");
        }

        boolean hasRestriction = appointmentService.hasLicensePlateRestriction(
                vehicle.getLicensePlate(), date);

        if (hasRestriction) {
            return LicensePlateRestrictionResponseDTO.of(vehicle.getLicensePlate(), date);
        }
        return null;
    }

    @Override
    public ReworkRedirectResponseDTO getReworkRedirectInfo() {
        return ReworkRedirectResponseDTO.defaultResponse();
    }

    // ---------------------------------------------------------------
    // GESTIÓN DE CITAS DEL CLIENTE
    // ---------------------------------------------------------------

    @Override
    @Transactional
    public AppointmentResponseDTO scheduleAppointment(CreateAppointmentRequestDTO request) {
        UserEntity currentUser = getAuthenticatedUser();
        return appointmentService.createAppointment(request, currentUser.getId());
    }

    @Override
    @Transactional
    public AppointmentResponseDTO cancelMyAppointment(Long appointmentId) {
        UserEntity currentUser = getAuthenticatedUser();

        var appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new AppointmentNotFoundException(appointmentId));

        // Solo puede cancelar sus propias citas
        if (!appointment.getVehicle().getOwner().getId().equals(currentUser.getId())) {
            throw new AppointmentException("No tienes permiso para cancelar esta cita.");
        }

        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new AppointmentException("La cita ya se encuentra cancelada.");
        }

        if (appointment.getStatus() == AppointmentStatus.IN_PROGRESS
                || appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new AppointmentException("No se puede cancelar una cita en progreso o completada.");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setCancellationReason("Cancelada por el cliente.");
        return appointmentMapper.toResponseDTO(appointmentRepository.save(appointment));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponseDTO> getMyAppointmentHistory() {
        UserEntity currentUser = getAuthenticatedUser();
        return appointmentRepository.findByClientIdOrderByDateDesc(currentUser.getId())
                .stream()
                .map(appointmentMapper::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponseDTO> getMyVehicleAppointments(Long vehicleId) {
        UserEntity currentUser = getAuthenticatedUser();

        VehicleEntity vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new AppointmentException(
                        "No se encontró el vehículo con ID: " + vehicleId));

        if (!vehicle.getOwner().getId().equals(currentUser.getId())) {
            throw new AppointmentException("El vehículo no pertenece al usuario autenticado.");
        }

        return appointmentRepository.findByVehicleIdOrderByAppointmentDateDesc(vehicleId)
                .stream()
                .map(appointmentMapper::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AppointmentResponseDTO getMyAppointmentById(Long appointmentId) {
        UserEntity currentUser = getAuthenticatedUser();

        var appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new AppointmentNotFoundException(appointmentId));

        if (!appointment.getVehicle().getOwner().getId().equals(currentUser.getId())) {
            throw new AppointmentException("No tienes permiso para ver esta cita.");
        }

        return appointmentMapper.toResponseDTO(appointment);
    }

    // ---------------------------------------------------------------
    // HELPER PRIVADO
    // ---------------------------------------------------------------

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
        return jpaUserRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario autenticado no encontrado: " + email));
    }
}

