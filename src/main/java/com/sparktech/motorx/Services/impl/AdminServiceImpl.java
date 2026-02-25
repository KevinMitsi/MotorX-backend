package com.sparktech.motorx.Services.impl;

import com.sparktech.motorx.dto.appointment.CancelAppointmentRequestDTO;
import com.sparktech.motorx.dto.appointment.CreateUnplannedAppointmentRequestDTO;
import com.sparktech.motorx.dto.appointment.UpdateAppointmentTechnicianRequestDTO;
import com.sparktech.motorx.dto.appointment.AppointmentResponseDTO;
import com.sparktech.motorx.dto.appointment.AvailableSlotsResponseDTO;
import com.sparktech.motorx.entity.AppointmentType;
import com.sparktech.motorx.Services.IAdminService;
import com.sparktech.motorx.Services.IAppointmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements IAdminService {

    // El admin delega toda la lógica de citas al AppointmentService.
    // AdminServiceImpl no repite lógica, solo orquesta y añade
    // las acciones exclusivas del rol administrador.
    private final IAppointmentService appointmentService;

    // ---------------------------------------------------------------
    // VISIBILIDAD DE LA AGENDA
    // ---------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponseDTO> getDailyAgenda(LocalDate date) {
        return appointmentService.getAppointmentsByDate(date);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponseDTO> getCalendarView(LocalDate start, LocalDate end) {
        return appointmentService.getAppointmentsByDateRange(start, end);
    }

    @Override
    @Transactional(readOnly = true)
    public AvailableSlotsResponseDTO getAvailableSlots(LocalDate date, AppointmentType type) {
        return appointmentService.getAvailableSlots(date, type);
    }

    // ---------------------------------------------------------------
    // OPERACIONES EXCLUSIVAS DEL ADMIN
    // ---------------------------------------------------------------

    @Override
    @Transactional
    public AppointmentResponseDTO registerUnplannedAppointment(CreateUnplannedAppointmentRequestDTO request) {
        return appointmentService.createUnplannedAppointment(request);
    }

    @Override
    @Transactional
    public AppointmentResponseDTO cancelAppointment(Long appointmentId, CancelAppointmentRequestDTO request) {
        return appointmentService.cancelAppointment(appointmentId, request);
    }

    @Override
    @Transactional
    public AppointmentResponseDTO changeTechnician(Long appointmentId, UpdateAppointmentTechnicianRequestDTO request) {
        return appointmentService.updateTechnician(appointmentId, request);
    }

    // ---------------------------------------------------------------
    // CONSULTAS
    // ---------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public AppointmentResponseDTO getAppointmentById(Long appointmentId) {
        return appointmentService.getAppointmentById(appointmentId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponseDTO> getClientAppointmentHistory(Long clientId) {
        return appointmentService.getAppointmentsByClient(clientId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponseDTO> getVehicleAppointmentHistory(Long vehicleId) {
        return appointmentService.getAppointmentsByVehicle(vehicleId);
    }
}