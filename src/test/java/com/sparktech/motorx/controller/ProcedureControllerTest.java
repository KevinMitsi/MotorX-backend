package com.sparktech.motorx.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparktech.motorx.Services.IMetricsService;
import com.sparktech.motorx.Services.IProcedureService;
import com.sparktech.motorx.controller.error.GlobalControllerAdvice;
import com.sparktech.motorx.dto.procedure.CreateProcedureDTO;
import com.sparktech.motorx.dto.procedure.ProcedureResponseDTO;
import com.sparktech.motorx.dto.procedure.UpdateProcedureDTO;
import com.sparktech.motorx.dto.procedure.UpdateServiceProceduresDTO;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ProcedureController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalControllerAdvice.class, ProcedureControllerTest.TestConfig.class})
@DisplayName("ProcedureController - Tests")
class ProcedureControllerTest {

    @Resource
    private MockMvc mockMvc;

    @Resource
    private IProcedureService procedureService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        reset(procedureService);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/v1/procedures retorna 201")
    void shouldCreateProcedure() throws Exception {
        CreateProcedureDTO request = new CreateProcedureDTO("Lavado", "Desc", true);
        when(procedureService.create(any(CreateProcedureDTO.class))).thenReturn(response(1L));

        mockMvc.perform(post("/api/v1/procedures")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1)));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/v1/procedures retorna 400 con body invalido")
    void shouldReturn400WhenCreateInvalid() throws Exception {
        mockMvc.perform(post("/api/v1/procedures")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(procedureService);
    }

    @Test
    @WithMockUser(roles = "TECHNICIAN")
    @DisplayName("GET /api/v1/procedures retorna lista")
    void shouldListProcedures() throws Exception {
        when(procedureService.getAll()).thenReturn(List.of(response(1L), response(2L)));

        mockMvc.perform(get("/api/v1/procedures"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @WithMockUser(roles = "TECHNICIAN")
    @DisplayName("GET /api/v1/procedures/active retorna lista")
    void shouldListActiveProcedures() throws Exception {
        when(procedureService.getActive()).thenReturn(List.of(response(1L)));

        mockMvc.perform(get("/api/v1/procedures/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @WithMockUser(roles = "TECHNICIAN")
    @DisplayName("GET /api/v1/procedures/{id} retorna item")
    void shouldGetById() throws Exception {
        when(procedureService.getById(7L)).thenReturn(response(7L));

        mockMvc.perform(get("/api/v1/procedures/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(7)));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("PUT /api/v1/procedures/{id} retorna 200")
    void shouldUpdateProcedure() throws Exception {
        UpdateProcedureDTO request = new UpdateProcedureDTO("Lavado", "Desc", false);
        when(procedureService.update(eq(4L), any(UpdateProcedureDTO.class))).thenReturn(response(4L));

        mockMvc.perform(put("/api/v1/procedures/4")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(4)));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("PUT /api/v1/procedures/{id} retorna 400 con body invalido")
    void shouldReturn400WhenUpdateInvalid() throws Exception {
        mockMvc.perform(put("/api/v1/procedures/4")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(procedureService);
    }

    @Test
    @WithMockUser(roles = "TECHNICIAN")
    @DisplayName("GET /api/v1/procedures/service/{id} retorna lista")
    void shouldGetByService() throws Exception {
        when(procedureService.getProceduresByService(9L)).thenReturn(List.of(response(1L)));

        mockMvc.perform(get("/api/v1/procedures/service/9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("PUT /api/v1/procedures/service/{id} retorna 200")
    void shouldUpdateServiceProcedures() throws Exception {
        UpdateServiceProceduresDTO request = new UpdateServiceProceduresDTO(List.of(1L, 2L));
        when(procedureService.updateServiceProcedures(eq(9L), any(UpdateServiceProceduresDTO.class))).thenReturn(List.of(response(1L)));

        mockMvc.perform(put("/api/v1/procedures/service/9")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    private ProcedureResponseDTO response(Long id) {
        return new ProcedureResponseDTO(id, "Procedimiento", "Desc", true, LocalDateTime.now(), LocalDateTime.now());
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        IProcedureService procedureService() {
            return mock(IProcedureService.class);
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

