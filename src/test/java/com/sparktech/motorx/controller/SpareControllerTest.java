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
    @WithMockUser(roles = "WAREHOUSE_WORKER")
    @DisplayName("POST /api/v1/spares retorna 201")
    void shouldCreateSpare() throws Exception {
        CreateSpareDTO request = new CreateSpareDTO("Filtro", "AKT", "SAV-1", "REP-1", new BigDecimal("100"), false, "Prov", 3, 2, "01-01-01-01");
        when(spareService.createSpare(any(CreateSpareDTO.class))).thenReturn(response(1L));

        mockMvc.perform(post("/api/v1/spares")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1)));
    }

    @Test
    @WithMockUser(roles = "WAREHOUSE_WORKER")
    @DisplayName("POST /api/v1/spares retorna 400 con body inválido")
    void shouldReturn400WhenCreateRequestInvalid() throws Exception {
        mockMvc.perform(post("/api/v1/spares")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(spareService);
    }

    @Test
    @WithMockUser(roles = "RECEPTIONIST")
    @DisplayName("GET /api/v1/spares retorna lista")
    void shouldListSpares() throws Exception {
        when(spareService.getAllSpares()).thenReturn(List.of(response(1L), response(2L)));

        mockMvc.perform(get("/api/v1/spares"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @WithMockUser(roles = "RECEPTIONIST")
    @DisplayName("GET /api/v1/spares con filtros delega busqueda por nombre y SAV")
    void shouldSearchSparesByOptionalFilters() throws Exception {
        when(spareService.searchSpares("filtro", "SAV-1")).thenReturn(List.of(response(1L)));

        mockMvc.perform(get("/api/v1/spares")
                        .param("name", "filtro")
                        .param("savCode", "SAV-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(1)));

        verify(spareService).searchSpares("filtro", "SAV-1");
        verify(spareService, never()).getAllSpares();
    }

    @Test
    @WithMockUser(roles = "RECEPTIONIST")
    @DisplayName("GET /api/v1/spares/{id} retorna item")
    void shouldGetSpareById() throws Exception {
        when(spareService.getSpareById(10L)).thenReturn(response(10L));

        mockMvc.perform(get("/api/v1/spares/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(10)));
    }

    @Test
    @WithMockUser(roles = "WAREHOUSE_WORKER")
    @DisplayName("PUT /api/v1/spares/{id} actualiza")
    void shouldUpdateSpare() throws Exception {
        UpdateSpareDTO request = new UpdateSpareDTO("Filtro", "AKT", "SAV-1", "REP-1", new BigDecimal("110"), false, "Prov", 4, 3, "01-01-01-01");
        when(spareService.updateSpare(eq(7L), any(UpdateSpareDTO.class))).thenReturn(response(7L));

        mockMvc.perform(put("/api/v1/spares/7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(7)));
    }

    @Test
    @WithMockUser(roles = "WAREHOUSE_WORKER")
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
    @WithMockUser(roles = "ADMIN")
    @DisplayName("DELETE /api/v1/spares/{id} retorna 204")
    void shouldDeleteSpare() throws Exception {
        doNothing().when(spareService).deleteSpare(9L);

        mockMvc.perform(delete("/api/v1/spares/9"))
                .andExpect(status().isNoContent());

        verify(spareService).deleteSpare(9L);
    }

    @Test
    @WithMockUser(roles = "RECEPTIONIST")
    @DisplayName("GET /api/v1/spares/below-threshold retorna lista")
    void shouldGetBelowThreshold() throws Exception {
        when(spareService.getSparesBelowThreshold()).thenReturn(List.of(response(2L)));

        mockMvc.perform(get("/api/v1/spares/below-threshold"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(2)));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/v1/spares/{id}/notify-restock retorna cantidad")
    void shouldNotifyRestock() throws Exception {
        when(spareService.notifyWarehouseWorkersToRestock(6L)).thenReturn(4L);

        mockMvc.perform(post("/api/v1/spares/6/notify-restock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", is(4)));
    }

    private SpareResponseDTO response(Long id) {
        return new SpareResponseDTO(id, "Filtro", "AKT", "SAV-1", "REP-1", new BigDecimal("100"), new BigDecimal("135"), false, "Prov", 3, 2, "01-01-01-01");
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

