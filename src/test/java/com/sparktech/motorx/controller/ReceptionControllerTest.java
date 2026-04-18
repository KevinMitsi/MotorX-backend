package com.sparktech.motorx.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparktech.motorx.Services.IReceptionService;
import com.sparktech.motorx.Services.IMetricsService;
import com.sparktech.motorx.controller.error.GlobalControllerAdvice;
import com.sparktech.motorx.dto.appointment.AppointmentResponseDTO;
import com.sparktech.motorx.dto.reception.ConfirmReceptionDTO;
import com.sparktech.motorx.entity.AppointmentStatus;
import com.sparktech.motorx.entity.AppointmentType;
import com.sparktech.motorx.security.CustomUserDetailsService;
import com.sparktech.motorx.security.JwtAuthenticationFilter;
import com.sparktech.motorx.security.JwtService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ReceptionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalControllerAdvice.class, ReceptionControllerTest.TestConfig.class})
@DisplayName("ReceptionController - Tests")
class ReceptionControllerTest {

    @Resource
    private MockMvc mockMvc;

    @Resource
    private IReceptionService receptionService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        reset(receptionService);
    }

    @Test
    @WithMockUser(roles = "RECEPTIONIST")
    @DisplayName("POST /api/v1/reception/initiate/{appointmentId} retorna 200")
    void shouldInitiateReception() throws Exception {
        when(receptionService.initiateReception(15L)).thenReturn(response(15L, AppointmentStatus.AWAITING_CONFIRMATION));

        mockMvc.perform(post("/api/v1/reception/initiate/15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(15)))
                .andExpect(jsonPath("$.status", is("AWAITING_CONFIRMATION")));
    }

    @Test
    @WithMockUser(roles = "RECEPTIONIST")
    @DisplayName("POST /api/v1/reception/confirm retorna 200")
    void shouldConfirmReception() throws Exception {
        ConfirmReceptionDTO request = new ConfirmReceptionDTO("ABC123", "1234");
        when(receptionService.confirmReception(any(ConfirmReceptionDTO.class))).thenReturn(response(15L, AppointmentStatus.IN_PROGRESS));

        mockMvc.perform(post("/api/v1/reception/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("IN_PROGRESS")));
    }

    @Test
    @WithMockUser(roles = "RECEPTIONIST")
    @DisplayName("POST /api/v1/reception/confirm retorna 400 si código inválido")
    void shouldReturn400WhenConfirmBodyInvalid() throws Exception {
        ConfirmReceptionDTO request = new ConfirmReceptionDTO("ABC123", "12");

        mockMvc.perform(post("/api/v1/reception/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(receptionService);
    }

    private AppointmentResponseDTO response(Long id, AppointmentStatus status) {
        return new AppointmentResponseDTO(
                id,
                AppointmentType.MAINTENANCE,
                status,
                LocalDate.now().plusDays(1),
                LocalTime.of(9, 0),
                LocalTime.of(10, 0),
                1L,
                "ABC123",
                "Honda",
                "CB500",
                2L,
                "Cliente",
                "cliente@test.com",
                null,
                null,
                1000,
                null,
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        IReceptionService receptionService() {
            return mock(IReceptionService.class);
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

        @Bean
        @Primary
        IMetricsService metricsService() {
            return mock(IMetricsService.class);
        }
    }
}

