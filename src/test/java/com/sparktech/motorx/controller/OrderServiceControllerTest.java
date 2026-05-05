package com.sparktech.motorx.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sparktech.motorx.Services.IMetricsService;
import com.sparktech.motorx.Services.IOrderService;
import com.sparktech.motorx.controller.error.GlobalControllerAdvice;
import com.sparktech.motorx.dto.order.AddProcedureToOrderDTO;
import com.sparktech.motorx.dto.order.AddSpareToOrderDTO;
import com.sparktech.motorx.dto.order.OrderResponseDTO;
import com.sparktech.motorx.dto.order.UpdateOrderProcedureCostDTO;
import com.sparktech.motorx.entity.OrderStatus;
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
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = OrderServiceController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalControllerAdvice.class, OrderServiceControllerTest.TestConfig.class})
@DisplayName("OrderServiceController - Tests")
class OrderServiceControllerTest {

    @Resource
    private MockMvc mockMvc;

    @Resource
    private IOrderService orderService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        reset(orderService);
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Test
    @WithMockUser(roles = "TECHNICIAN")
    @DisplayName("POST /api/v1/orders/appointment/{id} retorna 201")
    void shouldCreateOrder() throws Exception {
        when(orderService.createOrder(5L)).thenReturn(response(1L, 5L, OrderStatus.IN_PROGRESS));

        mockMvc.perform(post("/api/v1/orders/appointment/5"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1)));
    }

    @Test
    @WithMockUser(roles = "TECHNICIAN")
    @DisplayName("POST /api/v1/orders/{id}/procedures retorna 200")
    void shouldAddProcedure() throws Exception {
        AddProcedureToOrderDTO request = new AddProcedureToOrderDTO(10L, new BigDecimal("50"));
        when(orderService.addProcedure(eq(3L), any(AddProcedureToOrderDTO.class))).thenReturn(response(3L, 2L, OrderStatus.IN_PROGRESS));

        mockMvc.perform(post("/api/v1/orders/3/procedures")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(3)));
    }

    @Test
    @WithMockUser(roles = "TECHNICIAN")
    @DisplayName("POST /api/v1/orders/{id}/procedures retorna 400 con body invalido")
    void shouldReturn400WhenAddProcedureInvalid() throws Exception {
        mockMvc.perform(post("/api/v1/orders/3/procedures")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(orderService);
    }

    @Test
    @WithMockUser(roles = "TECHNICIAN")
    @DisplayName("PATCH /api/v1/orders/{id}/procedures/{id} retorna 200")
    void shouldUpdateProcedureCost() throws Exception {
        UpdateOrderProcedureCostDTO request = new UpdateOrderProcedureCostDTO(new BigDecimal("80"));
        when(orderService.updateProcedureCost(eq(3L), eq(11L), any(UpdateOrderProcedureCostDTO.class))).thenReturn(response(3L, 2L, OrderStatus.IN_PROGRESS));

        mockMvc.perform(patch("/api/v1/orders/3/procedures/11")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(3)));
    }

    @Test
    @WithMockUser(roles = "TECHNICIAN")
    @DisplayName("POST /api/v1/orders/{id}/spares retorna 200")
    void shouldAddSpare() throws Exception {
        AddSpareToOrderDTO request = new AddSpareToOrderDTO(12L, 2);
        when(orderService.addSpare(eq(3L), any(AddSpareToOrderDTO.class))).thenReturn(response(3L, 2L, OrderStatus.IN_PROGRESS));

        mockMvc.perform(post("/api/v1/orders/3/spares")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(3)));
    }

    @Test
    @WithMockUser(roles = "TECHNICIAN")
    @DisplayName("POST /api/v1/orders/{id}/complete retorna 200")
    void shouldCompleteOrder() throws Exception {
        when(orderService.completeOrder(3L)).thenReturn(response(3L, 2L, OrderStatus.COMPLETED));

        mockMvc.perform(post("/api/v1/orders/3/complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("COMPLETED")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/v1/orders/appointment/{id} retorna 200")
    void shouldGetByAppointment() throws Exception {
        when(orderService.getOrderByAppointment(8L)).thenReturn(response(7L, 8L, OrderStatus.IN_PROGRESS));

        mockMvc.perform(get("/api/v1/orders/appointment/8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appointmentId", is(8)));
    }

    private OrderResponseDTO response(Long orderId, Long appointmentId, OrderStatus status) {
        return new OrderResponseDTO(
                orderId,
                appointmentId,
                15L,
                LocalDateTime.now(),
                null,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                status,
                List.of(),
                List.of()
        );
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        IOrderService orderService() {
            return mock(IOrderService.class);
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
