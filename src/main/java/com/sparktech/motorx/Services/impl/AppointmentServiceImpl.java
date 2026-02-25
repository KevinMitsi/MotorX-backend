package com.sparktech.motorx.Services.impl;

import com.sparktech.motorx.config.AppointmentScheduleConfig;
import com.sparktech.motorx.dto.appointment.CancelAppointmentRequestDTO;
import com.sparktech.motorx.dto.appointment.CreateAppointmentRequestDTO;
import com.sparktech.motorx.dto.appointment.CreateUnplannedAppointmentRequestDTO;
import com.sparktech.motorx.dto.appointment.UpdateAppointmentTechnicianRequestDTO;
import com.sparktech.motorx.dto.appointment.AppointmentResponseDTO;
import com.sparktech.motorx.dto.appointment.AvailableSlotsResponseDTO;
import com.sparktech.motorx.entity.*;
import com.sparktech.motorx.exception.*;
import com.sparktech.motorx.mapper.AppointmentMapper;
import com.sparktech.motorx.repository.JpaAppointmentRepository;
import com.sparktech.motorx.repository.JpaEmployeeRepository;

import com.sparktech.motorx.repository.JpaVehicleRepository;
import com.sparktech.motorx.Services.IAppointmentService;
import com.sparktech.motorx.Services.IEmailNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AppointmentServiceImpl implements IAppointmentService {

    private final JpaAppointmentRepository appointmentRepository;
    private final JpaEmployeeRepository technicianRepository;
    private final JpaVehicleRepository vehicleRepository;
    private final AppointmentMapper appointmentMapper;
    private final IEmailNotificationService notificationService;

    // ---------------------------------------------------------------
    // CONSULTA DE DISPONIBILIDAD
    // ---------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public AvailableSlotsResponseDTO getAvailableSlots(LocalDate date, AppointmentType type) {
        validateWorkingDay(date);

        List<LocalTime> candidateSlots = AppointmentScheduleConfig.VALID_SLOTS_BY_TYPE
                .getOrDefault(type, List.of());

        List<EmployeeEntity> activeTechnicians = technicianRepository.findAllActive();
        List<AvailableSlotsResponseDTO.AvailableSlotDTO> availableSlots = new ArrayList<>();

        for (LocalTime slotStart : candidateSlots) {
            LocalTime slotEnd = resolveEndTime(type, slotStart);
            int freeTechnicians = countFreeTechniciansForSlot(activeTechnicians, date, slotStart, slotEnd);

            if (freeTechnicians > 0) {
                availableSlots.add(new AvailableSlotsResponseDTO.AvailableSlotDTO(
                        slotStart,
                        slotEnd,
                        freeTechnicians
                ));
            }
        }

        return new AvailableSlotsResponseDTO(date, type, availableSlots);
    }

    // ---------------------------------------------------------------
    // CREACIÓN DE CITA (CLIENTE)
    // ---------------------------------------------------------------

    @Override
    @Transactional
    public AppointmentResponseDTO createAppointment(CreateAppointmentRequestDTO request, Long clientId) {

        // 1. El tipo REWORK nunca se agenda online
        if (request.appointmentType() == AppointmentType.REWORK) {
            throw new ReworkNotBookableOnlineException();
        }

        // 2. El tipo UNPLANNED solo lo puede crear el admin (este method es para clientes)
        if (request.appointmentType() == AppointmentType.UNPLANNED) {
            throw new InvalidAppointmentSlotException(
                    "Las citas no planeadas solo pueden ser creadas por el administrador."
            );
        }

        // 3. Solo tipos bookables por el usuario
        if (!AppointmentScheduleConfig.USER_BOOKABLE_TYPES.contains(request.appointmentType())) {
            throw new InvalidAppointmentSlotException(
                    "El tipo de cita seleccionado no está disponible para agendamiento en línea."
            );
        }

        VehicleEntity vehicle = vehicleRepository.findById(request.vehicleId())
                .orElseThrow(() -> new AppointmentException(
                        "No se encontró el vehículo con ID: " + request.vehicleId()
                ));

        // 4. Verificar que el vehículo pertenece al cliente autenticado
        if (!vehicle.getOwner().getId().equals(clientId)) {
            throw new AppointmentException("El vehículo no pertenece al usuario autenticado.");
        }

        // 5. Pico y placa
        if (hasLicensePlateRestriction(vehicle.getLicensePlate(), request.appointmentDate())) {
            throw new LicensePlateRestrictionException(
                    "La moto con placa " + vehicle.getLicensePlate() +
                            " tiene restricción de movilidad el " + request.appointmentDate() +
                            ". No es posible agendar la cita para ese día."
            );
        }

        // 6. Validar que la marca es compatible con el tipo de cita
        validateBrandCompatibility(vehicle.getBrand(), request.appointmentType());

        // 7. Validar que el slot horario es válido para el tipo
        validateSlotForType(request.startTime(), request.appointmentType());

        // 8. Validar que no estamos en horario de almuerzo ni fuera del horario laboral
        validateWithinBusinessHours(request.startTime());

        // 9. Asignar técnico automáticamente (rotación)
        LocalTime endTime = resolveEndTime(request.appointmentType(), request.startTime());
        EmployeeEntity assignedTechnician = assignTechnicianAutomatically(
                request.appointmentDate(), request.startTime(), endTime
        );

        // 10. Persistir
        String clientNotesStr = (request.clientNotes() != null && !request.clientNotes().isEmpty())
                ? String.join("; ", request.clientNotes())
                : null;

        AppointmentEntity appointment = AppointmentEntity.builder()
                .vehicle(vehicle)
                .appointmentType(request.appointmentType())
                .appointmentDate(request.appointmentDate())
                .startTime(request.startTime())
                .endTime(endTime)
                .technician(assignedTechnician)
                .status(AppointmentStatus.SCHEDULED)
                .currentMileage(request.currentMileage())
                .clientNotes(clientNotesStr)
                .build();

        AppointmentEntity saved = appointmentRepository.save(appointment);

        // 11. Notificar al cliente (siempre al crear)
        notificationService.sendAppointmentCreatedNotification(saved);

        return appointmentMapper.toResponseDTO(saved);
    }

    // ---------------------------------------------------------------
    // CREACIÓN DE CITA NO PLANEADA (SOLO ADMIN)
    // ---------------------------------------------------------------

    @Override
    @Transactional
    public AppointmentResponseDTO createUnplannedAppointment(CreateUnplannedAppointmentRequestDTO request) {

        VehicleEntity vehicle = vehicleRepository.findById(request.vehicleId())
                .orElseThrow(() -> new AppointmentException(
                        "No se encontró el vehículo con ID: " + request.vehicleId()
                ));

        // Pico y placa aplica también para citas no planeadas
        if (hasLicensePlateRestriction(vehicle.getLicensePlate(), request.appointmentDate())) {
            throw new LicensePlateRestrictionException(
                    "La moto con placa " + vehicle.getLicensePlate() +
                            " tiene restricción de movilidad el " + request.appointmentDate() + "."
            );
        }

        validateWithinBusinessHours(request.startTime());

        LocalTime endTime = resolveEndTime(request.appointmentType(), request.startTime());

        EmployeeEntity technician;
        if (request.technicianId() != null) {
            // Admin asignó técnico manualmente → validar que esté libre
            technician = technicianRepository.findById(request.technicianId())
                    .orElseThrow(() -> new AppointmentException(
                            "No se encontró el técnico con ID: " + request.technicianId()
                    ));
            boolean isBusy = appointmentRepository.existsTechnicianConflict(
                    technician.getId(), request.appointmentDate(), request.startTime(), endTime
            );
            if (isBusy) {
                throw new TechnicianSlotOccupiedException(
                        "El técnico seleccionado ya tiene una cita en ese horario."
                );
            }
        } else {
            // Asignación automática
            technician = assignTechnicianAutomatically(
                    request.appointmentDate(), request.startTime(), endTime
            );
        }

        AppointmentEntity appointment = AppointmentEntity.builder()
                .vehicle(vehicle)
                .appointmentType(AppointmentType.UNPLANNED)
                .appointmentDate(request.appointmentDate())
                .startTime(request.startTime())
                .endTime(endTime)
                .technician(technician)
                .status(AppointmentStatus.SCHEDULED)
                .adminNotes(request.adminNotes())
                .build();

        AppointmentEntity saved = appointmentRepository.save(appointment);
        return appointmentMapper.toResponseDTO(saved);
    }

    // ---------------------------------------------------------------
    // CANCELACIÓN (ADMIN)
    // ---------------------------------------------------------------

    @Override
    @Transactional
    public AppointmentResponseDTO cancelAppointment(Long appointmentId, CancelAppointmentRequestDTO request) {
        AppointmentEntity appointment = findAppointmentOrThrow(appointmentId);

        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new AppointmentException("La cita ya se encuentra cancelada.");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setCancellationReason(request.reason());
        AppointmentEntity saved = appointmentRepository.save(appointment);

        if (request.notifyClient()) {
            notificationService.sendAppointmentCancelledNotification(saved, request.reason());
        }

        return appointmentMapper.toResponseDTO(saved);
    }

    // ---------------------------------------------------------------
    // CAMBIO DE TÉCNICO (ADMIN)
    // ---------------------------------------------------------------

    @Override
    @Transactional
    public AppointmentResponseDTO updateTechnician(Long appointmentId, UpdateAppointmentTechnicianRequestDTO request) {
        AppointmentEntity appointment = findAppointmentOrThrow(appointmentId);

        EmployeeEntity newTechnician = technicianRepository.findById(request.newTechnicianId())
                .orElseThrow(() -> new AppointmentException(
                        "No se encontró el técnico con ID: " + request.newTechnicianId()
                ));

        // Verificar que el nuevo técnico tiene libre ese slot
        boolean isBusy = appointmentRepository.existsTechnicianConflict(
                newTechnician.getId(),
                appointment.getAppointmentDate(),
                appointment.getStartTime(),
                appointment.getEndTime()
        );

        if (isBusy) {
            throw new TechnicianSlotOccupiedException(
                    "El técnico " + newTechnician.getUser().getName() +
                            " ya tiene ocupado ese horario. Elige otro técnico."
            );
        }

        appointment.setTechnician(newTechnician);
        AppointmentEntity saved = appointmentRepository.save(appointment);

        if (request.notifyClient()) {
            notificationService.sendAppointmentUpdatedNotification(saved);
        }

        return appointmentMapper.toResponseDTO(saved);
    }

    // ---------------------------------------------------------------
    // CONSULTAS
    // ---------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public AppointmentResponseDTO getAppointmentById(Long appointmentId) {
        return appointmentMapper.toResponseDTO(findAppointmentOrThrow(appointmentId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponseDTO> getAppointmentsByVehicle(Long vehicleId) {
        return appointmentRepository.findByVehicleIdOrderByAppointmentDateDesc(vehicleId)
                .stream()
                .map(appointmentMapper::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponseDTO> getAppointmentsByClient(Long clientId) {
        return appointmentRepository.findByClientIdOrderByDateDesc(clientId)
                .stream()
                .map(appointmentMapper::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponseDTO> getAppointmentsByDate(LocalDate date) {
        return appointmentRepository.findByAppointmentDateOrderByStartTime(date)
                .stream()
                .map(appointmentMapper::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponseDTO> getAppointmentsByDateRange(LocalDate start, LocalDate end) {
        return appointmentRepository.findByDateRange(start, end)
                .stream()
                .map(appointmentMapper::toResponseDTO)
                .toList();
    }

    // ---------------------------------------------------------------
    // PICO Y PLACA
    // Restricción de Colombia: dos dígitos finales de la placa por día.
    // Lunes: 1-2 | Martes: 3-4 | Miércoles: 5-6 | Jueves: 7-8 | Viernes: 9-0
    // ---------------------------------------------------------------

    @Override
    public boolean hasLicensePlateRestriction(String plate, LocalDate date) {
        if (plate == null || plate.isBlank()) return false;

        DayOfWeek dayOfWeek = date.getDayOfWeek();

        // Fines de semana no hay pico y placa
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            return false;
        }

        // Último dígito de la placa
        String cleanPlate = plate.trim().toUpperCase();
        char lastChar = cleanPlate.charAt(cleanPlate.length() - 1);
        if (!Character.isDigit(lastChar)) return false;
        int lastDigit = Character.getNumericValue(lastChar);

        Map<DayOfWeek, List<Integer>> restrictions = Map.of(
                DayOfWeek.MONDAY,    List.of(1, 2),
                DayOfWeek.TUESDAY,   List.of(3, 4),
                DayOfWeek.WEDNESDAY, List.of(5, 6),
                DayOfWeek.THURSDAY,  List.of(7, 8),
                DayOfWeek.FRIDAY,    List.of(9, 0)
        );

        return restrictions.getOrDefault(dayOfWeek, List.of()).contains(lastDigit);
    }

    // ---------------------------------------------------------------
    // HELPERS PRIVADOS
    // ---------------------------------------------------------------

    /**
     * Asigna automáticamente el primer técnico disponible para el slot.
     * Orden: técnico A → técnico B → técnico C... hasta encontrar uno libre.
     * Si ninguno está libre, lanza excepción.
     */
    private EmployeeEntity assignTechnicianAutomatically(
            LocalDate date, LocalTime startTime, LocalTime endTime) {

        List<EmployeeEntity> activeTechnicians = technicianRepository.findAllActive();

        for (EmployeeEntity technician : activeTechnicians) {
            boolean isBusy = appointmentRepository.existsTechnicianConflict(
                    technician.getId(), date, startTime, endTime
            );
            if (!isBusy) {
                return technician;
            }
        }

        throw new NoAvailableTechnicianException(
                "No hay técnicos disponibles para el horario " + startTime +
                        " el día " + date + ". Por favor elige otro día u horario."
        );
    }

    /**
     * Cuenta cuántos técnicos tienen libre un slot específico.
     */
    private int countFreeTechniciansForSlot(
            List<EmployeeEntity> technicians, LocalDate date,
            LocalTime startTime, LocalTime endTime) {

        int count = 0;
        for (EmployeeEntity tech : technicians) {
            boolean busy = appointmentRepository.existsTechnicianConflict(
                    tech.getId(), date, startTime, endTime
            );
            if (!busy) count++;
        }
        return count;
    }

    /**
     * Valida que el slot de hora solicitado sea un horario válido para ese tipo de cita.
     */
    private void validateSlotForType(LocalTime requestedSlot, AppointmentType type) {
        List<LocalTime> validSlots = AppointmentScheduleConfig.VALID_SLOTS_BY_TYPE
                .getOrDefault(type, List.of());

        boolean isValid = validSlots.stream().anyMatch(s -> s.equals(requestedSlot));
        if (!isValid) {
            throw new InvalidAppointmentSlotException(
                    "El horario " + requestedSlot + " no es válido para el tipo de cita " + type +
                            ". Horarios válidos: " + validSlots
            );
        }
    }

    /**
     * Valida que el horario no esté en horario de almuerzo ni fuera del horario laboral.
     */
    private void validateWithinBusinessHours(LocalTime startTime) {
        if (startTime.isBefore(AppointmentScheduleConfig.WORK_START) ||
                startTime.isAfter(AppointmentScheduleConfig.WORK_END)) {
            throw new AppointmentOutsideBusinessHoursException(
                    "El horario " + startTime + " está fuera del horario laboral (7:00 AM - 5:30 PM)."
            );
        }

        // No se reciben motos en horario de almuerzo
        if (!startTime.isBefore(AppointmentScheduleConfig.LUNCH_START) &&
                startTime.isBefore(AppointmentScheduleConfig.LUNCH_END)) {
            throw new AppointmentOutsideBusinessHoursException(
                    "No se reciben citas durante el horario de almuerzo (12:00 PM - 1:00 PM)."
            );
        }
    }

    /**
     * Valida que la marca del vehículo sea compatible con el tipo de cita.
     * MANUAL_WARRANTY_REVIEW y AUTECO_WARRANTY solo aplican para Auteco.
     */
    private void validateBrandCompatibility(String brand, AppointmentType type) {
        if (AppointmentScheduleConfig.AUTECO_ONLY_TYPES.contains(type) &&
                (brand == null || !brand.equalsIgnoreCase("AUTECO"))) {
            throw new AppointmentTypeNotAllowedForBrandException(
                    "El tipo de cita " + type + " solo aplica para motos de marca Auteco. " +
                            "La moto registrada es de marca: " + brand
            );
        }
    }

    /**
     * Valida que la fecha sea un día laboral (no festivo o fin de semana).
     * Por ahora valida fines de semana; los festivos se pueden agregar luego.
     */
    private void validateWorkingDay(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            throw new AppointmentOutsideBusinessHoursException(
                    "No se pueden agendar citas los fines de semana."
            );
        }
    }

    /**
     * Resuelve la hora de fin según el tipo de cita.
     * Los cambios de aceite duran 30 minutos; el resto ocupa el slot hasta la siguiente recepción.
     * Para efectos del sistema, se asigna una duración estimada estándar por tipo.
     */
    private LocalTime resolveEndTime(AppointmentType type, LocalTime startTime) {
        return switch (type) {
            case OIL_CHANGE -> startTime.plusMinutes(AppointmentScheduleConfig.OIL_CHANGE_DURATION_MINUTES);
            case QUICK_SERVICE, UNPLANNED -> startTime.plusMinutes(60);
            case MANUAL_WARRANTY_REVIEW -> startTime.plusMinutes(45);
            case AUTECO_WARRANTY, REWORK -> startTime.plusMinutes(120);
            case MAINTENANCE -> startTime.plusMinutes(180);
        };
    }

    private AppointmentEntity findAppointmentOrThrow(Long id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new AppointmentNotFoundException(id));
    }
}
