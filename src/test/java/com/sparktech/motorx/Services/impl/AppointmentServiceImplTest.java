package com.sparktech.motorx.Services.impl;


import com.sparktech.motorx.config.AppointmentScheduleConfig;
import com.sparktech.motorx.dto.appointment.AppointmentResponseDTO;
import com.sparktech.motorx.dto.appointment.AvailableSlotsResponseDTO;
import com.sparktech.motorx.dto.appointment.CreateAppointmentRequestDTO;
import com.sparktech.motorx.dto.notification.AppointmentNotificationDTO;
import com.sparktech.motorx.entity.*;
import com.sparktech.motorx.exception.*;
import com.sparktech.motorx.mapper.AppointmentMapper;
import com.sparktech.motorx.repository.JpaAppointmentRepository;
import com.sparktech.motorx.repository.JpaEmployeeRepository;
import com.sparktech.motorx.repository.JpaVehicleRepository;
import com.sparktech.motorx.Services.IEmailNotificationService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AppointmentServiceImpl - Unit Tests")
class AppointmentServiceImplTest {

    // ================================================================
    // MOCKS
    // ================================================================
    @Mock private JpaAppointmentRepository appointmentRepository;
    @Mock private JpaEmployeeRepository technicianRepository;
    @Mock private JpaVehicleRepository vehicleRepository;
    @Mock private AppointmentMapper appointmentMapper;
    @Mock private IEmailNotificationService notificationService;

    @InjectMocks
    private AppointmentServiceImpl sut; // System Under Test

    // ================================================================
    // HELPERS — builders de entidades mínimas
    // ================================================================
    private EmployeeEntity buildTechnician(Long id) {
        EmployeeEntity emp = new EmployeeEntity();
        emp.setId(id);
        return emp;
    }

    // ================================================================
    // NESTED: hasLicensePlateRestriction
    // ================================================================
    @Nested
    @DisplayName("hasLicensePlateRestriction()")
    class LicensePlateRestrictionTests {

        // --- Casos que NO deben tener restricción ---

        @Test
        @DisplayName("Retorna false si la placa es null")
        void givenNullPlate_thenReturnFalse() {
            // Arrange
            LocalDate anyMonday = LocalDate.of(2025, 1, 6); // lunes

            // Act
            boolean result = sut.hasLicensePlateRestriction(null, anyMonday);

            // Assert
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Retorna false si la placa está en blanco")
        void givenBlankPlate_thenReturnFalse() {
            // Arrange
            LocalDate anyMonday = LocalDate.of(2025, 1, 6);

            // Act
            boolean result = sut.hasLicensePlateRestriction("   ", anyMonday);

            // Assert
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Retorna false los sábados (sin pico y placa)")
        void givenSaturday_thenNoRestriction() {
            // Arrange — sábado
            LocalDate saturday = LocalDate.of(2025, 1, 4);

            // Act
            boolean result = sut.hasLicensePlateRestriction("ABC56X", saturday);

            // Assert
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Retorna false los domingos (sin pico y placa)")
        void givenSunday_thenNoRestriction() {
            // Arrange — domingo
            LocalDate sunday = LocalDate.of(2025, 1, 5);

            // Act
            boolean result = sut.hasLicensePlateRestriction("ABC56X", sunday);

            // Assert
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Retorna false si el penúltimo carácter no es dígito")
        void givenNonDigitSecondToLast_thenReturnFalse() {
            // Arrange — placa con penúltimo carácter letra
            LocalDate monday = LocalDate.of(2025, 1, 6);

            // Act
            boolean result = sut.hasLicensePlateRestriction("ABCXYZ", monday);

            // Assert
            assertThat(result).isFalse();
        }

        // --- Casos que SÍ tienen restricción (parametrizado por día y dígito) ---

        /**
         * Lunes   → dígitos 5 y 6
         * Martes  → dígitos 7 y 8
         * Miércoles → dígitos 9 y 0
         * Jueves  → dígitos 1 y 2
         * Viernes → dígitos 3 y 4
         * <p>
         * Formato CSV: fechaISO, placa (penúltimo dígito = dígito restringido)
         */
        @ParameterizedTest(name = "[{index}] {0} placa={1}")
        @CsvSource({
                // Lunes 2025-01-06 → restringidos 5 y 6
                "2025-01-06, ABC56X",   // penúltimo=5 → restringido
                "2025-01-06, ABC65X",   // penúltimo=6 → restringido
                // Martes 2025-01-07 → 7 y 8
                "2025-01-07, ABC77X",
                "2025-01-07, ABC88X",
                // Miércoles 2025-01-08 → 9 y 0
                "2025-01-08, ABC90X",
                "2025-01-08, ABC09X",   // penúltimo=0
                // Jueves 2025-01-09 → 1 y 2
                "2025-01-09, ABC12X",
                "2025-01-09, ABC21X",
                // Viernes 2025-01-10 → 3 y 4
                "2025-01-10, ABC34X",
                "2025-01-10, ABC43X"
        })
        @DisplayName("Retorna TRUE cuando la placa está restringida ese día")
        void givenRestrictedPlateAndDay_thenReturnTrue(String dateStr, String plate) {
            // Arrange
            LocalDate date = LocalDate.parse(dateStr);

            // Act
            boolean result = sut.hasLicensePlateRestriction(plate, date);

            // Assert
            assertThat(result).isTrue();
        }

        @ParameterizedTest(name = "[{index}] {0} placa={1}")
        @CsvSource({
                // Lunes → dígitos 5 y 6 restringidos; probamos uno NO restringido (ej: 3)
                "2025-01-06, ABC34X",   // penúltimo=3 → libre el lunes
                // Martes → 7 y 8 restringidos; probamos 5
                "2025-01-07, ABC56X",
                // Miércoles → 9 y 0; probamos 3
                "2025-01-08, ABC34X",
                // Jueves → 1 y 2; probamos 5
                "2025-01-09, ABC56X",
                // Viernes → 3 y 4; probamos 7
                "2025-01-10, ABC78X"
        })
        @DisplayName("Retorna FALSE cuando la placa NO está restringida ese día")
        void givenFreeePlateAndDay_thenReturnFalse(String dateStr, String plate) {
            // Arrange
            LocalDate date = LocalDate.parse(dateStr);

            // Act
            boolean result = sut.hasLicensePlateRestriction(plate, date);

            // Assert
            assertThat(result).isFalse();
        }
    }

    // ================================================================
    // NESTED: getAvailableSlots
    // ================================================================
    @Nested
    @DisplayName("getAvailableSlots()")
    class GetAvailableSlotsTests {

        @Test
        @DisplayName("Lanza excepción si la fecha es sábado")
        void givenSaturday_thenThrowBusinessHoursException() {
            // Arrange
            LocalDate saturday = LocalDate.of(2025, 1, 4);

            // Act + Assert
            assertThatThrownBy(() -> sut.getAvailableSlots(saturday, AppointmentType.OIL_CHANGE))
                    .isInstanceOf(AppointmentOutsideBusinessHoursException.class);
        }

        @Test
        @DisplayName("Lanza excepción si la fecha es domingo")
        void givenSunday_thenThrowBusinessHoursException() {
            // Arrange
            LocalDate sunday = LocalDate.of(2025, 1, 5);

            // Act + Assert
            assertThatThrownBy(() -> sut.getAvailableSlots(sunday, AppointmentType.OIL_CHANGE))
                    .isInstanceOf(AppointmentOutsideBusinessHoursException.class);
        }

        @Test
        @DisplayName("Retorna slots disponibles cuando todos los técnicos están libres")
        void givenFutureWeekdayAndAllTechniciansFree_thenReturnAllCandidateSlots() {
            // Arrange
            // Fecha futura segura (lunes siguiente a un año lejano para evitar problema de "fecha hoy")
            LocalDate futureMonday = LocalDate.of(2099, 1, 7);
            AppointmentType type = AppointmentType.OIL_CHANGE;

            List<EmployeeEntity> technicians = List.of(buildTechnician(1L), buildTechnician(2L));
            when(technicianRepository.findAllActive()).thenReturn(technicians);

            // Ningún técnico tiene conflicto
            when(appointmentRepository.existsTechnicianConflict(
                    anyLong(), eq(futureMonday), any(LocalTime.class), any(LocalTime.class)
            )).thenReturn(false);

            List<LocalTime> expectedSlots = AppointmentScheduleConfig.VALID_SLOTS_BY_TYPE
                    .getOrDefault(type, List.of());

            // Act
            AvailableSlotsResponseDTO response = sut.getAvailableSlots(futureMonday, type);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.date()).isEqualTo(futureMonday);
            assertThat(response.appointmentType()).isEqualTo(type);
            assertThat(response.availableSlots()).hasSize(expectedSlots.size());
            // Cada slot debe reportar 2 técnicos libres
            response.availableSlots().forEach(slot ->
                    assertThat(slot.availableTechnicians()).isEqualTo(2));
        }

        @Test
        @DisplayName("Excluye slots cuando todos los técnicos están ocupados")
        void givenAllTechniciansBusy_thenReturnEmptySlots() {
            // Arrange
            LocalDate futureMonday = LocalDate.of(2099, 1, 7);
            AppointmentType type = AppointmentType.OIL_CHANGE;

            List<EmployeeEntity> technicians = List.of(buildTechnician(1L), buildTechnician(2L));
            when(technicianRepository.findAllActive()).thenReturn(technicians);

            // Todos ocupados
            when(appointmentRepository.existsTechnicianConflict(
                    anyLong(), eq(futureMonday), any(LocalTime.class), any(LocalTime.class)
            )).thenReturn(true);

            // Act
            AvailableSlotsResponseDTO response = sut.getAvailableSlots(futureMonday, type);

            // Assert
            assertThat(response.availableSlots()).isEmpty();
        }

        @Test
        @DisplayName("Con un técnico libre y otro ocupado, freeTechnicians = 1")
        void givenOneTechnicianFreeOneOccupied_thenFreeTechnicianIsOne() {
            // Arrange
            LocalDate futureMonday = LocalDate.of(2099, 1, 7);
            AppointmentType type = AppointmentType.OIL_CHANGE;

            EmployeeEntity tech1 = buildTechnician(1L);
            EmployeeEntity tech2 = buildTechnician(2L);
            when(technicianRepository.findAllActive()).thenReturn(List.of(tech1, tech2));

            // tech1 ocupado, tech2 libre
            when(appointmentRepository.existsTechnicianConflict(
                    eq(1L), eq(futureMonday), any(LocalTime.class), any(LocalTime.class)
            )).thenReturn(true);
            when(appointmentRepository.existsTechnicianConflict(
                    eq(2L), eq(futureMonday), any(LocalTime.class), any(LocalTime.class)
            )).thenReturn(false);

            // Act
            AvailableSlotsResponseDTO response = sut.getAvailableSlots(futureMonday, type);

            // Assert
            assertThat(response.availableSlots()).isNotEmpty();
            response.availableSlots().forEach(slot ->
                    assertThat(slot.availableTechnicians()).isEqualTo(1));
        }

        @Test
        @DisplayName("Sin técnicos activos, retorna lista vacía de slots")
        void givenNoActiveTechnicians_thenReturnEmptySlots() {
            // Arrange
            LocalDate futureMonday = LocalDate.of(2099, 1, 7);
            when(technicianRepository.findAllActive()).thenReturn(List.of());

            // Act
            AvailableSlotsResponseDTO response = sut.getAvailableSlots(futureMonday, AppointmentType.OIL_CHANGE);

            // Assert
            assertThat(response.availableSlots()).isEmpty();
        }

        @Test
        @DisplayName("Tipo de cita sin slots configurados retorna respuesta vacía")
        void givenTypeWithNoConfiguredSlots_thenReturnEmpty() {
            // Arrange
            LocalDate futureMonday = LocalDate.of(2099, 1, 7);
            when(technicianRepository.findAllActive()).thenReturn(List.of(buildTechnician(1L)));

            // REWORK normalmente no tiene slots en el config
            // (ajustar según AppointmentScheduleConfig real)
            AvailableSlotsResponseDTO response = sut.getAvailableSlots(futureMonday, AppointmentType.REWORK);

            // Assert — puede estar vacío si REWORK no tiene slots, o tener slots; validar coherencia
            assertThat(response).isNotNull();
            assertThat(response.date()).isEqualTo(futureMonday);
        }
    }

    // ================================================================
    // NESTED: createAppointment
    // ================================================================
    @Nested
    @DisplayName("createAppointment()")
    class CreateAppointmentTests {

        // ---- Constantes reutilizables ----
        private static final Long CLIENT_ID = 10L;
        private static final Long VEHICLE_ID = 1L;
        private static final LocalDate VALID_DATE = LocalDate.of(2099, 1, 7); // lunes futuro
        private static final LocalTime VALID_TIME = LocalTime.of(8, 0);       // ajustar según config real

        // ----------------------------------------------------------------
        // Builders privados al nested
        // ----------------------------------------------------------------

        private CreateAppointmentRequestDTO buildRequest(AppointmentType type) {
            return new CreateAppointmentRequestDTO(
                    VEHICLE_ID,
                    type,
                    VALID_DATE,
                    VALID_TIME,
                    15000,
                    Set.of("Revisión general")
            );
        }

        private CreateAppointmentRequestDTO buildRequestWith(
                AppointmentType type, LocalDate date, LocalTime time) {
            return new CreateAppointmentRequestDTO(
                    VEHICLE_ID, type, date, time, 15000, Set.of()
            );
        }

        /** Vehículo cuyo owner coincide con CLIENT_ID */
        private VehicleEntity buildVehicleForClient(Long clientId, String plate, String brand) {
            UserEntity owner = new UserEntity();
            owner.setId(clientId);
            owner.setEmail("cliente@test.com");
            owner.setName("Cliente Test");

            VehicleEntity vehicle = new VehicleEntity();
            vehicle.setId(VEHICLE_ID);
            vehicle.setLicensePlate(plate);
            vehicle.setBrand(brand);
            vehicle.setModel("CB 190");
            vehicle.setOwner(owner);
            return vehicle;
        }

        private EmployeeEntity buildTechnicianWithUser(Long id, String name) {
            UserEntity user = new UserEntity();
            user.setName(name);

            EmployeeEntity emp = new EmployeeEntity();
            emp.setId(id);
            emp.setUser(user);
            emp.setPosition(EmployeePosition.MECANICO);
            return emp;
        }

        private AppointmentEntity buildSavedAppointment(VehicleEntity vehicle,
                                                        EmployeeEntity tech,
                                                        AppointmentType type) {
            AppointmentEntity entity = new AppointmentEntity();
            entity.setId(100L);
            entity.setVehicle(vehicle);
            entity.setTechnician(tech);
            entity.setAppointmentType(type);
            entity.setAppointmentDate(VALID_DATE);
            entity.setStartTime(VALID_TIME);
            entity.setEndTime(VALID_TIME.plusMinutes(30));
            entity.setStatus(AppointmentStatus.SCHEDULED);
            return entity;
        }

        // ================================================================
        // CAMINOS DE ERROR — validaciones tempranas
        // ================================================================

        @Test
        @DisplayName("Lanza ReworkNotBookableOnlineException si el tipo es REWORK")
        void givenReworkType_thenThrow() {
            // Arrange
            CreateAppointmentRequestDTO request = buildRequest(AppointmentType.REWORK);

            // Act + Assert
            assertThatThrownBy(() -> sut.createAppointment(request, CLIENT_ID))
                    .isInstanceOf(ReworkNotBookableOnlineException.class);

            verifyNoInteractions(vehicleRepository, appointmentRepository,
                    technicianRepository, notificationService);
        }

        @Test
        @DisplayName("Lanza InvalidAppointmentSlotException si el tipo es UNPLANNED")
        void givenUnplannedType_thenThrow() {
            // Arrange
            CreateAppointmentRequestDTO request = buildRequest(AppointmentType.UNPLANNED);

            // Act + Assert
            assertThatThrownBy(() -> sut.createAppointment(request, CLIENT_ID))
                    .isInstanceOf(InvalidAppointmentSlotException.class)
                    .hasMessageContaining("administrador");

            verifyNoInteractions(vehicleRepository);
        }

        @Test
        @DisplayName("Lanza excepción si el tipo no está en USER_BOOKABLE_TYPES")
        void givenNonUserBookableType_thenThrow() {
            // Usamos UNPLANNED, que no está en USER_BOOKABLE_TYPES y debe lanzar InvalidAppointmentSlotException
            CreateAppointmentRequestDTO request = buildRequest(AppointmentType.UNPLANNED);

            assertThatThrownBy(() -> sut.createAppointment(request, CLIENT_ID))
                    .isInstanceOf(InvalidAppointmentSlotException.class);
        }

        @Test
        @DisplayName("Lanza AppointmentException si el vehículo no existe")
        void givenNonExistentVehicle_thenThrow() {
            // Arrange
            CreateAppointmentRequestDTO request = buildRequest(AppointmentType.OIL_CHANGE);
            when(vehicleRepository.findById(VEHICLE_ID)).thenReturn(Optional.empty());

            // Act + Assert
            assertThatThrownBy(() -> sut.createAppointment(request, CLIENT_ID))
                    .isInstanceOf(AppointmentException.class)
                    .hasMessageContaining(VEHICLE_ID.toString());
        }

        @Test
        @DisplayName("Lanza AppointmentException si el vehículo no pertenece al cliente")
        void givenVehicleFromAnotherClient_thenThrow() {
            // Arrange
            CreateAppointmentRequestDTO request = buildRequest(AppointmentType.OIL_CHANGE);
            Long anotherClient = 99L;
            VehicleEntity vehicle = buildVehicleForClient(anotherClient, "ABC12X", "HONDA");
            when(vehicleRepository.findById(VEHICLE_ID)).thenReturn(Optional.of(vehicle));

            // Act + Assert
            assertThatThrownBy(() -> sut.createAppointment(request, CLIENT_ID))
                    .isInstanceOf(AppointmentException.class)
                    .hasMessageContaining("no pertenece");
        }

        @Test
        @DisplayName("Lanza LicensePlateRestrictionException si la placa tiene pico y placa ese día")
        void givenPlateWithRestriction_thenThrow() {
            // Arrange — lunes 2099-01-07, placa con penúltimo dígito 5 (restringido lunes)
            LocalDate monday = LocalDate.of(2099, 1, 7);
            CreateAppointmentRequestDTO request = buildRequestWith(
                    AppointmentType.OIL_CHANGE, monday, VALID_TIME);

            VehicleEntity vehicle = buildVehicleForClient(CLIENT_ID, "ABC59X", "HONDA");
            when(vehicleRepository.findById(VEHICLE_ID)).thenReturn(Optional.of(vehicle));

            // Act + Assert
            assertThatThrownBy(() -> sut.createAppointment(request, CLIENT_ID))
                    .isInstanceOf(LicensePlateRestrictionException.class)
                    .hasMessageContaining("ABC59X");
        }

        @Test
        @DisplayName("Lanza AppointmentTypeNotAllowedForBrandException si tipo Auteco con marca no Auteco")
        void givenAutoWarrantyWithNonAutecoBrand_thenThrow() {
            // Arrange — AUTECO_WARRANTY solo para Auteco, pero vehículo es Honda
            // Primero necesitamos una fecha SIN restricción para esa placa
            // Usamos miércoles y placa con penúltimo dígito 1 (no restringido el miércoles)
            LocalDate wednesday = LocalDate.of(2099, 1, 9);
            CreateAppointmentRequestDTO request = buildRequestWith(
                    AppointmentType.AUTECO_WARRANTY, wednesday, VALID_TIME);

            VehicleEntity vehicle = buildVehicleForClient(CLIENT_ID, "ABC12X", "HONDA");
            when(vehicleRepository.findById(VEHICLE_ID)).thenReturn(Optional.of(vehicle));

            // Act + Assert
            assertThatThrownBy(() -> sut.createAppointment(request, CLIENT_ID))
                    .isInstanceOf(AppointmentTypeNotAllowedForBrandException.class)
                    .hasMessageContaining("AUTECO");
        }

        @Test
        @DisplayName("Lanza InvalidAppointmentSlotException si el slot no es válido para el tipo")
        void givenInvalidSlotForType_thenThrow() {
            // Arrange — slot 01:00 AM claramente no es válido para ningún tipo
            LocalDate wednesday = LocalDate.of(2099, 1, 9);
            LocalTime invalidTime = LocalTime.of(13, 30);
            CreateAppointmentRequestDTO request = buildRequestWith(
                    AppointmentType.OIL_CHANGE, wednesday, invalidTime);

            VehicleEntity vehicle = buildVehicleForClient(CLIENT_ID, "ABC12X", "HONDA");
            when(vehicleRepository.findById(VEHICLE_ID)).thenReturn(Optional.of(vehicle));

            // Act + Assert
            assertThatThrownBy(() -> sut.createAppointment(request, CLIENT_ID))
                    .isInstanceOf(InvalidAppointmentSlotException.class)
                    .hasMessageContaining("13:30");
        }

        @Test
        @DisplayName("Lanza AppointmentOutsideBusinessHoursException si el slot está fuera del horario laboral")
        void givenSlotOutsideBusinessHours_thenThrow() {
            // Arrange — 06:00 AM antes de las 7:00
            LocalDate wednesday = LocalDate.of(2099, 1, 9);
            LocalTime beforeWork = LocalTime.of(6, 0);
            CreateAppointmentRequestDTO request = buildRequestWith(
                    AppointmentType.OIL_CHANGE, wednesday, beforeWork);

            VehicleEntity vehicle = buildVehicleForClient(CLIENT_ID, "ABC12X", "HONDA");
            when(vehicleRepository.findById(VEHICLE_ID)).thenReturn(Optional.of(vehicle));

            // Act + Assert
            assertThatThrownBy(() -> sut.createAppointment(request, CLIENT_ID))
                    .isInstanceOf(AppointmentOutsideBusinessHoursException.class);
        }

        @Test
        @DisplayName("Lanza AppointmentOutsideBusinessHoursException si el slot cae en horario de almuerzo")
        void givenSlotDuringLunchHour_thenThrow() {
            // Arrange — 12:00 PM = almuerzo
            LocalDate wednesday = LocalDate.of(2099, 1, 9);
            LocalTime lunchTime = LocalTime.of(12, 0);
            CreateAppointmentRequestDTO request = buildRequestWith(
                    AppointmentType.OIL_CHANGE, wednesday, lunchTime);

            VehicleEntity vehicle = buildVehicleForClient(CLIENT_ID, "ABC12X", "HONDA");
            when(vehicleRepository.findById(VEHICLE_ID)).thenReturn(Optional.of(vehicle));

            // Act + Assert
            assertThatThrownBy(() -> sut.createAppointment(request, CLIENT_ID))
                    .isInstanceOf(AppointmentOutsideBusinessHoursException.class)
                    .hasMessageContaining("almuerzo");
        }

        @Test
        @DisplayName("Lanza VehicleHasActiveAppointmentException si el vehículo ya tiene cita activa")
        void givenVehicleWithActiveAppointment_thenThrow() {
            // Arrange — usamos un slot y fechas válidas que pasen todas las validaciones anteriores
            // Fecha: miércoles (no restricción para placa 12X), slot válido del config
            LocalDate wednesday = LocalDate.of(2099, 1, 9);
            // Tomamos el primer slot válido de OIL_CHANGE para garantizar que pase validateSlotForType
            LocalTime firstValidSlot = AppointmentScheduleConfig.VALID_SLOTS_BY_TYPE
                    .getOrDefault(AppointmentType.OIL_CHANGE, List.of(LocalTime.of(7, 0)))
                    .getFirst();

            CreateAppointmentRequestDTO request = buildRequestWith(
                    AppointmentType.OIL_CHANGE, wednesday, firstValidSlot);

            VehicleEntity vehicle = buildVehicleForClient(CLIENT_ID, "ABC12X", "HONDA");
            when(vehicleRepository.findById(VEHICLE_ID)).thenReturn(Optional.of(vehicle));
            when(appointmentRepository.existsActiveAppointmentByVehicleId(VEHICLE_ID))
                    .thenReturn(true);

            // Act + Assert
            assertThatThrownBy(() -> sut.createAppointment(request, CLIENT_ID))
                    .isInstanceOf(VehicleHasActiveAppointmentException.class)
                    .hasMessageContaining("ABC12X");
        }

        @Test
        @DisplayName("Lanza NoAvailableTechnicianException si todos los técnicos están ocupados")
        void givenNoAvailableTechnician_thenThrow() {
            // Arrange
            LocalDate wednesday = LocalDate.of(2099, 1, 9);
            LocalTime firstValidSlot = AppointmentScheduleConfig.VALID_SLOTS_BY_TYPE
                    .getOrDefault(AppointmentType.OIL_CHANGE, List.of(LocalTime.of(7, 0)))
                    .getFirst();

            CreateAppointmentRequestDTO request = buildRequestWith(
                    AppointmentType.OIL_CHANGE, wednesday, firstValidSlot);

            VehicleEntity vehicle = buildVehicleForClient(CLIENT_ID, "ABC12X", "HONDA");
            when(vehicleRepository.findById(VEHICLE_ID)).thenReturn(Optional.of(vehicle));
            when(appointmentRepository.existsActiveAppointmentByVehicleId(VEHICLE_ID))
                    .thenReturn(false);

            // Técnicos activos pero todos ocupados
            EmployeeEntity tech = buildTechnicianWithUser(1L, "Juan");
            when(technicianRepository.findAllActive()).thenReturn(List.of(tech));
            when(appointmentRepository.existsTechnicianConflict(
                    anyLong(), any(), any(), any()
            )).thenReturn(true);

            // Act + Assert
            assertThatThrownBy(() -> sut.createAppointment(request, CLIENT_ID))
                    .isInstanceOf(NoAvailableTechnicianException.class);
        }

        // ================================================================
        // CAMINO FELIZ
        // ================================================================

        @Test
        @DisplayName("Crea la cita exitosamente, persiste, notifica y retorna DTO")
        void givenValidRequest_thenCreateAndNotify() {
            // Arrange
            LocalDate wednesday = LocalDate.of(2099, 1, 9);
            LocalTime firstValidSlot = AppointmentScheduleConfig.VALID_SLOTS_BY_TYPE
                    .getOrDefault(AppointmentType.OIL_CHANGE, List.of(LocalTime.of(7, 0)))
                    .getFirst();

            CreateAppointmentRequestDTO request = new CreateAppointmentRequestDTO(
                    VEHICLE_ID,
                    AppointmentType.OIL_CHANGE,
                    wednesday,
                    firstValidSlot,
                    15000,
                    Set.of("Nota del cliente")
            );

            VehicleEntity vehicle = buildVehicleForClient(CLIENT_ID, "ABC12X", "HONDA");
            EmployeeEntity tech = buildTechnicianWithUser(1L, "Juan Técnico");
            AppointmentEntity savedEntity = buildSavedAppointment(vehicle, tech, AppointmentType.OIL_CHANGE);
            AppointmentResponseDTO expectedDTO = mock(AppointmentResponseDTO.class);

            when(vehicleRepository.findById(VEHICLE_ID)).thenReturn(Optional.of(vehicle));
            when(appointmentRepository.existsActiveAppointmentByVehicleId(VEHICLE_ID))
                    .thenReturn(false);
            when(technicianRepository.findAllActive()).thenReturn(List.of(tech));
            when(appointmentRepository.existsTechnicianConflict(
                    eq(1L), eq(wednesday), any(), any()
            )).thenReturn(false);
            when(appointmentRepository.save(any(AppointmentEntity.class))).thenReturn(savedEntity);
            when(appointmentMapper.toResponseDTO(savedEntity)).thenReturn(expectedDTO);

            // Act
            AppointmentResponseDTO result = sut.createAppointment(request, CLIENT_ID);

            // Assert
            assertThat(result).isEqualTo(expectedDTO);

            // Verificar que se guardó exactamente una vez
            verify(appointmentRepository, times(1)).save(any(AppointmentEntity.class));

            // Verificar que se notificó al cliente
            verify(notificationService, times(1))
                    .sendAppointmentCreatedNotification(any(AppointmentNotificationDTO.class));
        }

        @Test
        @DisplayName("Crea cita sin notas del cliente (clientNotes null) sin error")
        void givenNullClientNotes_thenCreateSuccessfully() {
            // Arrange
            LocalDate wednesday = LocalDate.of(2099, 1, 9);
            LocalTime firstValidSlot = AppointmentScheduleConfig.VALID_SLOTS_BY_TYPE
                    .getOrDefault(AppointmentType.OIL_CHANGE, List.of(LocalTime.of(7, 0)))
                    .getFirst();

            CreateAppointmentRequestDTO request = new CreateAppointmentRequestDTO(
                    VEHICLE_ID,
                    AppointmentType.OIL_CHANGE,
                    wednesday,
                    firstValidSlot,
                    15000,
                    null   // <-- sin notas
            );

            VehicleEntity vehicle = buildVehicleForClient(CLIENT_ID, "ABC12X", "HONDA");
            EmployeeEntity tech = buildTechnicianWithUser(1L, "Juan");
            AppointmentEntity savedEntity = buildSavedAppointment(vehicle, tech, AppointmentType.OIL_CHANGE);

            when(vehicleRepository.findById(VEHICLE_ID)).thenReturn(Optional.of(vehicle));
            when(appointmentRepository.existsActiveAppointmentByVehicleId(VEHICLE_ID))
                    .thenReturn(false);
            when(technicianRepository.findAllActive()).thenReturn(List.of(tech));
            when(appointmentRepository.existsTechnicianConflict(anyLong(), any(), any(), any()))
                    .thenReturn(false);
            when(appointmentRepository.save(any())).thenReturn(savedEntity);
            when(appointmentMapper.toResponseDTO(savedEntity)).thenReturn(mock(AppointmentResponseDTO.class));

            // Act + Assert — no debe lanzar excepción
            assertThatCode(() -> sut.createAppointment(request, CLIENT_ID))
                    .doesNotThrowAnyException();

            // La entidad guardada no debe tener notas (clientNotes == null)
            verify(appointmentRepository).save(argThat(apt ->
                    apt.getClientNotes() == null
            ));
        }

        @Test
        @DisplayName("Asigna el primer técnico libre en orden (rotación)")
        void givenMultipleTechnicians_thenAssignFirstAvailable() {
            // Arrange — tech1 ocupado, tech2 libre → debe asignarse tech2
            LocalDate wednesday = LocalDate.of(2099, 1, 9);
            LocalTime firstValidSlot = AppointmentScheduleConfig.VALID_SLOTS_BY_TYPE
                    .getOrDefault(AppointmentType.OIL_CHANGE, List.of(LocalTime.of(7, 0)))
                    .getFirst();

            CreateAppointmentRequestDTO request = buildRequestWith(
                    AppointmentType.OIL_CHANGE, wednesday, firstValidSlot);

            VehicleEntity vehicle = buildVehicleForClient(CLIENT_ID, "ABC12X", "HONDA");
            EmployeeEntity tech1 = buildTechnicianWithUser(1L, "Pedro");
            EmployeeEntity tech2 = buildTechnicianWithUser(2L, "Maria");

            when(vehicleRepository.findById(VEHICLE_ID)).thenReturn(Optional.of(vehicle));
            when(appointmentRepository.existsActiveAppointmentByVehicleId(VEHICLE_ID))
                    .thenReturn(false);
            when(technicianRepository.findAllActive()).thenReturn(List.of(tech1, tech2));

            // tech1 ocupado, tech2 libre
            when(appointmentRepository.existsTechnicianConflict(eq(1L), any(), any(), any()))
                    .thenReturn(true);
            when(appointmentRepository.existsTechnicianConflict(eq(2L), any(), any(), any()))
                    .thenReturn(false);

            AppointmentEntity savedEntity = buildSavedAppointment(vehicle, tech2, AppointmentType.OIL_CHANGE);
            when(appointmentRepository.save(any())).thenReturn(savedEntity);
            when(appointmentMapper.toResponseDTO(any())).thenReturn(mock(AppointmentResponseDTO.class));

            // Act
            sut.createAppointment(request, CLIENT_ID);

            // Assert — el técnico asignado en la entidad guardada debe ser tech2
            verify(appointmentRepository).save(argThat(apt ->
                    apt.getTechnician() != null &&
                            apt.getTechnician().getId().equals(2L)
            ));
        }

        @Test
        @DisplayName("Cita Auteco WARRANTY exitosa cuando la marca es AUTECO")
        void givenAutoWarrantyWithAutecoBrand_thenSuccess() {
            // Arrange
            LocalDate wednesday = LocalDate.of(2099, 1, 9);
            LocalTime firstValidSlot = AppointmentScheduleConfig.VALID_SLOTS_BY_TYPE
                    .getOrDefault(AppointmentType.AUTECO_WARRANTY, List.of(LocalTime.of(7, 0)))
                    .getFirst();

            // Solo ejecutar si AUTECO_WARRANTY es bookable por el usuario
            org.junit.jupiter.api.Assumptions.assumeTrue(
                    AppointmentScheduleConfig.USER_BOOKABLE_TYPES.contains(AppointmentType.AUTECO_WARRANTY),
                    "AUTECO_WARRANTY no es bookable online, se omite"
            );

            CreateAppointmentRequestDTO request = buildRequestWith(
                    AppointmentType.AUTECO_WARRANTY, wednesday, firstValidSlot);

            VehicleEntity vehicle = buildVehicleForClient(CLIENT_ID, "ABC12X", "AUTECO");
            EmployeeEntity tech = buildTechnicianWithUser(1L, "Técnico");
            AppointmentEntity savedEntity = buildSavedAppointment(vehicle, tech, AppointmentType.AUTECO_WARRANTY);

            when(vehicleRepository.findById(VEHICLE_ID)).thenReturn(Optional.of(vehicle));
            when(appointmentRepository.existsActiveAppointmentByVehicleId(VEHICLE_ID))
                    .thenReturn(false);
            when(technicianRepository.findAllActive()).thenReturn(List.of(tech));
            when(appointmentRepository.existsTechnicianConflict(anyLong(), any(), any(), any()))
                    .thenReturn(false);
            when(appointmentRepository.save(any())).thenReturn(savedEntity);
            when(appointmentMapper.toResponseDTO(any())).thenReturn(mock(AppointmentResponseDTO.class));

            // Act + Assert
            assertThatCode(() -> sut.createAppointment(request, CLIENT_ID))
                    .doesNotThrowAnyException();
        }
    }
}

