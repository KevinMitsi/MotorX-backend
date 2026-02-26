package com.sparktech.motorx.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sparktech.motorx.Services.IEmployeeService;
import com.sparktech.motorx.controller.error.GlobalControllerAdvice;
import com.sparktech.motorx.dto.vehicle.TransferVehicleOwnershipRequestDTO;
import com.sparktech.motorx.dto.vehicle.VehicleResponseDTO;

import com.sparktech.motorx.exception.UserNotFoundException;
import com.sparktech.motorx.exception.VehicleAlreadyOwnedException;
import com.sparktech.motorx.exception.VehicleNotFoundException;
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

@WebMvcTest(controllers = AdminVehicleController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalControllerAdvice.class, AdminVehicleControllerTest.TestConfig.class})
@DisplayName("AdminVehicleController - Tests")
class AdminVehicleControllerTest {

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


    private VehicleResponseDTO buildVehicleResponse(Long id) {
        return new VehicleResponseDTO(
                id,
                "Honda",
                "CB500F",
                2022,
                "ABC12B",
                500,
                "CHASSIS-001",
                10L,
                "María García",
                "maria.garcia@mail.com",
                LocalDateTime.of(2025, 1, 10, 8, 0),
                LocalDateTime.of(2025, 1, 10, 8, 0)
        );
    }

    private VehicleResponseDTO buildVehicleResponseWithOwner() {
        return new VehicleResponseDTO(
                1L,
                "Yamaha",
                "MT-07",
                2023,
                "XYZ78X",
                700,
                "CHASSIS-002",
                20L,
                "Carlos López",
                "carlos.lopez@mail.com",
                LocalDateTime.of(2025, 2, 1, 9, 0),
                LocalDateTime.of(2025, 4, 1, 10, 0)
        );
    }

    private String json(Object o) throws Exception {
        return objectMapper.writeValueAsString(o);
    }

    // ---------------------------------------------------------------
    // GET /api/v1/admin/vehicles
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("GET /api/v1/admin/vehicles")
    class GetAllVehicles {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("200 - retorna lista completa de vehículos")
        void shouldReturnAllVehicles() throws Exception {
            // Arrange
            List<VehicleResponseDTO> vehicles = List.of(
                    buildVehicleResponse(1L),
                    buildVehicleResponse(2L),
                    buildVehicleResponse(3L)
            );
            when(employeeService.getAllVehicles()).thenReturn(vehicles);

            // Act & Assert
            mockMvc.perform(get("/api/v1/admin/vehicles"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(3)))
                    .andExpect(jsonPath("$[0].id", is(1)))
                    .andExpect(jsonPath("$[0].brand", is("Honda")))
                    .andExpect(jsonPath("$[0].model", is("CB500F")))
                    .andExpect(jsonPath("$[0].licensePlate", is("ABC12B")))
                    .andExpect(jsonPath("$[0].ownerName", is("María García")))
                    .andExpect(jsonPath("$[1].id", is(2)))
                    .andExpect(jsonPath("$[2].id", is(3)));

            verify(employeeService).getAllVehicles();
        }


        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("200 - retorna lista vacía cuando no hay vehículos")
        void shouldReturnEmptyListWhenNoVehicles() throws Exception {
            // Arrange
            when(employeeService.getAllVehicles()).thenReturn(List.of());

            // Act & Assert
            mockMvc.perform(get("/api/v1/admin/vehicles"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));

            verify(employeeService).getAllVehicles();
        }
    }


    @Nested
    @DisplayName("GET /api/v1/admin/vehicles/{vehicleId}")
    class GetVehicleById {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("200 - retorna detalle completo del vehículo")
        void shouldReturnVehicleDetail() throws Exception {
            // Arrange
            VehicleResponseDTO response = buildVehicleResponse(1L);
            when(employeeService.getVehicleById(1L)).thenReturn(response);

            // Act & Assert
            mockMvc.perform(get("/api/v1/admin/vehicles/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.brand", is("Honda")))
                    .andExpect(jsonPath("$.model", is("CB500F")))
                    .andExpect(jsonPath("$.yearOfManufacture", is(2022)))
                    .andExpect(jsonPath("$.licensePlate", is("ABC12B")))
                    .andExpect(jsonPath("$.cylinderCapacity", is(500)))
                    .andExpect(jsonPath("$.chassisNumber", is("CHASSIS-001")))
                    .andExpect(jsonPath("$.ownerId", is(10)))
                    .andExpect(jsonPath("$.ownerName", is("María García")))
                    .andExpect(jsonPath("$.ownerEmail", is("maria.garcia@mail.com")));

            verify(employeeService).getVehicleById(1L);
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("404 - vehículo no encontrado")
        void shouldReturn404WhenVehicleNotFound() throws Exception {
            // Arrange
            when(employeeService.getVehicleById(999L))
                    .thenThrow(new VehicleNotFoundException("Vehículo no encontrado"));

            // Act & Assert
            mockMvc.perform(get("/api/v1/admin/vehicles/999"))
                    .andExpect(status().isNotFound());

            verify(employeeService).getVehicleById(999L);
        }
    }

    // ---------------------------------------------------------------
    // PATCH /api/v1/admin/vehicles/{vehicleId}/transfer-ownership
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("PATCH /api/v1/admin/vehicles/{vehicleId}/transfer-ownership")
    class TransferOwnership {

        private TransferVehicleOwnershipRequestDTO buildValidRequest(Long newOwnerId) {
            return new TransferVehicleOwnershipRequestDTO(newOwnerId);
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("200 - propiedad transferida exitosamente")
        void shouldTransferOwnershipSuccessfully() throws Exception {
            // Arrange
            TransferVehicleOwnershipRequestDTO req = buildValidRequest(20L);
            VehicleResponseDTO response = buildVehicleResponseWithOwner(
            );
            when(employeeService.transferVehicleOwnership(eq(1L), any(TransferVehicleOwnershipRequestDTO.class)))
                    .thenReturn(response);

            // Act & Assert
            mockMvc.perform(patch("/api/v1/admin/vehicles/1/transfer-ownership")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.ownerId", is(20)))
                    .andExpect(jsonPath("$.ownerName", is("Carlos López")))
                    .andExpect(jsonPath("$.ownerEmail", is("carlos.lopez@mail.com")));

            verify(employeeService).transferVehicleOwnership(eq(1L), any(TransferVehicleOwnershipRequestDTO.class));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("400 - body vacío sin newOwnerId")
        void shouldReturn400WhenBodyEmpty() throws Exception {
            mockMvc.perform(patch("/api/v1/admin/vehicles/1/transfer-ownership")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(employeeService);
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("400 - newOwnerId nulo")
        void shouldReturn400WhenNewOwnerIdNull() throws Exception {
            // Arrange
            TransferVehicleOwnershipRequestDTO req = new TransferVehicleOwnershipRequestDTO(null);

            // Act & Assert
            mockMvc.perform(patch("/api/v1/admin/vehicles/1/transfer-ownership")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(employeeService);
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("404 - vehículo no encontrado al transferir")
        void shouldReturn404WhenVehicleNotFound() throws Exception {
            // Arrange
            TransferVehicleOwnershipRequestDTO req = buildValidRequest(20L);
            when(employeeService.transferVehicleOwnership(eq(999L), any()))
                    .thenThrow(new VehicleNotFoundException("Vehículo no encontrado"));

            // Act & Assert
            mockMvc.perform(patch("/api/v1/admin/vehicles/999/transfer-ownership")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isNotFound());

            verify(employeeService).transferVehicleOwnership(eq(999L), any());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("404 - nuevo propietario (usuario) no encontrado")
        void shouldReturn404WhenNewOwnerNotFound() throws Exception {
            // Arrange
            TransferVehicleOwnershipRequestDTO req = buildValidRequest(999L);
            when(employeeService.transferVehicleOwnership(eq(1L), any()))
                    .thenThrow(new UserNotFoundException("Usuario no encontrado"));

            // Act & Assert
            mockMvc.perform(patch("/api/v1/admin/vehicles/1/transfer-ownership")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("409 - nuevo propietario ya tiene registrada la misma placa")
        void shouldReturn409WhenNewOwnerAlreadyHasPlate() throws Exception {
            // Arrange
            TransferVehicleOwnershipRequestDTO req = buildValidRequest(20L);
            when(employeeService.transferVehicleOwnership(eq(1L), any()))
                    .thenThrow(new VehicleAlreadyOwnedException(
                            "El nuevo dueño ya tiene registrada esa placa"
                    ));

            // Act & Assert
            mockMvc.perform(patch("/api/v1/admin/vehicles/1/transfer-ownership")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isConflict());
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