package com.sparktech.motorx.Services;

import com.sparktech.motorx.dto.appointment.CreateAppointmentRequestDTO;
import com.sparktech.motorx.dto.appointment.AppointmentResponseDTO;
import com.sparktech.motorx.dto.appointment.AvailableSlotsResponseDTO;
import com.sparktech.motorx.dto.appointment.LicensePlateRestrictionResponseDTO;
import com.sparktech.motorx.dto.appointment.ReworkRedirectResponseDTO;
import com.sparktech.motorx.entity.AppointmentType;

import java.time.LocalDate;
import java.util.List;
import com.sparktech.motorx.dto.auth.RegisterUserDTO;
import com.sparktech.motorx.dto.user.UpdateUserRequestDTO;

public interface IUserService {

    void register(RegisterUserDTO request);

    void updateUserDTO(Long userId, UpdateUserRequestDTO userUpdate);

    /**
     * Consulta los slots disponibles para una fecha y tipo de cita.
     * Si el tipo es REWORK, devuelve la respuesta de redirección.
     */
    AvailableSlotsResponseDTO getAvailableSlots(LocalDate date, AppointmentType type);

    /**
     * Verifica si un vehículo del usuario tiene pico y placa en la fecha dada.
     * Se llama ANTES de mostrar los slots para informar al cliente con anticipación.
     */
    LicensePlateRestrictionResponseDTO checkLicensePlateRestriction(Long vehicleId, LocalDate date);

    /**
     * Obtiene la información de redirección para reprocesos.
     * El sistema nunca permite agendar un reproceso online.
     */
    ReworkRedirectResponseDTO getReworkRedirectInfo();

    /**
     * Agenda una nueva cita para el cliente autenticado.
     * Extrae el usuario del SecurityContextHolder.
     */
    AppointmentResponseDTO scheduleAppointment(CreateAppointmentRequestDTO request);

    /**
     * Cancela una cita del cliente autenticado.
     * Solo puede cancelar sus propias citas.
     */
    AppointmentResponseDTO cancelMyAppointment(Long appointmentId);

    /**
     * Retorna el historial de citas del cliente autenticado (por todos sus vehículos).
     */
    List<AppointmentResponseDTO> getMyAppointmentHistory();

    /**
     * Retorna el historial de citas de un vehículo específico del cliente.
     */
    List<AppointmentResponseDTO> getMyVehicleAppointments(Long vehicleId);

    /**
     * Retorna el detalle de una cita específica del cliente autenticado.
     */
    AppointmentResponseDTO getMyAppointmentById(Long appointmentId);

}
