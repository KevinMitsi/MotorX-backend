package com.sparktech.motorx.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sparktech.motorx.Services.IMetricsService;
import com.sparktech.motorx.Services.INotificationService;
import com.sparktech.motorx.controller.error.GlobalControllerAdvice;
import com.sparktech.motorx.dto.notification.CreateNotificationDTO;
import com.sparktech.motorx.dto.notification.NotificationResponseDTO;
import com.sparktech.motorx.entity.NotificationUrgency;
import com.sparktech.motorx.exception.NotificationNotFoundException;
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

@WebMvcTest(controllers = NotificationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalControllerAdvice.class, NotificationControllerTest.TestConfig.class})
@DisplayName("NotificationController - Tests")
class NotificationControllerTest {

    @Resource
    private MockMvc mockMvc;

    @Resource
    private INotificationService notificationService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        reset(notificationService);
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/v1/notifications/admin retorna 201")
    void shouldCreateNotification() throws Exception {
        CreateNotificationDTO request = new CreateNotificationDTO(5L, "Aviso", "Detalle", NotificationUrgency.HIGH, "INVENTORY");
        when(notificationService.createNotification(any(CreateNotificationDTO.class))).thenReturn(response(1L, 5L, false));

        mockMvc.perform(post("/api/v1/notifications/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.userId", is(5)));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/v1/notifications/my retorna lista")
    void shouldGetMyNotifications() throws Exception {
        when(notificationService.getMyNotifications(false)).thenReturn(List.of(response(1L, 2L, false), response(2L, 2L, true)));

        mockMvc.perform(get("/api/v1/notifications/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @WithMockUser
    @DisplayName("PATCH /api/v1/notifications/my/{id}/read retorna 200")
    void shouldMarkNotificationAsRead() throws Exception {
        when(notificationService.markAsRead(9L)).thenReturn(response(9L, 2L, true));

        mockMvc.perform(patch("/api/v1/notifications/my/9/read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isRead", is(true)));
    }

    @Test
    @WithMockUser
    @DisplayName("PATCH /api/v1/notifications/my/{id}/read retorna 404 cuando no existe")
    void shouldReturn404WhenNotificationNotFound() throws Exception {
        when(notificationService.markAsRead(99L)).thenThrow(new NotificationNotFoundException(99L));

        mockMvc.perform(patch("/api/v1/notifications/my/99/read"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    @DisplayName("PATCH /api/v1/notifications/my/read-all retorna cantidad")
    void shouldMarkAllAsRead() throws Exception {
        when(notificationService.markAllAsRead()).thenReturn(3L);

        mockMvc.perform(patch("/api/v1/notifications/my/read-all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updatedCount", is(3)));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/v1/notifications/admin/user/{id} retorna lista")
    void shouldGetNotificationsByUser() throws Exception {
        when(notificationService.getNotificationsByUserId(7L)).thenReturn(List.of(response(1L, 7L, false)));

        mockMvc.perform(get("/api/v1/notifications/admin/user/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].userId", is(7)));
    }

    private NotificationResponseDTO response(Long id, Long userId, boolean isRead) {
        return new NotificationResponseDTO(
                id,
                userId,
                "Titulo",
                "Descripcion",
                NotificationUrgency.MEDIUM,
                isRead,
                "SYSTEM",
                LocalDateTime.now(),
                isRead ? LocalDateTime.now() : null
        );
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        @Primary
        INotificationService notificationService() {
            return mock(INotificationService.class);
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

