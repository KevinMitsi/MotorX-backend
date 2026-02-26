package com.sparktech.motorx.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sparktech.motorx.Services.IUserService;
import com.sparktech.motorx.controller.error.GlobalControllerAdvice;
import com.sparktech.motorx.dto.appointment.*;
import com.sparktech.motorx.entity.AppointmentStatus;
import com.sparktech.motorx.entity.AppointmentType;
import com.sparktech.motorx.exception.*;

import com.sparktech.motorx.security.CustomUserDetailsService;
import com.sparktech.motorx.security.JwtAuthenticationFilter;
import com.sparktech.motorx.security.JwtService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalControllerAdvice.class, UserControllerTest.TestConfig.class})
@DisplayName("UserController - Tests")
class UserControllerTest {

    @Resource
    private MockMvc mockMvc;

    @Resource
    private IUserService userService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        reset(userService);
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    // ---------------------------------------------------------------
    // Fixtures
    // ---------------------------------------------------------------

    private AppointmentResponseDTO buildAppointmentResponse(Long id) {
        return new AppointmentResponseDTO(
                id,
                AppointmentType.MAINTENANCE,
                AppointmentStatus.SCHEDULED,
                LocalDate.now().plusDays(5),
                LocalTime.of(9, 0),
                LocalTime.of(10, 0),
                1L, "ABC123", "Honda", "CB500F",
                2L, "María García", "maria@mail.com",
                null, null,
                15000, null, null,
                LocalDateTime.now(),
                null
        );
    }

    /** Fecha futura válida para pasar la validación @Future */
    private LocalDate futureDate() {
        return LocalDate.now().plusDays(3);
    }

    private CreateAppointmentRequestDTO buildValidCreateRequest() {
        return new CreateAppointmentRequestDTO(
                1L,
                AppointmentType.MAINTENANCE,
                futureDate(),
                LocalTime.of(9, 0),
                15000,
                null
        );
    }

    private String json(Object o) throws Exception {
        return objectMapper.writeValueAsString(o);
    }

    // ---------------------------------------------------------------
    // GET /api/v1/user/appointments/available-slots
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("GET /api/v1/user/appointments/available-slots")
    class GetAvailableSlots {

        @Test
        @WithMockUser
        @DisplayName("200 - retorna slots disponibles para la fecha y tipo")
        void shouldReturnAvailableSlots() throws Exception {
            // Arrange
            LocalDate date = futureDate();
            AvailableSlotsResponseDTO response = new AvailableSlotsResponseDTO(
                    date,
                    AppointmentType.MAINTENANCE,
                    List.of(
                            new AvailableSlotsResponseDTO.AvailableSlotDTO(LocalTime.of(9, 0),  LocalTime.of(10, 0), 3),
                            new AvailableSlotsResponseDTO.AvailableSlotDTO(LocalTime.of(10, 0), LocalTime.of(11, 0), 2)
                    )
            );
            when(userService.getAvailableSlots(date, AppointmentType.MAINTENANCE)).thenReturn(response);

            // Act & Assert
            mockMvc.perform(get("/api/v1/user/appointments/available-slots")
                            .param("date", date.toString())
                            .param("type", "MAINTENANCE"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.appointmentType", is("MAINTENANCE")))
                    .andExpect(jsonPath("$.availableSlots", hasSize(2)))
                    .andExpect(jsonPath("$.availableSlots[0].availableTechnicians", is(3)));

            verify(userService).getAvailableSlots(date, AppointmentType.MAINTENANCE);
        }

        @Test
        @WithMockUser
        @DisplayName("200 - retorna lista vacía cuando no hay slots")
        void shouldReturnEmptySlotsWhenNoneAvailable() throws Exception {
            // Arrange
            LocalDate date = futureDate();
            AvailableSlotsResponseDTO response = new AvailableSlotsResponseDTO(
                    date, AppointmentType.MAINTENANCE, List.of()
            );
            when(userService.getAvailableSlots(date, AppointmentType.MAINTENANCE)).thenReturn(response);

            // Act & Assert
            mockMvc.perform(get("/api/v1/user/appointments/available-slots")
                            .param("date", date.toString())
                            .param("type", "MAINTENANCE"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.availableSlots", hasSize(0)));
        }

        @Test
        @WithMockUser
        @DisplayName("400 - date param faltante")
        void shouldReturn400WhenDateMissing() throws Exception {
            mockMvc.perform(get("/api/v1/user/appointments/available-slots")
                            .param("type", "MAINTENANCE"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(userService);
        }

        @Test
        @WithMockUser
        @DisplayName("400 - type param faltante")
        void shouldReturn400WhenTypeMissing() throws Exception {
            mockMvc.perform(get("/api/v1/user/appointments/available-slots")
                            .param("date", futureDate().toString()))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(userService);
        }

        @Test
        @WithMockUser
        @DisplayName("400 - tipo de cita con valor inválido")
        void shouldReturn400WhenTypeInvalid() throws Exception {
            mockMvc.perform(get("/api/v1/user/appointments/available-slots")
                            .param("date", futureDate().toString())
                            .param("type", "TIPO_INVALIDO"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(userService);
        }

        @Test
        @WithMockUser
        @DisplayName("400 - formato de fecha inválido")
        void shouldReturn400WhenDateFormatInvalid() throws Exception {
            mockMvc.perform(get("/api/v1/user/appointments/available-slots")
                            .param("date", "15-06-2025")
                            .param("type", "MAINTENANCE"))
                    .andExpect(status().isBadRequest());
        }
    }

    // ---------------------------------------------------------------
    // GET /api/v1/user/appointments/check-plate-restriction
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("GET /api/v1/user/appointments/check-plate-restriction")
    class CheckPlateRestriction {

        @Test
        @WithMockUser
        @DisplayName("409 - vehículo con pico y placa ese día")
        void shouldReturn409WhenPlateRestricted() throws Exception {
            // Arrange
            LocalDate date = futureDate();
            LicensePlateRestrictionResponseDTO response =
                    LicensePlateRestrictionResponseDTO.of("ABC123", date);
            when(userService.checkLicensePlateRestriction(1L, date)).thenReturn(response);

            // Act & Assert
            mockMvc.perform(get("/api/v1/user/appointments/check-plate-restriction")
                            .param("vehicleId", "1")
                            .param("date", date.toString()))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.vehiclePlate", is("ABC123")))
                    .andExpect(jsonPath("$.message", containsString("restricción de movilidad")))
                    .andExpect(jsonPath("$.phoneNumber", is("+57 310 8402499")))
                    .andExpect(jsonPath("$.urgentContactMessage", notNullValue()));

            verify(userService).checkLicensePlateRestriction(1L, date);
        }

        @Test
        @WithMockUser
        @DisplayName("409 - vehículo sin restricción devuelve 409 igualmente (comportamiento del controller)")
        void shouldReturn409WithNoRestrictionMessage() throws Exception {
            // Arrange — el controller siempre devuelve 409 independientemente del resultado
            LocalDate date = futureDate();
            LicensePlateRestrictionResponseDTO response =
                    LicensePlateRestrictionResponseDTO.noRestriction("XYZ789", date);
            when(userService.checkLicensePlateRestriction(2L, date)).thenReturn(response);

            // Act & Assert
            mockMvc.perform(get("/api/v1/user/appointments/check-plate-restriction")
                            .param("vehicleId", "2")
                            .param("date", date.toString()))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.vehiclePlate", is("XYZ789")))
                    .andExpect(jsonPath("$.message", containsString("no tiene restricción")))
                    .andExpect(jsonPath("$.urgentContactMessage", nullValue()))
                    .andExpect(jsonPath("$.phoneNumber", nullValue()));
        }

        @Test
        @WithMockUser
        @DisplayName("404 - vehículo no encontrado")
        void shouldReturn404WhenVehicleNotFound() throws Exception {
            // Arrange
            LocalDate date = futureDate();
            when(userService.checkLicensePlateRestriction(999L, date))
                    .thenThrow(new VehicleNotFoundException("Vehículo no encontrado"));

            // Act & Assert
            mockMvc.perform(get("/api/v1/user/appointments/check-plate-restriction")
                            .param("vehicleId", "999")
                            .param("date", date.toString()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser
        @DisplayName("400 - vehicleId param faltante")
        void shouldReturn400WhenVehicleIdMissing() throws Exception {
            mockMvc.perform(get("/api/v1/user/appointments/check-plate-restriction")
                            .param("date", futureDate().toString()))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(userService);
        }

        @Test
        @WithMockUser
        @DisplayName("400 - date param faltante")
        void shouldReturn400WhenDateMissing() throws Exception {
            mockMvc.perform(get("/api/v1/user/appointments/check-plate-restriction")
                            .param("vehicleId", "1"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(userService);
        }
    }

    // ---------------------------------------------------------------
    // GET /api/v1/user/appointments/rework-info
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("GET /api/v1/user/appointments/rework-info")
    class GetReworkInfo {

        @Test
        @WithMockUser
        @DisplayName("200 - retorna información de contacto para reprocesos")
        void shouldReturnReworkRedirectInfo() throws Exception {
            // Arrange
            ReworkRedirectResponseDTO response = ReworkRedirectResponseDTO.defaultResponse();
            when(userService.getReworkRedirectInfo()).thenReturn(response);

            // Act & Assert
            mockMvc.perform(get("/api/v1/user/appointments/rework-info"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message", notNullValue()))
                    .andExpect(jsonPath("$.whatsappLink", is("https://wa.me/573108402499")))
                    .andExpect(jsonPath("$.phoneNumber", is("+57 310 8402499")))
                    .andExpect(jsonPath("$.businessHours", notNullValue()));

            verify(userService).getReworkRedirectInfo();
        }
    }

    // ---------------------------------------------------------------
    // POST /api/v1/user/appointments
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("POST /api/v1/user/appointments")
    class ScheduleAppointment {

        @Test
        @WithMockUser
        @DisplayName("201 - cita agendada exitosamente con técnico asignado automáticamente")
        void shouldScheduleAppointmentSuccessfully() throws Exception {
            // Arrange
            CreateAppointmentRequestDTO req = buildValidCreateRequest();
            AppointmentResponseDTO response = buildAppointmentResponse(1L);
            when(userService.scheduleAppointment(any(CreateAppointmentRequestDTO.class)))
                    .thenReturn(response);

            // Act & Assert
            mockMvc.perform(post("/api/v1/user/appointments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.type", is("MAINTENANCE")))
                    .andExpect(jsonPath("$.status", is("SCHEDULED")));

            verify(userService).scheduleAppointment(any(CreateAppointmentRequestDTO.class));
        }

        @Test
        @WithMockUser
        @DisplayName("201 - cita agendada con notas del cliente opcionales")
        void shouldScheduleAppointmentWithClientNotes() throws Exception {
            // Arrange
            CreateAppointmentRequestDTO req = new CreateAppointmentRequestDTO(
                    1L, AppointmentType.MAINTENANCE,
                    futureDate(), LocalTime.of(9, 0),
                    15000, Set.of("Ruido en el motor", "Frenos chirrían")
            );
            AppointmentResponseDTO response = buildAppointmentResponse(2L);
            when(userService.scheduleAppointment(any())).thenReturn(response);

            // Act & Assert
            mockMvc.perform(post("/api/v1/user/appointments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", is(2)));
        }

        @Test
        @WithMockUser
        @DisplayName("400 - body vacío falla todas las validaciones")
        void shouldReturn400WhenBodyEmpty() throws Exception {
            mockMvc.perform(post("/api/v1/user/appointments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(userService);
        }

        @Test
        @WithMockUser
        @DisplayName("400 - @NotNull falla con vehicleId nulo")
        void shouldReturn400WhenVehicleIdNull() throws Exception {
            // Arrange
            CreateAppointmentRequestDTO req = new CreateAppointmentRequestDTO(
                    null, AppointmentType.MAINTENANCE,
                    futureDate(), LocalTime.of(9, 0), 15000, null
            );

            // Act & Assert
            mockMvc.perform(post("/api/v1/user/appointments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(userService);
        }

        @Test
        @WithMockUser
        @DisplayName("400 - @NotNull falla con appointmentType nulo")
        void shouldReturn400WhenAppointmentTypeNull() throws Exception {
            // Arrange
            CreateAppointmentRequestDTO req = new CreateAppointmentRequestDTO(
                    1L, null,
                    futureDate(), LocalTime.of(9, 0), 15000, null
            );

            // Act & Assert
            mockMvc.perform(post("/api/v1/user/appointments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(userService);
        }

        @Test
        @WithMockUser
        @DisplayName("400 - @NotNull falla con appointmentDate nula")
        void shouldReturn400WhenDateNull() throws Exception {
            // Arrange
            CreateAppointmentRequestDTO req = new CreateAppointmentRequestDTO(
                    1L, AppointmentType.MAINTENANCE,
                    null, LocalTime.of(9, 0), 15000, null
            );

            // Act & Assert
            mockMvc.perform(post("/api/v1/user/appointments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(userService);
        }

        @Test
        @WithMockUser
        @DisplayName("400 - @Future falla con fecha en el pasado")
        void shouldReturn400WhenDateInPast() throws Exception {
            // Arrange — fecha de ayer no pasa @Future
            CreateAppointmentRequestDTO req = new CreateAppointmentRequestDTO(
                    1L, AppointmentType.MAINTENANCE,
                    LocalDate.now().minusDays(1), LocalTime.of(9, 0), 15000, null
            );

            // Act & Assert
            mockMvc.perform(post("/api/v1/user/appointments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(userService);
        }

        @Test
        @WithMockUser
        @DisplayName("400 - @Future falla con fecha de hoy")
        void shouldReturn400WhenDateIsToday() throws Exception {
            // Arrange — hoy tampoco pasa @Future (requiere estrictamente futuro)
            CreateAppointmentRequestDTO req = new CreateAppointmentRequestDTO(
                    1L, AppointmentType.MAINTENANCE,
                    LocalDate.now(), LocalTime.of(9, 0), 15000, null
            );

            // Act & Assert
            mockMvc.perform(post("/api/v1/user/appointments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(userService);
        }

        @Test
        @WithMockUser
        @DisplayName("400 - @NotNull falla con startTime nulo")
        void shouldReturn400WhenStartTimeNull() throws Exception {
            // Arrange
            CreateAppointmentRequestDTO req = new CreateAppointmentRequestDTO(
                    1L, AppointmentType.MAINTENANCE,
                    futureDate(), null, 15000, null
            );

            // Act & Assert
            mockMvc.perform(post("/api/v1/user/appointments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(userService);
        }

        @Test
        @WithMockUser
        @DisplayName("400 - @NotNull falla con currentMileage nulo")
        void shouldReturn400WhenMileageNull() throws Exception {
            // Arrange
            CreateAppointmentRequestDTO req = new CreateAppointmentRequestDTO(
                    1L, AppointmentType.MAINTENANCE,
                    futureDate(), LocalTime.of(9, 0), null, null
            );

            // Act & Assert
            mockMvc.perform(post("/api/v1/user/appointments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(userService);
        }

        @Test
        @WithMockUser
        @DisplayName("400 - @Min(0) falla con kilometraje negativo")
        void shouldReturn400WhenMileageNegative() throws Exception {
            // Arrange — -1 no pasa @Min(value = 0)
            CreateAppointmentRequestDTO req = new CreateAppointmentRequestDTO(
                    1L, AppointmentType.MAINTENANCE,
                    futureDate(), LocalTime.of(9, 0), -1, null
            );

            // Act & Assert
            mockMvc.perform(post("/api/v1/user/appointments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(userService);
        }

        @Test
        @WithMockUser
        @DisplayName("400 - kilometraje en cero es válido (límite exacto de @Min(0))")
        void shouldAcceptZeroMileage() throws Exception {
            // Arrange — 0 SÍ pasa @Min(value = 0)
            CreateAppointmentRequestDTO req = new CreateAppointmentRequestDTO(
                    1L, AppointmentType.MAINTENANCE,
                    futureDate(), LocalTime.of(9, 0), 0, null
            );
            AppointmentResponseDTO response = buildAppointmentResponse(3L);
            when(userService.scheduleAppointment(any())).thenReturn(response);

            // Act & Assert
            mockMvc.perform(post("/api/v1/user/appointments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isCreated());
        }

        @Test
        @WithMockUser
        @DisplayName("404 - vehículo no encontrado")
        void shouldReturn404WhenVehicleNotFound() throws Exception {
            // Arrange
            when(userService.scheduleAppointment(any()))
                    .thenThrow(new VehicleNotFoundException("Vehículo no encontrado"));

            // Act & Assert
            mockMvc.perform(post("/api/v1/user/appointments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(buildValidCreateRequest())))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser
        @DisplayName("409 - sin técnicos disponibles en ese horario")
        void shouldReturn409WhenNoTechnicianAvailable() throws Exception {
            // Arrange
            when(userService.scheduleAppointment(any()))
                    .thenThrow(new NoAvailableTechnicianException(
                            "No hay técnicos disponibles para ese horario"
                    ));

            // Act & Assert
            mockMvc.perform(post("/api/v1/user/appointments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(buildValidCreateRequest())))
                    .andExpect(status().isConflict());
        }

        @Test
        @WithMockUser
        @DisplayName("409 - vehículo con pico y placa en la fecha solicitada")
        void shouldReturn409WhenPlateRestriction() throws Exception {
            // Arrange
            when(userService.scheduleAppointment(any()))
                    .thenThrow(new LicensePlateRestrictionException(
                            "El vehículo tiene restricción de movilidad (pico y placa)"
                    ));

            // Act & Assert
            mockMvc.perform(post("/api/v1/user/appointments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(buildValidCreateRequest())))
                    .andExpect(status().isConflict());
        }
    }

    // ---------------------------------------------------------------
    // GET /api/v1/user/appointments/my
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("GET /api/v1/user/appointments/my")
    class GetMyAppointments {

        @Test
        @WithMockUser
        @DisplayName("200 - retorna historial de citas del cliente autenticado")
        void shouldReturnMyAppointments() throws Exception {
            // Arrange
            List<AppointmentResponseDTO> appointments = List.of(
                    buildAppointmentResponse(1L),
                    buildAppointmentResponse(2L)
            );
            when(userService.getMyAppointmentHistory()).thenReturn(appointments);

            // Act & Assert
            mockMvc.perform(get("/api/v1/user/appointments/my"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].id", is(1)))
                    .andExpect(jsonPath("$[1].id", is(2)));

            verify(userService).getMyAppointmentHistory();
        }

        @Test
        @WithMockUser
        @DisplayName("200 - retorna lista vacía cuando no hay citas")
        void shouldReturnEmptyListWhenNoAppointments() throws Exception {
            // Arrange
            when(userService.getMyAppointmentHistory()).thenReturn(List.of());

            // Act & Assert
            mockMvc.perform(get("/api/v1/user/appointments/my"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }


    @Nested
    @DisplayName("GET /api/v1/user/appointments/my/{appointmentId}")
    class GetMyAppointmentById {

        @Test
        @WithMockUser
        @DisplayName("200 - retorna detalle de la cita")
        void shouldReturnAppointmentDetail() throws Exception {
            // Arrange
            AppointmentResponseDTO response = buildAppointmentResponse(5L);
            when(userService.getMyAppointmentById(5L)).thenReturn(response);

            // Act & Assert
            mockMvc.perform(get("/api/v1/user/appointments/my/5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(5)))
                    .andExpect(jsonPath("$.type", is("MAINTENANCE")))
                    .andExpect(jsonPath("$.status", is("SCHEDULED")));

            verify(userService).getMyAppointmentById(5L);
        }

        @Test
        @WithMockUser
        @DisplayName("404 - cita no encontrada")
        void shouldReturn404WhenAppointmentNotFound() throws Exception {
            // Arrange
            when(userService.getMyAppointmentById(999L))
                    .thenThrow(new AppointmentNotFoundException(999L));

            // Act & Assert
            mockMvc.perform(get("/api/v1/user/appointments/my/999"))
                    .andExpect(status().isNotFound());
        }
    }


    @Nested
    @DisplayName("GET /api/v1/user/appointments/my/vehicle/{vehicleId}")
    class GetMyVehicleAppointments {

        @Test
        @WithMockUser
        @DisplayName("200 - retorna historial de citas del vehículo")
        void shouldReturnVehicleAppointments() throws Exception {
            // Arrange
            List<AppointmentResponseDTO> appointments = List.of(
                    buildAppointmentResponse(10L),
                    buildAppointmentResponse(11L)
            );
            when(userService.getMyVehicleAppointments(7L)).thenReturn(appointments);

            // Act & Assert
            mockMvc.perform(get("/api/v1/user/appointments/my/vehicle/7"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].id", is(10)));

            verify(userService).getMyVehicleAppointments(7L);
        }

        @Test
        @WithMockUser
        @DisplayName("200 - lista vacía cuando el vehículo no tiene citas")
        void shouldReturnEmptyWhenNoAppointments() throws Exception {
            // Arrange
            when(userService.getMyVehicleAppointments(99L)).thenReturn(List.of());

            // Act & Assert
            mockMvc.perform(get("/api/v1/user/appointments/my/vehicle/99"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @WithMockUser
        @DisplayName("404 - vehículo no encontrado")
        void shouldReturn404WhenVehicleNotFound() throws Exception {
            // Arrange
            when(userService.getMyVehicleAppointments(999L))
                    .thenThrow(new VehicleNotFoundException("Vehículo no encontrado"));

            // Act & Assert
            mockMvc.perform(get("/api/v1/user/appointments/my/vehicle/999"))
                    .andExpect(status().isNotFound());
        }
    }


    @Nested
    @DisplayName("DELETE /api/v1/user/appointments/my/{appointmentId}")
    class CancelMyAppointment {

        @Test
        @WithMockUser
        @DisplayName("200 - cita cancelada exitosamente")
        void shouldCancelAppointmentSuccessfully() throws Exception {
            // Arrange
            AppointmentResponseDTO response = new AppointmentResponseDTO(
                    3L, AppointmentType.MAINTENANCE, AppointmentStatus.CANCELLED,
                    LocalDate.now().plusDays(5), LocalTime.of(9, 0), LocalTime.of(10, 0),
                    1L, "ABC123", "Honda", "CB500F",
                    2L, "María García", "maria@mail.com",
                    null, null, 15000, null, null,
                    LocalDateTime.now(), null
            );
            when(userService.cancelMyAppointment(3L)).thenReturn(response);

            // Act & Assert
            mockMvc.perform(delete("/api/v1/user/appointments/my/3"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(3)))
                    .andExpect(jsonPath("$.status", is("CANCELLED")));

            verify(userService).cancelMyAppointment(3L);
        }

        @Test
        @WithMockUser
        @DisplayName("404 - cita no encontrada")
        void shouldReturn404WhenAppointmentNotFound() throws Exception {
            // Arrange
            when(userService.cancelMyAppointment(999L))
                    .thenThrow(new AppointmentNotFoundException(99L));

            // Act & Assert
            mockMvc.perform(delete("/api/v1/user/appointments/my/999"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser
        @DisplayName("403 - la cita no pertenece al usuario autenticado")
        void shouldReturn403WhenAppointmentNotOwnedByUser() throws Exception {
            // Arrange
            when(userService.cancelMyAppointment(5L))
                    .thenThrow(new AppointmentException(
                            "La cita no pertenece al usuaio"
                    ));

            // Act & Assert
            mockMvc.perform(delete("/api/v1/user/appointments/my/5"))
                    .andExpect(status().isForbidden());
        }
    }

    // ---------------------------------------------------------------
    // TestConfiguration
    // ---------------------------------------------------------------

    @TestConfiguration
    static class TestConfig {

        @Bean
        @Primary
        IUserService userService() {
            return mock(IUserService.class);
        }

        @Bean
        @Primary
        JwtService jwtService() {
            return mock(JwtService.class);
        }

        @Bean
        @Primary
        JwtAuthenticationFilter jwtAuthenticationFilter() {
            return mock(JwtAuthenticationFilter.class);
        }

        @Bean
        @Primary
        CustomUserDetailsService customUserDetailsService() {
            return mock(CustomUserDetailsService.class);
        }
    }
}