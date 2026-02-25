package com.sparktech.motorx.Services;



import com.sparktech.motorx.dto.appointment.CancelAppointmentRequestDTO;
import com.sparktech.motorx.dto.appointment.CreateAppointmentRequestDTO;
import com.sparktech.motorx.dto.appointment.CreateUnplannedAppointmentRequestDTO;
import com.sparktech.motorx.dto.appointment.UpdateAppointmentTechnicianRequestDTO;
import com.sparktech.motorx.dto.appointment.AppointmentResponseDTO;
import com.sparktech.motorx.dto.appointment.AvailableSlotsResponseDTO;
import com.sparktech.motorx.entity.AppointmentType;

import java.time.LocalDate;
import java.util.List;

/**
 * Servicio central de citas.
 * Contiene la lógica compartida que usan tanto UserService como AdminService.
 * Toda operación real contra la BD de citas pasa por aquí.
 */
public interface IAppointmentService {

    /**
     * Consulta los slots disponibles para una fecha y tipo de cita.
     * Un slot está disponible si al menos un técnico tiene ese horario libre.
     * Aplica las reglas de horario de recepción y horario laboral.
     */
    AvailableSlotsResponseDTO getAvailableSlots(LocalDate date, AppointmentType type);

    /**
     * Crea una cita asignando automáticamente el técnico disponible.
     * Aplica todas las validaciones de negocio:
     * - Horario válido para el tipo
     * - No pico y placa
     * - Marca compatible con el tipo
     * - Técnico disponible (rotación automática)
     */
    AppointmentResponseDTO createAppointment(CreateAppointmentRequestDTO request, Long clientId);

    /**
     * Crea una cita no planeada. Solo disponible para el admin.
     * Permite asignar técnico manualmente y usar horarios fuera de recepción.
     */
    AppointmentResponseDTO createUnplannedAppointment(CreateUnplannedAppointmentRequestDTO request);

    /**
     * Cancela una cita. El admin puede elegir si notificar al cliente.
     */
    AppointmentResponseDTO cancelAppointment(Long appointmentId, CancelAppointmentRequestDTO request);

    /**
     * Cambia el técnico asignado a una cita (sin cambiar el horario).
     * Valida que el nuevo técnico tenga ese slot libre.
     */
    AppointmentResponseDTO updateTechnician(Long appointmentId, UpdateAppointmentTechnicianRequestDTO request);

    /**
     * Consulta una cita por ID.
     */
    AppointmentResponseDTO getAppointmentById(Long appointmentId);

    /**
     * Lista las citas de un vehículo (historial).
     */
    List<AppointmentResponseDTO> getAppointmentsByVehicle(Long vehicleId);

    /**
     * Lista las citas de un cliente a través de sus vehículos.
     */
    List<AppointmentResponseDTO> getAppointmentsByClient(Long clientId);

    /**
     * Lista todas las citas de un día (vista de agenda para el admin).
     */
    List<AppointmentResponseDTO> getAppointmentsByDate(LocalDate date);

    /**
     * Lista citas en un rango de fechas (vista de calendario).
     */
    List<AppointmentResponseDTO> getAppointmentsByDateRange(LocalDate start, LocalDate end);

    /**
     * Verifica si una moto tiene pico y placa en la fecha dada.
     * La lógica de pico y placa se calcula por el último dígito de la placa.
     */
    boolean hasLicensePlateRestriction(String plate, LocalDate date);
}