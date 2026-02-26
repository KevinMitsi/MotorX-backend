package com.sparktech.motorx.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sparktech.motorx.Services.IAdminService;
import com.sparktech.motorx.controller.error.GlobalControllerAdvice;
import com.sparktech.motorx.dto.appointment.*;
import com.sparktech.motorx.entity.AppointmentStatus;
import com.sparktech.motorx.entity.AppointmentType;
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

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AdminController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalControllerAdvice.class, AdminControllerTest.TestConfig.class})
@DisplayName("AdminController - Tests")
class AdminControllerTest {

    @Resource
    private MockMvc mockMvc;

    @Resource
    private IAdminService adminService;

    private ObjectMapper objectMapper;

    // ---------------------------------------------------------------
    // Helpers / Fixtures
    // ---------------------------------------------------------------

    @BeforeEach
    void setUp() {
        reset(adminService);
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    /**
     * Construye un AppointmentResponseDTO de prueba usando el constructor canónico del record.
     */
    private AppointmentResponseDTO buildAppointmentResponse(Long id) {
        return new AppointmentResponseDTO(
                id,
                AppointmentType.MAINTENANCE,
                AppointmentStatus.SCHEDULED,
                LocalDate.of(2025, 6, 15),
                LocalTime.of(9, 0),
                LocalTime.of(10, 0),
                1L, "ABC123", "Honda", "CB500",
                2L, "Juan Pérez", "juan@mail.com",
                null, null,
                5000, null, null,
                LocalDateTime.of(2025, 6, 1, 8, 0),
                null
        );
    }

    private String json(Object o) throws Exception {
        return objectMapper.writeValueAsString(o);
    }

    // ---------------------------------------------------------------
    // GET /agenda
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("GET /api/v1/admin/appointments/agenda")
    class GetDailyAgenda {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("200 - retorna lista de citas del día")
        void shouldReturnDailyAgenda() throws Exception {
            LocalDate date = LocalDate.of(2025, 6, 15);
            List<AppointmentResponseDTO> appointments = List.of(
                    buildAppointmentResponse(1L),
                    buildAppointmentResponse(2L)
            );
            when(adminService.getDailyAgenda(date)).thenReturn(appointments);

            mockMvc.perform(get("/api/v1/admin/appointments/agenda")
                            .param("date", "2025-06-15"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].id", is(1)))
                    .andExpect(jsonPath("$[1].id", is(2)));

            verify(adminService).getDailyAgenda(date);
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("200 - retorna lista vacía cuando no hay citas")
        void shouldReturnEmptyListWhenNoCitas() throws Exception {
            LocalDate date = LocalDate.of(2025, 6, 15);
            when(adminService.getDailyAgenda(date)).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/admin/appointments/agenda")
                            .param("date", "2025-06-15"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("400 - date param faltante")
        void shouldReturn400WhenDateMissing() throws Exception {
            mockMvc.perform(get("/api/v1/admin/appointments/agenda"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(adminService);
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("400 - formato de fecha inválido")
        void shouldReturn400WhenDateFormatInvalid() throws Exception {
            mockMvc.perform(get("/api/v1/admin/appointments/agenda")
                            .param("date", "15-06-2025"))
                    .andExpect(status().isBadRequest());
        }
    }

    // ---------------------------------------------------------------
    // GET /calendar
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("GET /api/v1/admin/appointments/calendar")
    class GetCalendarView {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("200 - retorna citas en rango de fechas")
        void shouldReturnCalendarView() throws Exception {
            LocalDate start = LocalDate.of(2025, 6, 1);
            LocalDate end   = LocalDate.of(2025, 6, 30);
            List<AppointmentResponseDTO> appointments = List.of(
                    buildAppointmentResponse(10L),
                    buildAppointmentResponse(11L),
                    buildAppointmentResponse(12L)
            );
            when(adminService.getCalendarView(start, end)).thenReturn(appointments);

            mockMvc.perform(get("/api/v1/admin/appointments/calendar")
                            .param("start", "2025-06-01")
                            .param("end",   "2025-06-30"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(3)));

            verify(adminService).getCalendarView(start, end);
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("400 - start param faltante")
        void shouldReturn400WhenStartMissing() throws Exception {
            mockMvc.perform(get("/api/v1/admin/appointments/calendar")
                            .param("end", "2025-06-30"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("400 - end param faltante")
        void shouldReturn400WhenEndMissing() throws Exception {
            mockMvc.perform(get("/api/v1/admin/appointments/calendar")
                            .param("start", "2025-06-01"))
                    .andExpect(status().isBadRequest());
        }
    }

    // ---------------------------------------------------------------
    // GET /available-slots
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("GET /api/v1/admin/appointments/available-slots")
    class GetAvailableSlots {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("200 - retorna slots disponibles")
        void shouldReturnAvailableSlots() throws Exception {
            LocalDate date = LocalDate.of(2025, 6, 15);
            AvailableSlotsResponseDTO response = new AvailableSlotsResponseDTO(
                    date,
                    AppointmentType.MAINTENANCE,
                    List.of(
                            new AvailableSlotsResponseDTO.AvailableSlotDTO(LocalTime.of(9, 0),  LocalTime.of(10, 0), 2),
                            new AvailableSlotsResponseDTO.AvailableSlotDTO(LocalTime.of(10, 0), LocalTime.of(11, 0), 1)
                    )
            );
            when(adminService.getAvailableSlots(date, AppointmentType.MAINTENANCE)).thenReturn(response);

            mockMvc.perform(get("/api/v1/admin/appointments/available-slots")
                            .param("date", "2025-06-15")
                            .param("type", "MAINTENANCE"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.availableSlots", hasSize(2)));

            verify(adminService).getAvailableSlots(date, AppointmentType.MAINTENANCE);
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("400 - tipo de cita inválido")
        void shouldReturn400WhenTypeInvalid() throws Exception {
            mockMvc.perform(get("/api/v1/admin/appointments/available-slots")
                            .param("date", "2025-06-15")
                            .param("type", "INVALID_TYPE"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("400 - date param faltante")
        void shouldReturn400WhenDateMissing() throws Exception {
            mockMvc.perform(get("/api/v1/admin/appointments/available-slots")
                            .param("type", "MAINTENANCE"))
                    .andExpect(status().isBadRequest());
        }
    }

    // ---------------------------------------------------------------
    // POST /unplanned
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("POST /api/v1/admin/appointments/unplanned")
    class RegisterUnplannedAppointment {

        private CreateUnplannedAppointmentRequestDTO buildValidRequest() {
            return new CreateUnplannedAppointmentRequestDTO(
                    5L,
                    AppointmentType.MAINTENANCE,
                    LocalDate.of(2025, 6, 15),
                    LocalTime.of(9, 0),
                    5000,
                    null,
                    null
            );
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("201 - cita no planeada registrada exitosamente")
        void shouldRegisterUnplannedAppointment() throws Exception {
            AppointmentResponseDTO response = buildAppointmentResponse(99L);
            when(adminService.registerUnplannedAppointment(any(CreateUnplannedAppointmentRequestDTO.class)))
                    .thenReturn(response);

            mockMvc.perform(post("/api/v1/admin/appointments/unplanned")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(buildValidRequest())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", is(99)));

            verify(adminService).registerUnplannedAppointment(any(CreateUnplannedAppointmentRequestDTO.class));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("201 - con técnico asignado manualmente")
        void shouldRegisterWithManualTechnician() throws Exception {
            CreateUnplannedAppointmentRequestDTO req = new CreateUnplannedAppointmentRequestDTO(
                    5L, AppointmentType.MAINTENANCE,
                    LocalDate.of(2025, 6, 15), LocalTime.of(9, 0),
                    5000, 3L, null
            );
            AppointmentResponseDTO response = buildAppointmentResponse(100L);
            when(adminService.registerUnplannedAppointment(any())).thenReturn(response);

            mockMvc.perform(post("/api/v1/admin/appointments/unplanned")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", is(100)));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("400 - body vacío")
        void shouldReturn400WhenBodyEmpty() throws Exception {
            mockMvc.perform(post("/api/v1/admin/appointments/unplanned")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("400 - vehicleId nulo")
        void shouldReturn400WhenVehicleIdNull() throws Exception {
            CreateUnplannedAppointmentRequestDTO req = new CreateUnplannedAppointmentRequestDTO(
                    null, AppointmentType.MAINTENANCE,
                    LocalDate.of(2025, 6, 15), LocalTime.of(9, 0),
                    5000, null, null
            );
            mockMvc.perform(post("/api/v1/admin/appointments/unplanned")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("400 - appointmentDate nula")
        void shouldReturn400WhenDateNull() throws Exception {
            CreateUnplannedAppointmentRequestDTO req = new CreateUnplannedAppointmentRequestDTO(
                    5L, AppointmentType.MAINTENANCE,
                    null, LocalTime.of(9, 0),
                    5000, null, null
            );
            mockMvc.perform(post("/api/v1/admin/appointments/unplanned")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ---------------------------------------------------------------
    // PATCH /{appointmentId}/cancel
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("PATCH /api/v1/admin/appointments/{id}/cancel")
    class CancelAppointment {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("200 - cita cancelada exitosamente con notificación")
        void shouldCancelAppointmentWithNotification() throws Exception {
            CancelAppointmentRequestDTO req = new CancelAppointmentRequestDTO(
                    "Cliente no se presentó", true
            );
            AppointmentResponseDTO response = new AppointmentResponseDTO(
                    1L, AppointmentType.MAINTENANCE, AppointmentStatus.CANCELLED,
                    LocalDate.of(2025, 6, 15), LocalTime.of(9, 0), LocalTime.of(10, 0),
                    1L, "ABC123", "Honda", "CB500",
                    2L, "Juan Pérez", "juan@mail.com",
                    null, null, 5000, null, null,
                    LocalDateTime.of(2025, 6, 1, 8, 0), null
            );
            when(adminService.cancelAppointment(eq(1L), any(CancelAppointmentRequestDTO.class)))
                    .thenReturn(response);

            mockMvc.perform(patch("/api/v1/admin/appointments/1/cancel")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("CANCELLED")));

            verify(adminService).cancelAppointment(eq(1L), any(CancelAppointmentRequestDTO.class));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("200 - cita cancelada sin notificación")
        void shouldCancelAppointmentWithoutNotification() throws Exception {
            CancelAppointmentRequestDTO req = new CancelAppointmentRequestDTO("Taller cerrado", false);
            AppointmentResponseDTO response = new AppointmentResponseDTO(
                    2L, AppointmentType.MAINTENANCE, AppointmentStatus.CANCELLED,
                    LocalDate.of(2025, 6, 15), LocalTime.of(9, 0), LocalTime.of(10, 0),
                    1L, "ABC123", "Honda", "CB500",
                    2L, "Juan Pérez", "juan@mail.com",
                    null, null, 5000, null, null,
                    LocalDateTime.of(2025, 6, 1, 8, 0), null
            );
            when(adminService.cancelAppointment(eq(2L), any())).thenReturn(response);

            mockMvc.perform(patch("/api/v1/admin/appointments/2/cancel")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("400 - body vacío sin campos requeridos")
        void shouldReturn400WhenBodyInvalid() throws Exception {
            mockMvc.perform(patch("/api/v1/admin/appointments/1/cancel")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }
    }

    // ---------------------------------------------------------------
    // PATCH /{appointmentId}/technician
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("PATCH /api/v1/admin/appointments/{id}/technician")
    class ChangeTechnician {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("200 - técnico cambiado exitosamente")
        void shouldChangeTechnicianSuccessfully() throws Exception {
            UpdateAppointmentTechnicianRequestDTO req = new UpdateAppointmentTechnicianRequestDTO(7L, true);
            AppointmentResponseDTO response = buildAppointmentResponse(1L);
            when(adminService.changeTechnician(eq(1L), any(UpdateAppointmentTechnicianRequestDTO.class)))
                    .thenReturn(response);

            mockMvc.perform(patch("/api/v1/admin/appointments/1/technician")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(1)));

            verify(adminService).changeTechnician(eq(1L), any(UpdateAppointmentTechnicianRequestDTO.class));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("400 - newTechnicianId nulo")
        void shouldReturn400WhenTechnicianIdNull() throws Exception {
            String bodyWithNullTechnician = "{\"newTechnicianId\": null, \"notifyClient\": true}";

            mockMvc.perform(patch("/api/v1/admin/appointments/1/technician")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(bodyWithNullTechnician))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("400 - body vacío")
        void shouldReturn400WhenBodyEmpty() throws Exception {
            mockMvc.perform(patch("/api/v1/admin/appointments/1/technician")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }
    }


    @Nested
    @DisplayName("GET /api/v1/admin/appointments/{appointmentId}")
    class GetAppointmentById {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("200 - retorna detalle de la cita")
        void shouldReturnAppointmentDetail() throws Exception {
            AppointmentResponseDTO response = buildAppointmentResponse(5L);
            when(adminService.getAppointmentById(5L)).thenReturn(response);

            mockMvc.perform(get("/api/v1/admin/appointments/5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(5)));

            verify(adminService).getAppointmentById(5L);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/appointments/client/{clientId}")
    class GetClientHistory {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("200 - retorna historial del cliente")
        void shouldReturnClientHistory() throws Exception {
            List<AppointmentResponseDTO> history = List.of(
                    buildAppointmentResponse(1L),
                    buildAppointmentResponse(2L)
            );
            when(adminService.getClientAppointmentHistory(10L)).thenReturn(history);

            mockMvc.perform(get("/api/v1/admin/appointments/client/10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)));

            verify(adminService).getClientAppointmentHistory(10L);
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("200 - lista vacía cuando cliente no tiene historial")
        void shouldReturnEmptyHistoryWhenNoAppointments() throws Exception {
            when(adminService.getClientAppointmentHistory(99L)).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/admin/appointments/client/99"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }


    @Nested
    @DisplayName("GET /api/v1/admin/appointments/vehicle/{vehicleId}")
    class GetVehicleHistory {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("200 - retorna historial del vehículo")
        void shouldReturnVehicleHistory() throws Exception {
            List<AppointmentResponseDTO> history = List.of(buildAppointmentResponse(20L));
            when(adminService.getVehicleAppointmentHistory(7L)).thenReturn(history);

            mockMvc.perform(get("/api/v1/admin/appointments/vehicle/7"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].id", is(20)));

            verify(adminService).getVehicleAppointmentHistory(7L);
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("200 - lista vacía cuando vehículo no tiene historial")
        void shouldReturnEmptyWhenNoHistory() throws Exception {
            when(adminService.getVehicleAppointmentHistory(999L)).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/admin/appointments/vehicle/999"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    // ---------------------------------------------------------------
    // TestConfiguration: beans mock con @Primary
    // ---------------------------------------------------------------

    @TestConfiguration
    static class TestConfig {

        @Bean
        @Primary
        IAdminService adminService() {
            return mock(IAdminService.class);
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
