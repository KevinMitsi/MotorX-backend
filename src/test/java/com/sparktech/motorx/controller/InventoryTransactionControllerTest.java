package com.sparktech.motorx.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sparktech.motorx.Services.IInventoryTransactionService;
import com.sparktech.motorx.Services.IMetricsService;
import com.sparktech.motorx.controller.error.GlobalControllerAdvice;
import com.sparktech.motorx.dto.inventory.*;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = InventoryTransactionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalControllerAdvice.class, InventoryTransactionControllerTest.TestConfig.class})
@DisplayName("InventoryTransactionController - Tests")
class InventoryTransactionControllerTest {

    @Resource
    private MockMvc mockMvc;

    @Resource
    private IInventoryTransactionService inventoryTransactionService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        reset(inventoryTransactionService);
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Test
    @WithMockUser(roles = "WAREHOUSE_WORKER")
    @DisplayName("POST /api/v1/inventory/purchases retorna 201")
    void shouldRegisterPurchase() throws Exception {
        CreatePurchaseTransactionDTO request = new CreatePurchaseTransactionDTO("Proveedor", List.of(new CreatePurchaseItemDTO(1L, 2, new BigDecimal("100"))));
        when(inventoryTransactionService.registerPurchase(any(CreatePurchaseTransactionDTO.class))).thenReturn(purchaseResponse(1L));

        mockMvc.perform(post("/api/v1/inventory/purchases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1)));
    }

    @Test
    @WithMockUser(roles = "WAREHOUSE_WORKER")
    @DisplayName("POST /api/v1/inventory/purchases retorna 400 para body inválido")
    void shouldReturn400WhenPurchaseBodyInvalid() throws Exception {
        mockMvc.perform(post("/api/v1/inventory/purchases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(inventoryTransactionService);
    }

    @Test
    @WithMockUser(roles = "WAREHOUSE_WORKER")
    @DisplayName("GET /api/v1/inventory/purchases retorna lista")
    void shouldListPurchases() throws Exception {
        when(inventoryTransactionService.getPurchases()).thenReturn(List.of(purchaseResponse(1L), purchaseResponse(2L)));

        mockMvc.perform(get("/api/v1/inventory/purchases"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @WithMockUser(roles = "WAREHOUSE_WORKER")
    @DisplayName("GET /api/v1/inventory/purchases/{id} retorna detalle")
    void shouldGetPurchaseById() throws Exception {
        when(inventoryTransactionService.getPurchaseById(8L)).thenReturn(purchaseResponse(8L));

        mockMvc.perform(get("/api/v1/inventory/purchases/8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(8)));
    }

    @Test
    @WithMockUser(roles = "RECEPTIONIST")
    @DisplayName("POST /api/v1/inventory/sales retorna 201")
    void shouldRegisterSale() throws Exception {
        CreateSaleTransactionDTO request = new CreateSaleTransactionDTO(null, List.of(new CreateSaleItemDTO(1L, 2)));
        when(inventoryTransactionService.registerSale(any(CreateSaleTransactionDTO.class))).thenReturn(saleResponse(1L));

        mockMvc.perform(post("/api/v1/inventory/sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1)));
    }

    @Test
    @WithMockUser(roles = "RECEPTIONIST")
    @DisplayName("GET /api/v1/inventory/sales retorna lista")
    void shouldListSales() throws Exception {
        when(inventoryTransactionService.getSales()).thenReturn(List.of(saleResponse(1L), saleResponse(2L)));

        mockMvc.perform(get("/api/v1/inventory/sales"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @WithMockUser(roles = "RECEPTIONIST")
    @DisplayName("GET /api/v1/inventory/sales/today retorna resumen")
    void shouldGetTodaySalesSummary() throws Exception {
        when(inventoryTransactionService.getTodaySalesSummary()).thenReturn(
                new DailySalesSummaryDTO(LocalDate.now(), new BigDecimal("300"), 2, List.of(saleResponse(1L), saleResponse(2L)))
        );

        mockMvc.perform(get("/api/v1/inventory/sales/today"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionCount", is(2)));
    }

    @Test
    @WithMockUser(roles = "RECEPTIONIST")
    @DisplayName("GET /api/v1/inventory/sales/{id} retorna detalle")
    void shouldGetSaleById() throws Exception {
        when(inventoryTransactionService.getSaleById(3L)).thenReturn(saleResponse(3L));

        mockMvc.perform(get("/api/v1/inventory/sales/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(3)));
    }

    private PurchaseTransactionResponseDTO purchaseResponse(Long id) {
        return new PurchaseTransactionResponseDTO(
                id,
                "Proveedor",
                LocalDateTime.now(),
                1L,
                "admin@test.com",
                new BigDecimal("200"),
                List.of(new PurchaseItemResponseDTO(1L, 1L, "Filtro", 2, new BigDecimal("100"), new BigDecimal("200")))
        );
    }

    private SaleTransactionResponseDTO saleResponse(Long id) {
        return new SaleTransactionResponseDTO(
                id,
                LocalDateTime.now(),
                null,
                1L,
                "admin@test.com",
                new BigDecimal("150"),
                List.of(new SaleItemResponseDTO(1L, 1L, "Filtro", 1, new BigDecimal("150"), new BigDecimal("150")))
        );
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        IInventoryTransactionService inventoryTransactionService() {
            return mock(IInventoryTransactionService.class);
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

