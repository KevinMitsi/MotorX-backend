package com.sparktech.motorx.controller;

import com.sparktech.motorx.Services.ILogService;
import com.sparktech.motorx.Services.IMetricsService;
import com.sparktech.motorx.controller.error.GlobalControllerAdvice;
import com.sparktech.motorx.entity.LogActionType;
import com.sparktech.motorx.entity.LogEntity;
import com.sparktech.motorx.entity.LogResult;
import com.sparktech.motorx.entity.LogServiceName;
import com.sparktech.motorx.security.CustomUserDetailsService;
import com.sparktech.motorx.security.JwtAuthenticationFilter;
import com.sparktech.motorx.security.JwtService;
import jakarta.annotation.Resource;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = LogController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalControllerAdvice.class, LogControllerTest.TestConfig.class})
@DisplayName("LogController - Tests")
class LogControllerTest {

    @Resource
    private MockMvc mockMvc;

    @Resource
    private ILogService logService;

    @BeforeEach
    void setUp() {
        reset(logService);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/v1/admin/logs retorna pagina de logs")
    void shouldReturnPagedLogs() throws Exception {
        LogEntity entity = new LogEntity();
        entity.setId(7L);
        entity.setServiceName(LogServiceName.AUTHENTICATION);
        entity.setActionType(LogActionType.LOGIN);
        entity.setResult(LogResult.SUCCESS);
        entity.setActorEmail("admin@motorx.com");
        entity.setActorUserId(5L);
        entity.setMessage("Inicio de sesion exitoso");
        entity.setCreatedAt(LocalDateTime.of(2026, 3, 31, 10, 15));

        Page<@NotNull LogEntity> mockPage = new PageImpl<>(
                List.of(entity),
                PageRequest.of(0, 1, Sort.by(Sort.Order.desc("createdAt"))),
                1
        );

        when(logService.findAll(any())).thenReturn(mockPage);

        mockMvc.perform(get("/api/v1/admin/logs")
                        .param("page", "0")
                        .param("size", "1")
                        .param("sort", "createdAt,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id", is(7)))
                .andExpect(jsonPath("$.content[0].serviceName", is("AUTHENTICATION")))
                .andExpect(jsonPath("$.content[0].actionType", is("LOGIN")))
                .andExpect(jsonPath("$.content[0].result", is("SUCCESS")))
                .andExpect(jsonPath("$.content[0].actorEmail", is("admin@motorx.com")))
                .andExpect(jsonPath("$.content[0].actorUserId", is(5)))
                .andExpect(jsonPath("$.content[0].message", is("Inicio de sesion exitoso")))
                .andExpect(jsonPath("$.page", is(0)))
                .andExpect(jsonPath("$.size", is(1)))
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.totalPages", is(1)))
                .andExpect(jsonPath("$.first", is(true)))
                .andExpect(jsonPath("$.last", is(true)))
                .andExpect(jsonPath("$.empty", is(false)));

        verify(logService).findAll(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/v1/admin/logs retorna pagina vacia")
    void shouldReturnEmptyPage() throws Exception {
        Page<@NotNull LogEntity> mockPage = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
        when(logService.findAll(any())).thenReturn(mockPage);

        mockMvc.perform(get("/api/v1/admin/logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.empty", is(true)))
                .andExpect(jsonPath("$.totalElements", is(0)));

        verify(logService).findAll(any());
    }

    @Test
    @DisplayName("LogController exige rol ADMIN por anotacion")
    void shouldRequireAdminByAnnotation() {
        PreAuthorize preAuthorize = LogController.class.getAnnotation(PreAuthorize.class);
        org.junit.jupiter.api.Assertions.assertNotNull(preAuthorize);
        org.junit.jupiter.api.Assertions.assertEquals("hasRole('ADMIN')", preAuthorize.value());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/v1/admin/logs retorna 500 si el servicio falla")
    void shouldReturnInternalServerErrorWhenServiceThrows() throws Exception {
        when(logService.findAll(any()))
                .thenThrow(new RuntimeException("fallo inesperado"));

        mockMvc.perform(get("/api/v1/admin/logs"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code", is(500)))
                .andExpect(jsonPath("$.message", is("Error interno del servidor")));

        verify(logService).findAll(any());
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        @Primary
        ILogService logService() {
            return mock(ILogService.class);
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

