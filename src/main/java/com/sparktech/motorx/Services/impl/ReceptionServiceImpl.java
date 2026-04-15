package com.sparktech.motorx.Services.impl;

import com.sparktech.motorx.Services.IEmailNotificationService;
import com.sparktech.motorx.Services.ICurrentUserService;
import com.sparktech.motorx.Services.ILogService;
import com.sparktech.motorx.Services.IReceptionService;
import com.sparktech.motorx.dto.appointment.AppointmentResponseDTO;
import com.sparktech.motorx.dto.notification.EmailDTO;
import com.sparktech.motorx.dto.reception.ConfirmReceptionDTO;
import com.sparktech.motorx.entity.AppointmentEntity;
import com.sparktech.motorx.entity.AppointmentStatus;
import com.sparktech.motorx.entity.LogActionType;
import com.sparktech.motorx.entity.LogServiceName;
import com.sparktech.motorx.entity.UserEntity;
import com.sparktech.motorx.exception.AppointmentNotEligibleForReceptionException;
import com.sparktech.motorx.exception.AppointmentNotFoundException;
import com.sparktech.motorx.exception.InvalidVerificationCodeException;
import com.sparktech.motorx.mapper.AppointmentMapper;
import com.sparktech.motorx.repository.JpaAppointmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ReceptionServiceImpl implements IReceptionService {

    private static final int CODE_EXPIRATION_MINUTES = 15;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final JpaAppointmentRepository appointmentRepository;
    private final AppointmentMapper appointmentMapper;
    private final IEmailNotificationService emailNotificationService;
    private final ICurrentUserService currentUserService;
    private final ILogService logService;

    @Override
    @Transactional
    public AppointmentResponseDTO initiateReception(Long appointmentId) {
        UserEntity actor = currentUserService.getAuthenticatedUser();
        try {
            AppointmentEntity appointment = appointmentRepository.findById(appointmentId)
                    .orElseThrow(() -> new AppointmentNotFoundException(appointmentId));

            if (appointment.getStatus() != AppointmentStatus.SCHEDULED) {
                throw new AppointmentNotEligibleForReceptionException(appointmentId);
            }

            String code = generateCode();
            appointment.setVerificationCode(code);
            appointment.setVerificationCodeCreatedAt(LocalDateTime.now());
            appointment.setStatus(AppointmentStatus.AWAITING_CONFIRMATION);

            AppointmentEntity saved = appointmentRepository.save(appointment);

            String body = "Hola " + saved.getVehicle().getOwner().getName() + ",\n\n" +
                    "Tu codigo de recepcion de moto es: " + code + "\n" +
                    "Este codigo expira en " + CODE_EXPIRATION_MINUTES + " minutos.";
            emailNotificationService.sendMail(new EmailDTO(
                    "Codigo de recepcion - Jmmotoservicio",
                    body,
                    saved.getVehicle().getOwner().getEmail()
            ));

            logService.logSuccess(
                    LogServiceName.RECEPTION,
                    LogActionType.INITIATE_RECEPTION,
                    actor.getEmail(),
                    actor.getId(),
                    "Recepcion iniciada para cita " + appointmentId
            );
            return appointmentMapper.toResponseDTO(saved);
        } catch (RuntimeException ex) {
            logService.logFailure(
                    LogServiceName.RECEPTION,
                    LogActionType.INITIATE_RECEPTION,
                    actor.getEmail(),
                    actor.getId(),
                    ex.getMessage()
            );
            throw ex;
        }
    }

    @Override
    @Transactional
    public AppointmentResponseDTO confirmReception(ConfirmReceptionDTO dto) {
        UserEntity actor = currentUserService.getAuthenticatedUser();
        try {
            AppointmentEntity appointment = appointmentRepository
                    .findFirstByVehicleLicensePlateOrderByAppointmentDateDescStartTimeDesc(dto.licensePlate())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "No se encontro una cita para la placa: " + dto.licensePlate()));

            if (appointment.getStatus() != AppointmentStatus.AWAITING_CONFIRMATION) {
                throw new AppointmentNotEligibleForReceptionException(appointment.getId());
            }

            if (appointment.getVerificationCode() == null || !appointment.getVerificationCode().equals(dto.code())) {
                throw new InvalidVerificationCodeException("Codigo de verificacion incorrecto.");
            }

            if (appointment.getVerificationCodeCreatedAt() == null ||
                    appointment.getVerificationCodeCreatedAt().plusMinutes(CODE_EXPIRATION_MINUTES).isBefore(LocalDateTime.now())) {
                throw new InvalidVerificationCodeException("El codigo de verificacion ya expiro.");
            }

            appointment.setStatus(AppointmentStatus.IN_PROGRESS);
            appointment.setProcessStartedAt(LocalDateTime.now());
            appointment.setVerificationCode(null);
            appointment.setVerificationCodeCreatedAt(null);

            AppointmentEntity saved = appointmentRepository.save(appointment);
            logService.logSuccess(
                    LogServiceName.RECEPTION,
                    LogActionType.CONFIRM_RECEPTION,
                    actor.getEmail(),
                    actor.getId(),
                    "Recepcion confirmada para placa " + dto.licensePlate()
            );
            return appointmentMapper.toResponseDTO(saved);
        } catch (RuntimeException ex) {
            logService.logFailure(
                    LogServiceName.RECEPTION,
                    LogActionType.CONFIRM_RECEPTION,
                    actor.getEmail(),
                    actor.getId(),
                    ex.getMessage()
            );
            throw ex;
        }
    }

    private String generateCode() {
        int number = 1000 + SECURE_RANDOM.nextInt(9000);
        return String.valueOf(number);
    }
}


