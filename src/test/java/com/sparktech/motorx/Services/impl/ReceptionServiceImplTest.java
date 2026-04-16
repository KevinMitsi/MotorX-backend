package com.sparktech.motorx.Services.impl;

import com.sparktech.motorx.Services.IEmailNotificationService;
import com.sparktech.motorx.Services.ICurrentUserService;
import com.sparktech.motorx.Services.ILogService;
import com.sparktech.motorx.dto.appointment.AppointmentResponseDTO;
import com.sparktech.motorx.dto.notification.EmailDTO;
import com.sparktech.motorx.dto.reception.ConfirmReceptionDTO;
import com.sparktech.motorx.entity.*;
import com.sparktech.motorx.exception.AppointmentNotEligibleForReceptionException;
import com.sparktech.motorx.exception.AppointmentNotFoundException;
import com.sparktech.motorx.exception.InvalidVerificationCodeException;
import com.sparktech.motorx.mapper.AppointmentMapper;
import com.sparktech.motorx.repository.JpaAppointmentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReceptionServiceImpl - Unit Tests")
class ReceptionServiceImplTest {

    @Mock
    private JpaAppointmentRepository appointmentRepository;
    @Mock
    private AppointmentMapper appointmentMapper;
    @Mock
    private IEmailNotificationService emailNotificationService;
    @Mock
    private ICurrentUserService currentUserService;
    @Mock
    private ILogService logService;

    @InjectMocks
    private ReceptionServiceImpl sut;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        UserEntity actor = new UserEntity();
        actor.setId(55L);
        actor.setEmail("reception@test.com");
        when(currentUserService.getAuthenticatedUser()).thenReturn(actor);
    }

    @Test
    @DisplayName("initiateReception lanza AppointmentNotFoundException cuando no existe")
    void initiateReceptionShouldThrowWhenAppointmentMissing() {
        when(appointmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.initiateReception(99L)).isInstanceOf(AppointmentNotFoundException.class);
    }

    @Test
    @DisplayName("initiateReception valida estado SCHEDULED")
    void initiateReceptionShouldFailWhenStatusInvalid() {
        AppointmentEntity appointment = appointment(AppointmentStatus.IN_PROGRESS);
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));

        assertThatThrownBy(() -> sut.initiateReception(1L)).isInstanceOf(AppointmentNotEligibleForReceptionException.class);
    }

    @Test
    @DisplayName("initiateReception genera código y envía correo")
    void initiateReceptionShouldSetCodeAndSendEmail() {
        AppointmentEntity appointment = appointment(AppointmentStatus.SCHEDULED);

        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(appointment)).thenReturn(appointment);
        when(appointmentMapper.toResponseDTO(appointment)).thenReturn(mock(AppointmentResponseDTO.class));

        sut.initiateReception(1L);

        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.AWAITING_CONFIRMATION);
        assertThat(appointment.getVerificationCode()).matches("\\d{4}");
        assertThat(appointment.getVerificationCodeCreatedAt()).isNotNull();

        ArgumentCaptor<EmailDTO> captor = ArgumentCaptor.forClass(EmailDTO.class);
        verify(emailNotificationService).sendMail(captor.capture());
        assertThat(captor.getValue().recipient()).isEqualTo("client@test.com");
        verify(logService).logSuccess(eq(LogServiceName.RECEPTION), eq(LogActionType.INITIATE_RECEPTION), eq("reception@test.com"), eq(55L), contains("iniciada"));
    }

    @Test
    @DisplayName("confirmReception lanza IllegalArgumentException cuando no hay cita por placa")
    void confirmReceptionShouldThrowWhenLicensePlateNotFound() {
        when(appointmentRepository.findFirstByVehicleLicensePlateOrderByAppointmentDateDescStartTimeDesc("ABC123")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.confirmReception(new ConfirmReceptionDTO("ABC123", "1234")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No se encontro una cita");
    }

    @Test
    @DisplayName("confirmReception falla si la cita no está esperando confirmación")
    void confirmReceptionShouldFailWhenStatusInvalid() {
        AppointmentEntity appointment = appointment(AppointmentStatus.SCHEDULED);
        when(appointmentRepository.findFirstByVehicleLicensePlateOrderByAppointmentDateDescStartTimeDesc("ABC123")).thenReturn(Optional.of(appointment));

        assertThatThrownBy(() -> sut.confirmReception(new ConfirmReceptionDTO("ABC123", "1234")))
                .isInstanceOf(AppointmentNotEligibleForReceptionException.class);
    }

    @Test
    @DisplayName("confirmReception falla cuando el código no coincide")
    void confirmReceptionShouldFailWhenCodeDoesNotMatch() {
        AppointmentEntity appointment = appointmentAwaiting("1234", LocalDateTime.now());
        when(appointmentRepository.findFirstByVehicleLicensePlateOrderByAppointmentDateDescStartTimeDesc("ABC123")).thenReturn(Optional.of(appointment));

        assertThatThrownBy(() -> sut.confirmReception(new ConfirmReceptionDTO("ABC123", "9999")))
                .isInstanceOf(InvalidVerificationCodeException.class)
                .hasMessageContaining("incorrecto");
        verify(logService).logFailure(eq(LogServiceName.RECEPTION), eq(LogActionType.CONFIRM_RECEPTION), eq("reception@test.com"), eq(55L), contains("incorrecto"));
    }

    @Test
    @DisplayName("confirmReception falla cuando el código está expirado")
    void confirmReceptionShouldFailWhenCodeExpired() {
        AppointmentEntity appointment = appointmentAwaiting("1234", LocalDateTime.now().minusMinutes(16));
        when(appointmentRepository.findFirstByVehicleLicensePlateOrderByAppointmentDateDescStartTimeDesc("ABC123")).thenReturn(Optional.of(appointment));

        assertThatThrownBy(() -> sut.confirmReception(new ConfirmReceptionDTO("ABC123", "1234")))
                .isInstanceOf(InvalidVerificationCodeException.class)
                .hasMessageContaining("expiro");
    }

    @Test
    @DisplayName("confirmReception cambia estado a IN_PROGRESS y limpia código")
    void confirmReceptionShouldMoveToInProgress() {
        AppointmentEntity appointment = appointmentAwaiting("1234", LocalDateTime.now().minusMinutes(1));

        when(appointmentRepository.findFirstByVehicleLicensePlateOrderByAppointmentDateDescStartTimeDesc("ABC123")).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(appointment)).thenReturn(appointment);
        when(appointmentMapper.toResponseDTO(appointment)).thenReturn(mock(AppointmentResponseDTO.class));

        sut.confirmReception(new ConfirmReceptionDTO("ABC123", "1234"));

        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.IN_PROGRESS);
        assertThat(appointment.getProcessStartedAt()).isNotNull();
        assertThat(appointment.getVerificationCode()).isNull();
        assertThat(appointment.getVerificationCodeCreatedAt()).isNull();
        verify(logService).logSuccess(eq(LogServiceName.RECEPTION), eq(LogActionType.CONFIRM_RECEPTION), eq("reception@test.com"), eq(55L), contains("confirmada"));
    }

    private AppointmentEntity appointment(AppointmentStatus status) {
        UserEntity owner = new UserEntity();
        owner.setId(10L);
        owner.setName("Cliente");
        owner.setEmail("client@test.com");

        VehicleEntity vehicle = new VehicleEntity();
        vehicle.setId(5L);
        vehicle.setOwner(owner);
        vehicle.setLicensePlate("ABC123");
        vehicle.setBrand("Yamaha");
        vehicle.setModel("FZ");

        AppointmentEntity appointment = new AppointmentEntity();
        appointment.setId(1L);
        appointment.setStatus(status);
        appointment.setVehicle(vehicle);
        appointment.setAppointmentType(AppointmentType.MAINTENANCE);
        appointment.setAppointmentDate(LocalDate.now());
        appointment.setStartTime(LocalTime.of(9, 0));
        appointment.setEndTime(LocalTime.of(10, 0));
        appointment.setCurrentMileage(1000);
        return appointment;
    }

    private AppointmentEntity appointmentAwaiting(String code, LocalDateTime createdAt) {
        AppointmentEntity appointment = appointment(AppointmentStatus.AWAITING_CONFIRMATION);
        appointment.setVerificationCode(code);
        appointment.setVerificationCodeCreatedAt(createdAt);
        return appointment;
    }
}

