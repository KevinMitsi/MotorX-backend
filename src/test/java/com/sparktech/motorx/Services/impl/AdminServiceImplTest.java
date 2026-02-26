package com.sparktech.motorx.Services.impl;


import com.sparktech.motorx.dto.appointment.*;
import com.sparktech.motorx.entity.AppointmentType;
import com.sparktech.motorx.Services.IAppointmentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminServiceImpl - Unit Tests")
class AdminServiceImplTest {

    @Mock
    private IAppointmentService appointmentService;

    @InjectMocks
    private AdminServiceImpl sut;

    // ================================================================
    // Constantes reutilizables
    // ================================================================
    private static final LocalDate DATE       = LocalDate.of(2099, 1, 7);
    private static final LocalDate DATE_END   = LocalDate.of(2099, 1, 14);
    private static final Long APPOINTMENT_ID  = 42L;
    private static final Long CLIENT_ID       = 10L;
    private static final Long VEHICLE_ID      = 5L;
    private static final AppointmentType TYPE = AppointmentType.OIL_CHANGE;

    // ================================================================
    // VISIBILIDAD DE LA AGENDA
    // ================================================================

    @Nested
    @DisplayName("Visibilidad de agenda")
    class AgendaVisibilityTests {

        @Test
        @DisplayName("getDailyAgenda() delega a appointmentService.getAppointmentsByDate()")
        void getDailyAgenda_delegatesCorrectly() {
            // Arrange
            List<AppointmentResponseDTO> expected = List.of(mock(AppointmentResponseDTO.class));
            when(appointmentService.getAppointmentsByDate(DATE)).thenReturn(expected);

            // Act
            List<AppointmentResponseDTO> result = sut.getDailyAgenda(DATE);

            // Assert
            assertThat(result).isEqualTo(expected);
            verify(appointmentService, times(1)).getAppointmentsByDate(DATE);
            verifyNoMoreInteractions(appointmentService);
        }

        @Test
        @DisplayName("getCalendarView() delega a appointmentService.getAppointmentsByDateRange()")
        void getCalendarView_delegatesCorrectly() {
            // Arrange
            List<AppointmentResponseDTO> expected = List.of(mock(AppointmentResponseDTO.class));
            when(appointmentService.getAppointmentsByDateRange(DATE, DATE_END)).thenReturn(expected);

            // Act
            List<AppointmentResponseDTO> result = sut.getCalendarView(DATE, DATE_END);

            // Assert
            assertThat(result).isEqualTo(expected);
            verify(appointmentService, times(1)).getAppointmentsByDateRange(DATE, DATE_END);
            verifyNoMoreInteractions(appointmentService);
        }

        @Test
        @DisplayName("getAvailableSlots() delega a appointmentService.getAvailableSlots()")
        void getAvailableSlots_delegatesCorrectly() {
            // Arrange
            AvailableSlotsResponseDTO expected = mock(AvailableSlotsResponseDTO.class);
            when(appointmentService.getAvailableSlots(DATE, TYPE)).thenReturn(expected);

            // Act
            AvailableSlotsResponseDTO result = sut.getAvailableSlots(DATE, TYPE);

            // Assert
            assertThat(result).isEqualTo(expected);
            verify(appointmentService, times(1)).getAvailableSlots(DATE, TYPE);
            verifyNoMoreInteractions(appointmentService);
        }
    }

    // ================================================================
    // OPERACIONES EXCLUSIVAS DEL ADMIN
    // ================================================================

    @Nested
    @DisplayName("Operaciones exclusivas del admin")
    class AdminOperationsTests {

        @Test
        @DisplayName("registerUnplannedAppointment() delega a appointmentService.createUnplannedAppointment()")
        void registerUnplannedAppointment_delegatesCorrectly() {
            // Arrange
            CreateUnplannedAppointmentRequestDTO request = mock(CreateUnplannedAppointmentRequestDTO.class);
            AppointmentResponseDTO expected = mock(AppointmentResponseDTO.class);
            when(appointmentService.createUnplannedAppointment(request)).thenReturn(expected);

            // Act
            AppointmentResponseDTO result = sut.registerUnplannedAppointment(request);

            // Assert
            assertThat(result).isEqualTo(expected);
            verify(appointmentService, times(1)).createUnplannedAppointment(request);
            verifyNoMoreInteractions(appointmentService);
        }

        @Test
        @DisplayName("cancelAppointment() delega a appointmentService.cancelAppointment() con los mismos args")
        void cancelAppointment_delegatesCorrectly() {
            // Arrange
            CancelAppointmentRequestDTO request = mock(CancelAppointmentRequestDTO.class);
            AppointmentResponseDTO expected = mock(AppointmentResponseDTO.class);
            when(appointmentService.cancelAppointment(APPOINTMENT_ID, request)).thenReturn(expected);

            // Act
            AppointmentResponseDTO result = sut.cancelAppointment(APPOINTMENT_ID, request);

            // Assert
            assertThat(result).isEqualTo(expected);
            verify(appointmentService, times(1)).cancelAppointment(APPOINTMENT_ID, request);
            verifyNoMoreInteractions(appointmentService);
        }

        @Test
        @DisplayName("changeTechnician() delega a appointmentService.updateTechnician() con los mismos args")
        void changeTechnician_delegatesCorrectly() {
            // Arrange
            UpdateAppointmentTechnicianRequestDTO request = mock(UpdateAppointmentTechnicianRequestDTO.class);
            AppointmentResponseDTO expected = mock(AppointmentResponseDTO.class);
            when(appointmentService.updateTechnician(APPOINTMENT_ID, request)).thenReturn(expected);

            // Act
            AppointmentResponseDTO result = sut.changeTechnician(APPOINTMENT_ID, request);

            // Assert
            assertThat(result).isEqualTo(expected);
            verify(appointmentService, times(1)).updateTechnician(APPOINTMENT_ID, request);
            verifyNoMoreInteractions(appointmentService);
        }
    }

    // ================================================================
    // CONSULTAS
    // ================================================================

    @Nested
    @DisplayName("Consultas")
    class QueryTests {

        @Test
        @DisplayName("getAppointmentById() delega a appointmentService.getAppointmentById()")
        void getAppointmentById_delegatesCorrectly() {
            // Arrange
            AppointmentResponseDTO expected = mock(AppointmentResponseDTO.class);
            when(appointmentService.getAppointmentById(APPOINTMENT_ID)).thenReturn(expected);

            // Act
            AppointmentResponseDTO result = sut.getAppointmentById(APPOINTMENT_ID);

            // Assert
            assertThat(result).isEqualTo(expected);
            verify(appointmentService, times(1)).getAppointmentById(APPOINTMENT_ID);
            verifyNoMoreInteractions(appointmentService);
        }

        @Test
        @DisplayName("getClientAppointmentHistory() delega a appointmentService.getAppointmentsByClient()")
        void getClientAppointmentHistory_delegatesCorrectly() {
            // Arrange
            List<AppointmentResponseDTO> expected = List.of(mock(AppointmentResponseDTO.class));
            when(appointmentService.getAppointmentsByClient(CLIENT_ID)).thenReturn(expected);

            // Act
            List<AppointmentResponseDTO> result = sut.getClientAppointmentHistory(CLIENT_ID);

            // Assert
            assertThat(result).isEqualTo(expected);
            verify(appointmentService, times(1)).getAppointmentsByClient(CLIENT_ID);
            verifyNoMoreInteractions(appointmentService);
        }

        @Test
        @DisplayName("getVehicleAppointmentHistory() delega a appointmentService.getAppointmentsByVehicle()")
        void getVehicleAppointmentHistory_delegatesCorrectly() {
            // Arrange
            List<AppointmentResponseDTO> expected = List.of(mock(AppointmentResponseDTO.class));
            when(appointmentService.getAppointmentsByVehicle(VEHICLE_ID)).thenReturn(expected);

            // Act
            List<AppointmentResponseDTO> result = sut.getVehicleAppointmentHistory(VEHICLE_ID);

            // Assert
            assertThat(result).isEqualTo(expected);
            verify(appointmentService, times(1)).getAppointmentsByVehicle(VEHICLE_ID);
            verifyNoMoreInteractions(appointmentService);
        }
    }
}