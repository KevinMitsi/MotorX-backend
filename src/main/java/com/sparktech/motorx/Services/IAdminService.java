package com.sparktech.motorx.Services;


import com.sparktech.motorx.dto.appointment.CancelAppointmentRequestDTO;
import com.sparktech.motorx.dto.appointment.CreateUnplannedAppointmentRequestDTO;
import com.sparktech.motorx.dto.appointment.UpdateAppointmentTechnicianRequestDTO;
import com.sparktech.motorx.dto.appointment.AppointmentResponseDTO;
import com.sparktech.motorx.dto.appointment.AvailableSlotsResponseDTO;
import com.sparktech.motorx.entity.AppointmentType;

import java.time.LocalDate;
import java.util.List;

/**
 * Servicio de acciones administrativas sobre citas.
 * El admin tiene visibilidad total y puede crear citas no planeadas,
 * cambiar técnicos, cancelar cualquier cita y ver la agenda completa.
 */
public interface IAdminService {

    /**
     * Agenda del día: todas las citas de una fecha específica.
     */
    List<AppointmentResponseDTO> getDailyAgenda(LocalDate date);

    /**
     * Vista de calendario: citas en un rango de fechas.
     */
    List<AppointmentResponseDTO> getCalendarView(LocalDate start, LocalDate end);

    /**
     * Consulta de disponibilidad para cualquier fecha y tipo.
     */
    AvailableSlotsResponseDTO getAvailableSlots(LocalDate date, AppointmentType type);

    /**
     * Registra una cita no planeada (cuando hay espacio disponible y
     * el técnico puede atender sin cita previa).
     */
    AppointmentResponseDTO registerUnplannedAppointment(CreateUnplannedAppointmentRequestDTO request);

    /**
     * Cancela cualquier cita. El admin puede elegir si notificar o no al cliente.
     */
    AppointmentResponseDTO cancelAppointment(Long appointmentId, CancelAppointmentRequestDTO request);

    /**
     * Cambia el técnico asignado a una cita (sin modificar el horario).
     * Valida disponibilidad del nuevo técnico.
     */
    AppointmentResponseDTO changeTechnician(Long appointmentId, UpdateAppointmentTechnicianRequestDTO request);

    /**
     * Detalle de una cita específica.
     */
    AppointmentResponseDTO getAppointmentById(Long appointmentId);

    /**
     * Historial de citas de un cliente.
     */
    List<AppointmentResponseDTO> getClientAppointmentHistory(Long clientId);

    /**
     * Historial de citas de un vehículo específico.
     */
    List<AppointmentResponseDTO> getVehicleAppointmentHistory(Long vehicleId);
}