package com.sparktech.motorx.Services.impl;

import com.sparktech.motorx.Services.IAppointmentService;
import com.sparktech.motorx.Services.ICurrentUserService;
import com.sparktech.motorx.dto.appointment.*;
import com.sparktech.motorx.dto.auth.RegisterUserDTO;
import com.sparktech.motorx.dto.user.UpdateUserRequestDTO;
import com.sparktech.motorx.entity.*;
import com.sparktech.motorx.exception.AppointmentException;
import com.sparktech.motorx.exception.AppointmentNotFoundException;
import com.sparktech.motorx.mapper.AppointmentMapper;
import com.sparktech.motorx.repository.JpaAppointmentRepository;
import com.sparktech.motorx.repository.JpaUserRepository;
import com.sparktech.motorx.repository.JpaVehicleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceImpl - Unit Tests")
class UserServiceImplTest {

    // ================================================================
    // MOCKS
    // ================================================================
    @Mock private JpaUserRepository jpaUserRepository;
    @Mock private JpaVehicleRepository vehicleRepository;
    @Mock private JpaAppointmentRepository appointmentRepository;
    @Mock private IAppointmentService appointmentService;
    @Mock private AppointmentMapper appointmentMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private ICurrentUserService currentUserService;

    @InjectMocks
    private UserServiceImpl sut;

    @Captor private ArgumentCaptor<UserEntity> userCaptor;
    @Captor private ArgumentCaptor<AppointmentEntity> appointmentCaptor;

    // ================================================================
    // BUILDERS
    // ================================================================

    private UserEntity buildUser(Long id) {
        UserEntity u = new UserEntity();
        u.setId(id);
        u.setName("Usuario " + id);
        u.setEmail("user" + id + "@test.com");
        u.setDni("DNI" + id);
        u.setPhone("300000000" + id);
        u.setRole(Role.CLIENT);
        u.setEnabled(true);
        u.setAccountLocked(false);
        return u;
    }

    private VehicleEntity buildVehicle(Long id, String plate, UserEntity owner) {
        VehicleEntity v = new VehicleEntity();
        v.setId(id);
        v.setLicensePlate(plate);
        v.setBrand("HONDA");
        v.setModel("CB 190");
        v.setOwner(owner);
        return v;
    }

    private AppointmentEntity buildAppointment(Long id, UserEntity owner,
                                               AppointmentStatus status) {
        VehicleEntity vehicle = buildVehicle(99L, "ABC33X", owner);
        AppointmentEntity a = new AppointmentEntity();
        a.setId(id);
        a.setVehicle(vehicle);
        a.setStatus(status);
        a.setAppointmentType(AppointmentType.OIL_CHANGE);
        return a;
    }

    // ================================================================
    // register()
    // ================================================================

    @Nested
    @DisplayName("register()")
    class RegisterTests {

        private final RegisterUserDTO request = new RegisterUserDTO(
                "Nuevo Usuario", "123456789", "nuevo@test.com", "pass123", "3001234567");

        @Test
        @DisplayName("Camino feliz: crea usuario con rol CLIENT, enabled, password encriptada")
        void givenValidRequest_thenPersistUserCorrectly() {
            // Arrange
            when(jpaUserRepository.existsByEmail("nuevo@test.com")).thenReturn(false);
            when(jpaUserRepository.existsByDni("123456789")).thenReturn(false);
            when(passwordEncoder.encode("pass123")).thenReturn("encoded-pass");

            // Act
            assertThatCode(() -> sut.register(request)).doesNotThrowAnyException();

            // Assert
            verify(jpaUserRepository).save(userCaptor.capture());
            UserEntity saved = userCaptor.getValue();
            assertThat(saved.getRole()).isEqualTo(Role.CLIENT);
            assertThat(saved.isEnabled()).isTrue();
            assertThat(saved.isAccountLocked()).isFalse();
            assertThat(saved.getPassword()).isEqualTo("encoded-pass");
            assertThat(saved.getEmail()).isEqualTo("nuevo@test.com");
        }

        @Test
        @DisplayName("Lanza IllegalArgumentException si el email ya está registrado")
        void givenDuplicateEmail_thenThrow() {
            // Arrange
            when(jpaUserRepository.existsByEmail("nuevo@test.com")).thenReturn(true);

            // Act + Assert
            assertThatThrownBy(() -> sut.register(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("email");

            verify(jpaUserRepository, never()).save(any());
        }

        @Test
        @DisplayName("Lanza IllegalArgumentException si el DNI ya está registrado")
        void givenDuplicateDni_thenThrow() {
            // Arrange
            when(jpaUserRepository.existsByEmail(any())).thenReturn(false);
            when(jpaUserRepository.existsByDni("123456789")).thenReturn(true);

            // Act + Assert
            assertThatThrownBy(() -> sut.register(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("DNI");

            verify(jpaUserRepository, never()).save(any());
        }
    }

    // ================================================================
    // updateUserDTO()
    // ================================================================

    @Nested
    @DisplayName("updateUserDTO()")
    class UpdateUserTests {

        @Test
        @DisplayName("Actualiza nombre y teléfono correctamente")
        void givenValidRequest_thenUpdateNameAndPhone() {
            // Arrange
            UserEntity user = buildUser(1L);
            UpdateUserRequestDTO request = new UpdateUserRequestDTO("Nuevo Nombre", "3119998877");
            when(jpaUserRepository.findById(1L)).thenReturn(Optional.of(user));

            // Act
            sut.updateUserDTO(1L, request);

            // Assert
            verify(jpaUserRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getName()).isEqualTo("Nuevo Nombre");
            assertThat(userCaptor.getValue().getPhone()).isEqualTo("3119998877");
        }

        @Test
        @DisplayName("Lanza IllegalArgumentException si el usuario no existe")
        void givenNonExistentUser_thenThrow() {
            // Arrange
            Long userId = 99L;
            var request = new UpdateUserRequestDTO("X", "Y"); // Extraído de la lambda

            when(jpaUserRepository.findById(userId)).thenReturn(Optional.empty());

            // Act + Assert
            // La lambda ahora contiene una única sentencia
            assertThatThrownBy(() -> sut.updateUserDTO(userId, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(String.valueOf(userId));

            verify(jpaUserRepository, never()).save(any());
        }
    }

    // ================================================================
    // getAvailableSlots()
    // ================================================================

    @Nested
    @DisplayName("getAvailableSlots()")
    class AvailableSlotsTests {

        @Test
        @DisplayName("Delega correctamente a appointmentService")
        void givenValidArgs_thenDelegateToAppointmentService() {
            // Arrange
            LocalDate date = LocalDate.of(2099, 1, 7);
            AvailableSlotsResponseDTO expected = mock(AvailableSlotsResponseDTO.class);
            when(appointmentService.getAvailableSlots(date, AppointmentType.OIL_CHANGE))
                    .thenReturn(expected);

            // Act
            AvailableSlotsResponseDTO result =
                    sut.getAvailableSlots(date, AppointmentType.OIL_CHANGE);

            // Assert
            assertThat(result).isEqualTo(expected);
            verify(appointmentService, times(1))
                    .getAvailableSlots(date, AppointmentType.OIL_CHANGE);
        }
    }

    // ================================================================
    // checkLicensePlateRestriction()
    // ================================================================

    @Nested
    @DisplayName("checkLicensePlateRestriction()")
    class LicensePlateRestrictionTests {

        private final LocalDate date = LocalDate.of(2099, 1, 7);

        @Test
        @DisplayName("Retorna noRestriction cuando la placa no tiene restricción")
        void givenPlateWithoutRestriction_thenReturnNoRestriction() {
            // Arrange
            UserEntity user = buildUser(1L);
            VehicleEntity vehicle = buildVehicle(10L, "ABC32X", user);
            when(currentUserService.getAuthenticatedUser()).thenReturn(user);
            when(vehicleRepository.findById(10L)).thenReturn(Optional.of(vehicle));
            when(appointmentService.hasLicensePlateRestriction("ABC32X", date))
                    .thenReturn(false);

            // Act
            LicensePlateRestrictionResponseDTO result =
                    sut.checkLicensePlateRestriction(10L, date);

            // Assert
            // Validamos que los campos de contacto urgente sean nulos,
            // lo cual define un estado sin restricción según tus métodos estáticos.
            assertThat(result.urgentContactMessage()).isNull();
            assertThat(result.phoneNumber()).isNull();
            assertThat(result.vehiclePlate()).isEqualTo("ABC32X");
        }

        @Test
        @DisplayName("Retorna restriction cuando la placa tiene restricción")
        void givenPlateWithRestriction_thenReturnRestriction() {
            // Arrange
            UserEntity user = buildUser(1L);
            VehicleEntity vehicle = buildVehicle(10L, "ABC51X", user);
            when(currentUserService.getAuthenticatedUser()).thenReturn(user);
            when(vehicleRepository.findById(10L)).thenReturn(Optional.of(vehicle));
            when(appointmentService.hasLicensePlateRestriction("ABC51X", date))
                    .thenReturn(true);

            // Act
            LicensePlateRestrictionResponseDTO result =
                    sut.checkLicensePlateRestriction(10L, date);

            // Assert
            // Validamos que los campos de restricción existan.
            assertThat(result.urgentContactMessage()).isNotNull();
            assertThat(result.phoneNumber()).isNotBlank();
            assertThat(result.vehiclePlate()).isEqualTo("ABC51X");
        }

        @Test
        @DisplayName("Lanza AppointmentException si el vehículo no existe")
        void givenNonExistentVehicle_thenThrow() {
            // Arrange
            when(currentUserService.getAuthenticatedUser()).thenReturn(buildUser(1L));
            when(vehicleRepository.findById(99L)).thenReturn(Optional.empty());

            // Act + Assert
            assertThatThrownBy(() -> sut.checkLicensePlateRestriction(99L, date))
                    .isInstanceOf(AppointmentException.class)
                    .hasMessageContaining("99");
        }

        @Test
        @DisplayName("Lanza AppointmentException si el vehículo no pertenece al usuario autenticado")
        void givenVehicleOfAnotherUser_thenThrow() {
            // Arrange
            UserEntity currentUser = buildUser(1L);
            UserEntity otherUser   = buildUser(2L);
            VehicleEntity vehicle  = buildVehicle(10L, "ABC33X", otherUser);

            when(currentUserService.getAuthenticatedUser()).thenReturn(currentUser);
            when(vehicleRepository.findById(10L)).thenReturn(Optional.of(vehicle));

            // Act + Assert
            assertThatThrownBy(() -> sut.checkLicensePlateRestriction(10L, date))
                    .isInstanceOf(AppointmentException.class)
                    .hasMessageContaining("no pertenece");
        }
    }

    // ================================================================
    // getReworkRedirectInfo()
    // ================================================================

    @Nested
    @DisplayName("getReworkRedirectInfo()")
    class ReworkRedirectTests {

        @Test
        @DisplayName("Retorna la respuesta por defecto sin lanzar excepción")
        void givenCall_thenReturnDefaultResponse() {
            // Act + Assert
            assertThatCode(() -> sut.getReworkRedirectInfo())
                    .doesNotThrowAnyException();

            ReworkRedirectResponseDTO result = sut.getReworkRedirectInfo();
            assertThat(result).isNotNull();
        }
    }

    // ================================================================
    // scheduleAppointment()
    // ================================================================

    @Nested
    @DisplayName("scheduleAppointment()")
    class ScheduleAppointmentTests {

        @Test
        @DisplayName("Delega a appointmentService con el ID del usuario autenticado")
        void givenValidRequest_thenDelegateWithCurrentUserId() {
            // Arrange
            UserEntity user = buildUser(1L);
            CreateAppointmentRequestDTO request = mock(CreateAppointmentRequestDTO.class);
            AppointmentResponseDTO expected = mock(AppointmentResponseDTO.class);

            when(currentUserService.getAuthenticatedUser()).thenReturn(user);
            when(appointmentService.createAppointment(request, 1L)).thenReturn(expected);

            // Act
            AppointmentResponseDTO result = sut.scheduleAppointment(request);

            // Assert
            assertThat(result).isEqualTo(expected);
            verify(appointmentService, times(1)).createAppointment(request, 1L);
        }
    }

    // ================================================================
    // cancelMyAppointment()
    // ================================================================

    @Nested
    @DisplayName("cancelMyAppointment()")
    class CancelMyAppointmentTests {

        @Test
        @DisplayName("Cancela exitosamente una cita SCHEDULED propia")
        void givenScheduledOwnAppointment_thenCancelAndReturn() {
            // Arrange
            UserEntity user = buildUser(1L);
            AppointmentEntity appointment = buildAppointment(10L, user, AppointmentStatus.SCHEDULED);
            AppointmentResponseDTO expected = mock(AppointmentResponseDTO.class);

            when(currentUserService.getAuthenticatedUser()).thenReturn(user);
            when(appointmentRepository.findById(10L)).thenReturn(Optional.of(appointment));
            when(appointmentRepository.save(any())).thenReturn(appointment);
            when(appointmentMapper.toResponseDTO(appointment)).thenReturn(expected);

            // Act
            AppointmentResponseDTO result = sut.cancelMyAppointment(10L);

            // Assert
            assertThat(result).isEqualTo(expected);
            verify(appointmentRepository).save(appointmentCaptor.capture());
            assertThat(appointmentCaptor.getValue().getStatus())
                    .isEqualTo(AppointmentStatus.CANCELLED);
            assertThat(appointmentCaptor.getValue().getCancellationReason())
                    .contains("cliente");
        }

        @Test
        @DisplayName("Lanza AppointmentNotFoundException si la cita no existe")
        void givenNonExistentAppointment_thenThrow() {
            // Arrange
            when(currentUserService.getAuthenticatedUser()).thenReturn(buildUser(1L));
            when(appointmentRepository.findById(99L)).thenReturn(Optional.empty());

            // Act + Assert
            assertThatThrownBy(() -> sut.cancelMyAppointment(99L))
                    .isInstanceOf(AppointmentNotFoundException.class);

            verify(appointmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Lanza AppointmentException si la cita pertenece a otro usuario")
        void givenAppointmentOfAnotherUser_thenThrow() {
            // Arrange
            UserEntity currentUser = buildUser(1L);
            UserEntity otherUser   = buildUser(2L);
            AppointmentEntity appointment =
                    buildAppointment(10L, otherUser, AppointmentStatus.SCHEDULED);

            when(currentUserService.getAuthenticatedUser()).thenReturn(currentUser);
            when(appointmentRepository.findById(10L)).thenReturn(Optional.of(appointment));

            // Act + Assert
            assertThatThrownBy(() -> sut.cancelMyAppointment(10L))
                    .isInstanceOf(AppointmentException.class)
                    .hasMessageContaining("permiso");

            verify(appointmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Lanza AppointmentException si la cita ya está CANCELLED")
        void givenAlreadyCancelledAppointment_thenThrow() {
            // Arrange
            UserEntity user = buildUser(1L);
            AppointmentEntity appointment =
                    buildAppointment(10L, user, AppointmentStatus.CANCELLED);

            when(currentUserService.getAuthenticatedUser()).thenReturn(user);
            when(appointmentRepository.findById(10L)).thenReturn(Optional.of(appointment));

            // Act + Assert
            assertThatThrownBy(() -> sut.cancelMyAppointment(10L))
                    .isInstanceOf(AppointmentException.class)
                    .hasMessageContaining("cancelada");

            verify(appointmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Lanza AppointmentException si la cita está IN_PROGRESS")
        void givenInProgressAppointment_thenThrow() {
            // Arrange
            UserEntity user = buildUser(1L);
            AppointmentEntity appointment =
                    buildAppointment(10L, user, AppointmentStatus.IN_PROGRESS);

            when(currentUserService.getAuthenticatedUser()).thenReturn(user);
            when(appointmentRepository.findById(10L)).thenReturn(Optional.of(appointment));

            // Act + Assert
            assertThatThrownBy(() -> sut.cancelMyAppointment(10L))
                    .isInstanceOf(AppointmentException.class)
                    .hasMessageContaining("progreso");

            verify(appointmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Lanza AppointmentException si la cita está COMPLETED")
        void givenCompletedAppointment_thenThrow() {
            // Arrange
            UserEntity user = buildUser(1L);
            AppointmentEntity appointment =
                    buildAppointment(10L, user, AppointmentStatus.COMPLETED);

            when(currentUserService.getAuthenticatedUser()).thenReturn(user);
            when(appointmentRepository.findById(10L)).thenReturn(Optional.of(appointment));

            // Act + Assert
            assertThatThrownBy(() -> sut.cancelMyAppointment(10L))
                    .isInstanceOf(AppointmentException.class)
                    .hasMessageContaining("progreso");

            verify(appointmentRepository, never()).save(any());
        }
    }

    // ================================================================
    // getMyAppointmentHistory()
    // ================================================================

    @Nested
    @DisplayName("getMyAppointmentHistory()")
    class AppointmentHistoryTests {

        @Test
        @DisplayName("Retorna lista mapeada del usuario autenticado")
        void givenAuthenticatedUser_thenReturnMappedHistory() {
            // Arrange
            UserEntity user = buildUser(1L);
            AppointmentEntity a1 = buildAppointment(1L, user, AppointmentStatus.COMPLETED);
            AppointmentEntity a2 = buildAppointment(2L, user, AppointmentStatus.CANCELLED);
            AppointmentResponseDTO dto1 = mock(AppointmentResponseDTO.class);
            AppointmentResponseDTO dto2 = mock(AppointmentResponseDTO.class);

            when(currentUserService.getAuthenticatedUser()).thenReturn(user);
            when(appointmentRepository.findByClientIdOrderByDateDesc(1L))
                    .thenReturn(List.of(a1, a2));
            when(appointmentMapper.toResponseDTO(a1)).thenReturn(dto1);
            when(appointmentMapper.toResponseDTO(a2)).thenReturn(dto2);

            // Act
            List<AppointmentResponseDTO> result = sut.getMyAppointmentHistory();

            // Assert
            assertThat(result).containsExactly(dto1, dto2);
        }

        @Test
        @DisplayName("Retorna lista vacía si no hay citas")
        void givenNoAppointments_thenReturnEmptyList() {
            // Arrange
            UserEntity user = buildUser(1L);
            when(currentUserService.getAuthenticatedUser()).thenReturn(user);
            when(appointmentRepository.findByClientIdOrderByDateDesc(1L))
                    .thenReturn(List.of());

            // Act + Assert
            assertThat(sut.getMyAppointmentHistory()).isEmpty();
        }
    }

    // ================================================================
    // getMyVehicleAppointments()
    // ================================================================

    @Nested
    @DisplayName("getMyVehicleAppointments()")
    class MyVehicleAppointmentsTests {

        @Test
        @DisplayName("Retorna citas del vehículo cuando pertenece al usuario")
        void givenOwnVehicle_thenReturnAppointments() {
            // Arrange
            UserEntity user = buildUser(1L);
            VehicleEntity vehicle = buildVehicle(10L, "ABC33X", user);
            AppointmentEntity a = buildAppointment(1L, user, AppointmentStatus.SCHEDULED);
            AppointmentResponseDTO dto = mock(AppointmentResponseDTO.class);

            when(currentUserService.getAuthenticatedUser()).thenReturn(user);
            when(vehicleRepository.findById(10L)).thenReturn(Optional.of(vehicle));
            when(appointmentRepository.findByVehicleIdOrderByAppointmentDateDesc(10L))
                    .thenReturn(List.of(a));
            when(appointmentMapper.toResponseDTO(a)).thenReturn(dto);

            // Act
            List<AppointmentResponseDTO> result = sut.getMyVehicleAppointments(10L);

            // Assert
            assertThat(result).containsExactly(dto);
        }

        @Test
        @DisplayName("Lanza AppointmentException si el vehículo no existe")
        void givenNonExistentVehicle_thenThrow() {
            // Arrange
            when(currentUserService.getAuthenticatedUser()).thenReturn(buildUser(1L));
            when(vehicleRepository.findById(99L)).thenReturn(Optional.empty());

            // Act + Assert
            assertThatThrownBy(() -> sut.getMyVehicleAppointments(99L))
                    .isInstanceOf(AppointmentException.class)
                    .hasMessageContaining("99");
        }

        @Test
        @DisplayName("Lanza AppointmentException si el vehículo no pertenece al usuario")
        void givenVehicleOfAnotherUser_thenThrow() {
            // Arrange
            UserEntity currentUser = buildUser(1L);
            UserEntity otherUser   = buildUser(2L);
            VehicleEntity vehicle  = buildVehicle(10L, "ABC33X", otherUser);

            when(currentUserService.getAuthenticatedUser()).thenReturn(currentUser);
            when(vehicleRepository.findById(10L)).thenReturn(Optional.of(vehicle));

            // Act + Assert
            assertThatThrownBy(() -> sut.getMyVehicleAppointments(10L))
                    .isInstanceOf(AppointmentException.class)
                    .hasMessageContaining("no pertenece");
        }
    }

    // ================================================================
    // getMyAppointmentById()
    // ================================================================

    @Nested
    @DisplayName("getMyAppointmentById()")
    class GetMyAppointmentByIdTests {

        @Test
        @DisplayName("Retorna DTO cuando la cita pertenece al usuario autenticado")
        void givenOwnAppointment_thenReturnDTO() {
            // Arrange
            UserEntity user = buildUser(1L);
            AppointmentEntity appointment =
                    buildAppointment(10L, user, AppointmentStatus.SCHEDULED);
            AppointmentResponseDTO expected = mock(AppointmentResponseDTO.class);

            when(currentUserService.getAuthenticatedUser()).thenReturn(user);
            when(appointmentRepository.findById(10L)).thenReturn(Optional.of(appointment));
            when(appointmentMapper.toResponseDTO(appointment)).thenReturn(expected);

            // Act
            AppointmentResponseDTO result = sut.getMyAppointmentById(10L);

            // Assert
            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("Lanza AppointmentNotFoundException si la cita no existe")
        void givenNonExistentAppointment_thenThrow() {
            // Arrange
            when(currentUserService.getAuthenticatedUser()).thenReturn(buildUser(1L));
            when(appointmentRepository.findById(99L)).thenReturn(Optional.empty());

            // Act + Assert
            assertThatThrownBy(() -> sut.getMyAppointmentById(99L))
                    .isInstanceOf(AppointmentNotFoundException.class);
        }

        @Test
        @DisplayName("Lanza AppointmentException si la cita pertenece a otro usuario")
        void givenAppointmentOfAnotherUser_thenThrow() {
            // Arrange
            UserEntity currentUser = buildUser(1L);
            UserEntity otherUser   = buildUser(2L);
            AppointmentEntity appointment =
                    buildAppointment(10L, otherUser, AppointmentStatus.SCHEDULED);

            when(currentUserService.getAuthenticatedUser()).thenReturn(currentUser);
            when(appointmentRepository.findById(10L)).thenReturn(Optional.of(appointment));

            // Act + Assert
            assertThatThrownBy(() -> sut.getMyAppointmentById(10L))
                    .isInstanceOf(AppointmentException.class)
                    .hasMessageContaining("permiso");
        }
    }
}