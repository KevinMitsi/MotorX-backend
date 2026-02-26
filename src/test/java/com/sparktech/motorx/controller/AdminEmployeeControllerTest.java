package com.sparktech.motorx.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sparktech.motorx.Services.IEmployeeService;
import com.sparktech.motorx.controller.error.GlobalControllerAdvice;
import com.sparktech.motorx.dto.auth.RegisterUserDTO;
import com.sparktech.motorx.dto.employee.CreateEmployeeRequestDTO;
import com.sparktech.motorx.dto.employee.EmployeeResponseDTO;
import com.sparktech.motorx.dto.employee.UpdateEmployeeRequestDTO;

import com.sparktech.motorx.entity.EmployeePosition;
import com.sparktech.motorx.entity.EmployeeState;
import com.sparktech.motorx.exception.EmployeeNotFoundException;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AdminEmployeeController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalControllerAdvice.class, AdminEmployeeControllerTest.TestConfig.class})
@DisplayName("AdminEmployeeController - Tests")
class AdminEmployeeControllerTest {

    @Resource
    private MockMvc mockMvc;

    @Resource
    private IEmployeeService employeeService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        reset(employeeService);
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    // ---------------------------------------------------------------
    // Fixtures
    // ---------------------------------------------------------------

    private EmployeeResponseDTO buildEmployeeResponse(Long id) {
        LocalDateTime now = LocalDateTime.now();
        return new EmployeeResponseDTO(
                id,
                EmployeePosition.MECANICO,
                EmployeeState.AVAILABLE,
                now.minusDays(30),
                10L,
                "Juan",
                "juan.perez@motorx.com",
                "12345678",
                "555-1234",
                now.minusDays(30),
                now
        );
    }

    private CreateEmployeeRequestDTO buildValidCreateRequest() {
        RegisterUserDTO user = new RegisterUserDTO(
                "Juan",
                "12345678",
                "juan.perez@motorx.com",
                "Password123!",
                "555-1234"
        );
        return new CreateEmployeeRequestDTO(
                EmployeePosition.MECANICO,
                user
        );
    }

    private UpdateEmployeeRequestDTO buildValidUpdateRequest() {
        return new UpdateEmployeeRequestDTO(
                EmployeePosition.MECANICO,
                EmployeeState.NOT_AVAILABLE
        );
    }

    private String json(Object o) throws Exception {
        return objectMapper.writeValueAsString(o);
    }

    // ---------------------------------------------------------------
    // POST /api/v1/admin/employees
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("POST /api/v1/admin/employees")
    class CreateEmployee {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("201 - empleado creado exitosamente")
        void shouldCreateEmployeeSuccessfully() throws Exception {
            // Arrange
            EmployeeResponseDTO response = buildEmployeeResponse(1L);
            when(employeeService.createEmployee(any(CreateEmployeeRequestDTO.class))).thenReturn(response);

            // Act & Assert
            mockMvc.perform(post("/api/v1/admin/employees")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(buildValidCreateRequest())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.employeeId", is(1)))
                    .andExpect(jsonPath("$.userName", is("Juan")))
                    .andExpect(jsonPath("$.userEmail", is("juan.perez@motorx.com")))
                    .andExpect(jsonPath("$.state", is("AVAILABLE")));

            verify(employeeService).createEmployee(any(CreateEmployeeRequestDTO.class));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("400 - body vacío")
        void shouldReturn400WhenBodyEmpty() throws Exception {
            mockMvc.perform(post("/api/v1/admin/employees")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(employeeService);
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("400 - name nulo")
        void shouldReturn400WhenFirstNameNull() throws Exception {
            // Arrange
            RegisterUserDTO user = new RegisterUserDTO(
                    null, "12345678", "juan.perez@motorx.com",
                    "Password123!", "555-1234"
            );
            CreateEmployeeRequestDTO req = new CreateEmployeeRequestDTO(
                    EmployeePosition.MECANICO,
                    user
            );

            // Act & Assert
            mockMvc.perform(post("/api/v1/admin/employees")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(employeeService);
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("400 - email inválido")
        void shouldReturn400WhenEmailInvalid() throws Exception {
            // Arrange
            RegisterUserDTO user = new RegisterUserDTO(
                    "Juan", "12345678", "no-es-un-email",
                    "Password123!", "555-1234"
            );
            CreateEmployeeRequestDTO req = new CreateEmployeeRequestDTO(
                    EmployeePosition.MECANICO,
                    user
            );

            // Act & Assert
            mockMvc.perform(post("/api/v1/admin/employees")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(employeeService);
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("400 - password nula")
        void shouldReturn400WhenPasswordNull() throws Exception {
            // Arrange
            RegisterUserDTO user = new RegisterUserDTO(
                    "Juan", "12345678", "juan.perez@motorx.com",
                    null, "555-1234"
            );
            CreateEmployeeRequestDTO req = new CreateEmployeeRequestDTO(
                    EmployeePosition.MECANICO,
                    user
            );

            // Act & Assert
            mockMvc.perform(post("/api/v1/admin/employees")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("400 - DNI nulo")
        void shouldReturn400WhenDniNull() throws Exception {
            // Arrange
            RegisterUserDTO user = new RegisterUserDTO(
                    "Juan", null, "juan.perez@motorx.com",
                    "Password123!", "555-1234"
            );
            CreateEmployeeRequestDTO req = new CreateEmployeeRequestDTO(
                    EmployeePosition.MECANICO,
                    user
            );

            // Act & Assert
            mockMvc.perform(post("/api/v1/admin/employees")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ---------------------------------------------------------------
    // GET /api/v1/admin/employees
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("GET /api/v1/admin/employees")
    class GetAllEmployees {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("200 - retorna lista de empleados")
        void shouldReturnAllEmployees() throws Exception {
            // Arrange
            List<EmployeeResponseDTO> employees = List.of(
                    buildEmployeeResponse(1L),
                    buildEmployeeResponse(2L),
                    buildEmployeeResponse(3L)
            );
            when(employeeService.getAllEmployees()).thenReturn(employees);

            // Act & Assert
            mockMvc.perform(get("/api/v1/admin/employees"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(3)))
                    .andExpect(jsonPath("$[0].employeeId", is(1)))
                    .andExpect(jsonPath("$[1].employeeId", is(2)))
                    .andExpect(jsonPath("$[2].employeeId", is(3)));

            verify(employeeService).getAllEmployees();
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("200 - retorna lista vacía cuando no hay empleados")
        void shouldReturnEmptyListWhenNoEmployees() throws Exception {
            // Arrange
            when(employeeService.getAllEmployees()).thenReturn(List.of());

            // Act & Assert
            mockMvc.perform(get("/api/v1/admin/employees"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));

            verify(employeeService).getAllEmployees();
        }
    }


    @Nested
    @DisplayName("GET /api/v1/admin/employees/{employeeId}")
    class GetEmployeeById {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("200 - retorna detalle del empleado")
        void shouldReturnEmployeeDetail() throws Exception {
            // Arrange
            EmployeeResponseDTO response = buildEmployeeResponse(5L);
            when(employeeService.getEmployeeById(5L)).thenReturn(response);

            // Act & Assert
            mockMvc.perform(get("/api/v1/admin/employees/5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.employeeId", is(5)))
                    .andExpect(jsonPath("$.userName", is("Juan")))
                    .andExpect(jsonPath("$.position", is("MECANICO")));

            verify(employeeService).getEmployeeById(5L);
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("404 - empleado no encontrado")
        void shouldReturn404WhenEmployeeNotFound() throws Exception {
            // Arrange
            when(employeeService.getEmployeeById(999L))
                    .thenThrow(new EmployeeNotFoundException(999L));

            // Act & Assert
            mockMvc.perform(get("/api/v1/admin/employees/999"))
                    .andExpect(status().isNotFound());

            verify(employeeService).getEmployeeById(999L);
        }
    }


    @Nested
    @DisplayName("PUT /api/v1/admin/employees/{employeeId}")
    class UpdateEmployee {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("200 - empleado actualizado exitosamente")
        void shouldUpdateEmployeeSuccessfully() throws Exception {
            // Arrange
            UpdateEmployeeRequestDTO req = buildValidUpdateRequest();
            EmployeeResponseDTO response = new EmployeeResponseDTO(
                    1L,
                    EmployeePosition.MECANICO,
                    EmployeeState.NOT_AVAILABLE,
                    LocalDateTime.now().minusDays(30),
                    10L,
                    "Juan",
                    "juan.perez@motorx.com",
                    "12345678",
                    "555-1234",
                    LocalDateTime.now().minusDays(30),
                    LocalDateTime.now()
            );
            when(employeeService.updateEmployee(eq(1L), any(UpdateEmployeeRequestDTO.class)))
                    .thenReturn(response);

            // Act & Assert
            mockMvc.perform(put("/api/v1/admin/employees/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.employeeId", is(1)))
                    .andExpect(jsonPath("$.position", is("MECANICO")))
                    .andExpect(jsonPath("$.state", is("NOT_AVAILABLE")));

            verify(employeeService).updateEmployee(eq(1L), any(UpdateEmployeeRequestDTO.class));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("200 - actualizar a estado AVAILABLE")
        void shouldUpdateEmployeeToAvailable() throws Exception {
            // Arrange
            UpdateEmployeeRequestDTO req = new UpdateEmployeeRequestDTO(EmployeePosition.MECANICO, EmployeeState.AVAILABLE);
            EmployeeResponseDTO response = buildEmployeeResponse(2L);
            when(employeeService.updateEmployee(eq(2L), any())).thenReturn(response);

            // Act & Assert
            mockMvc.perform(put("/api/v1/admin/employees/2")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.state", is("AVAILABLE")));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("400 - body vacío")
        void shouldReturn400WhenBodyEmpty() throws Exception {
            mockMvc.perform(put("/api/v1/admin/employees/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(employeeService);
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("400 - status nulo")
        void shouldReturn400WhenStatusNull() throws Exception {
            // Arrange
            UpdateEmployeeRequestDTO req = new UpdateEmployeeRequestDTO(EmployeePosition.MECANICO, null);

            // Act & Assert
            mockMvc.perform(put("/api/v1/admin/employees/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("404 - empleado no encontrado")
        void shouldReturn404WhenEmployeeNotFound() throws Exception {
            // Arrange
            when(employeeService.updateEmployee(eq(999L), any()))
                    .thenThrow(new EmployeeNotFoundException(999L));

            // Act & Assert
            mockMvc.perform(put("/api/v1/admin/employees/999")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(buildValidUpdateRequest())))
                    .andExpect(status().isNotFound());
        }
    }


    @Nested
    @DisplayName("DELETE /api/v1/admin/employees/{employeeId}")
    class DeleteEmployee {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("204 - empleado eliminado exitosamente")
        void shouldDeleteEmployeeSuccessfully() throws Exception {
            // Arrange
            doNothing().when(employeeService).deleteEmployee(1L);

            // Act & Assert
            mockMvc.perform(delete("/api/v1/admin/employees/1"))
                    .andExpect(status().isNoContent());

            verify(employeeService).deleteEmployee(1L);
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("404 - empleado no encontrado al eliminar")
        void shouldReturn404WhenEmployeeNotFound() throws Exception {
            // Arrange
            doThrow(new EmployeeNotFoundException(999L))
                    .when(employeeService).deleteEmployee(999L);

            // Act & Assert
            mockMvc.perform(delete("/api/v1/admin/employees/999"))
                    .andExpect(status().isNotFound());

            verify(employeeService).deleteEmployee(999L);
        }
    }

    // ---------------------------------------------------------------
    // TestConfiguration
    // ---------------------------------------------------------------

    @TestConfiguration
    static class TestConfig {

        @Bean
        @Primary
        IEmployeeService employeeService() {
            return mock(IEmployeeService.class);
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