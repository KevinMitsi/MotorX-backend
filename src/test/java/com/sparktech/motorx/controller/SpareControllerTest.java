package com.sparktech.motorx.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparktech.motorx.Services.IMetricsService;
import com.sparktech.motorx.Services.ISpareService;
import com.sparktech.motorx.controller.error.GlobalControllerAdvice;
import com.sparktech.motorx.dto.inventory.CreateSpareDTO;
import com.sparktech.motorx.dto.inventory.SpareResponseDTO;
import com.sparktech.motorx.dto.inventory.UpdateSpareDTO;
import com.sparktech.motorx.dto.inventory.UpdateSparePurchasePriceDTO;
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

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SpareController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalControllerAdvice.class, SpareControllerTest.TestConfig.class})
@DisplayName("SpareController - Tests")
class SpareControllerTest {

    @Resource
    private MockMvc mockMvc;

    @Resource
    private ISpareService spareService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        reset(spareService);
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/v1/spares retorna 201")
    void shouldCreateSpare() throws Exception {
        CreateSpareDTO request = new CreateSpareDTO("Filtro", "AKT", "SAV-1", "REP-1", new BigDecimal("100"), false, "Prov", 3, "01-01-01-01");
        when(spareService.createSpare(any(CreateSpareDTO.class))).thenReturn(response(1L));

        mockMvc.perform(post("/api/v1/spares")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1)));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/v1/spares retorna 400 con body inválido")
    void shouldReturn400WhenCreateRequestInvalid() throws Exception {
        mockMvc.perform(post("/api/v1/spares")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(spareService);
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/v1/spares retorna lista")
    void shouldListSpares() throws Exception {
        when(spareService.getAllSpares()).thenReturn(List.of(response(1L), response(2L)));

        mockMvc.perform(get("/api/v1/spares"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/v1/spares/{id} retorna item")
    void shouldGetSpareById() throws Exception {
        when(spareService.getSpareById(10L)).thenReturn(response(10L));

        mockMvc.perform(get("/api/v1/spares/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(10)));
    }

    @Test
    @WithMockUser
    @DisplayName("PUT /api/v1/spares/{id} actualiza")
    void shouldUpdateSpare() throws Exception {
        UpdateSpareDTO request = new UpdateSpareDTO("Filtro", "AKT", "SAV-1", "REP-1", new BigDecimal("110"), false, "Prov", 4, "01-01-01-01");
        when(spareService.updateSpare(eq(7L), any(UpdateSpareDTO.class))).thenReturn(response(7L));

        mockMvc.perform(put("/api/v1/spares/7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(7)));
    }

    @Test
    @WithMockUser
    @DisplayName("PATCH /api/v1/spares/{id}/purchase-price actualiza precio")
    void shouldUpdatePurchasePrice() throws Exception {
        UpdateSparePurchasePriceDTO request = new UpdateSparePurchasePriceDTO(new BigDecimal("120"));
        when(spareService.updatePurchasePrice(eq(5L), any(UpdateSparePurchasePriceDTO.class))).thenReturn(response(5L));

        mockMvc.perform(patch("/api/v1/spares/5/purchase-price")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(5)));
    }

    @Test
    @WithMockUser
    @DisplayName("DELETE /api/v1/spares/{id} retorna 204")
    void shouldDeleteSpare() throws Exception {
        doNothing().when(spareService).deleteSpare(9L);

        mockMvc.perform(delete("/api/v1/spares/9"))
                .andExpect(status().isNoContent());

        verify(spareService).deleteSpare(9L);
    }

    private SpareResponseDTO response(Long id) {
        return new SpareResponseDTO(id, "Filtro", "AKT", "SAV-1", "REP-1", new BigDecimal("100"), new BigDecimal("135"), false, "Prov", 3, "01-01-01-01");
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        ISpareService spareService() {
            return mock(ISpareService.class);
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

