package com.sparktech.motorx.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sparktech.motorx.Services.IVehicleService;
import com.sparktech.motorx.controller.error.GlobalControllerAdvice;
import com.sparktech.motorx.dto.vehicle.CreateVehicleRequestDTO;
import com.sparktech.motorx.dto.vehicle.UpdateVehicleRequestDTO;
import com.sparktech.motorx.dto.vehicle.VehicleResponseDTO;

import com.sparktech.motorx.exception.ChasisAlreadyRegisteredException;
import com.sparktech.motorx.exception.VehicleAlreadyOwnedException;
import com.sparktech.motorx.exception.VehicleDoesntBelongToUserException;
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

@WebMvcTest(controllers = VehicleController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalControllerAdvice.class, VehicleControllerTest.TestConfig.class})
@DisplayName("VehicleController - Tests")
class VehicleControllerTest {

    @Resource
    private MockMvc mockMvc;

    @Resource
    private IVehicleService vehicleService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        reset(vehicleService);
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    // ---------------------------------------------------------------
    // Fixtures
    // ---------------------------------------------------------------

    private VehicleResponseDTO buildVehicleResponse(Long id) {
        return new VehicleResponseDTO(
                id,
                "Honda",
                "CB500F",
                2022,
                "ABC12A",
                500,
                "CHASSIS-001",
                10L,
                "María García",
                "maria@mail.com",
                LocalDateTime.of(2025, 1, 10, 8, 0),
                LocalDateTime.of(2025, 1, 10, 8, 0)
        );
    }

    private CreateVehicleRequestDTO buildValidCreateRequest() {
        return new CreateVehicleRequestDTO(
                "Honda",
                "CB500F",
                2022,
                "ABC12A",
                500,
                "CHASSIS-001"
        );
    }

    private UpdateVehicleRequestDTO buildValidUpdateRequest() {
        return new UpdateVehicleRequestDTO("Yamaha", "MT-07", 700);
    }

    private String json(Object o) throws Exception {
        return objectMapper.writeValueAsString(o);
    }

    // ---------------------------------------------------------------
    // POST /api/v1/user/vehicles
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("POST /api/v1/user/vehicles")
    class AddVehicle {

        @Test
        @WithMockUser
        @DisplayName("201 - vehículo registrado exitosamente")
        void shouldAddVehicleSuccessfully() throws Exception {
            // Arrange
            VehicleResponseDTO response = buildVehicleResponse(1L);
            when(vehicleService.addVehicle(any(CreateVehicleRequestDTO.class))).thenReturn(response);

            // Act & Assert
            mockMvc.perform(post("/api/v1/user/vehicles")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(buildValidCreateRequest())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.brand", is("Honda")))
                    .andExpect(jsonPath("$.model", is("CB500F")))
                    .andExpect(jsonPath("$.licensePlate", is("ABC12A")))
                    .andExpect(jsonPath("$.cylinderCapacity", is(500)))
                    .andExpect(jsonPath("$.chassisNumber", is("CHASSIS-001")));

            verify(vehicleService).addVehicle(any(CreateVehicleRequestDTO.class));
        }

        @Test
        @WithMockUser
        @DisplayName("400 - body vacío falla todas las validaciones")
        void shouldReturn400WhenBodyEmpty() throws Exception {
            mockMvc.perform(post("/api/v1/user/vehicles")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(vehicleService);
        }

        @Test
        @WithMockUser
        @DisplayName("400 - @NotBlank falla con brand nulo")
        void shouldReturn400WhenBrandNull() throws Exception {
            // Arrange
            CreateVehicleRequestDTO req = new CreateVehicleRequestDTO(
                    null, "CB500F", 2022, "ABC12A", 500, "CHASSIS-001"
            );

            // Act & Assert
            mockMvc.perform(post("/api/v1/user/vehicles")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(vehicleService);
        }

        @Test
        @WithMockUser
        @DisplayName("400 - @NotBlank falla con model nulo")
        void shouldReturn400WhenModelNull() throws Exception {
            // Arrange
            CreateVehicleRequestDTO req = new CreateVehicleRequestDTO(
                    "Honda", null, 2022, "ABC12A", 500, "CHASSIS-001"
            );

            // Act & Assert
            mockMvc.perform(post("/api/v1/user/vehicles")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(vehicleService);
        }

        @Test
        @WithMockUser
        @DisplayName("400 - @NotNull falla con yearOfManufacture nulo")
        void shouldReturn400WhenYearNull() throws Exception {
            // Arrange
            CreateVehicleRequestDTO req = new CreateVehicleRequestDTO(
                    "Honda", "CB500F", null, "ABC123", 500, "CHASSIS-001"
            );

            // Act & Assert
            mockMvc.perform(post("/api/v1/user/vehicles")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(vehicleService);
        }

        @Test
        @WithMockUser
        @DisplayName("400 - @Min(1950) falla con año anterior a 1950")
        void shouldReturn400WhenYearBefore1950() throws Exception {
            // Arrange — 1949 no pasa @Min(1950)
            CreateVehicleRequestDTO req = new CreateVehicleRequestDTO(
                    "Honda", "CB500F", 1949, "ABC12A", 500, "CHASSIS-001"
            );

            // Act & Assert
            mockMvc.perform(post("/api/v1/user/vehicles")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(vehicleService);
        }

        @Test
        @WithMockUser
        @DisplayName("400 - @Max(2026) falla con año posterior a 2026")
        void shouldReturn400WhenYearAfter2026() throws Exception {
            // Arrange — 2027 no pasa @Max(2026)
            CreateVehicleRequestDTO req = new CreateVehicleRequestDTO(
                    "Honda", "CB500F", 2027, "ABC12A", 500, "CHASSIS-001"
            );

            // Act & Assert
            mockMvc.perform(post("/api/v1/user/vehicles")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(vehicleService);
        }

        @Test
        @WithMockUser
        @DisplayName("201 - año límite inferior 1950 es válido")
        void shouldAcceptYear1950() throws Exception {
            // Arrange — 1950 SÍ pasa @Min(1950)
            CreateVehicleRequestDTO req = new CreateVehicleRequestDTO(
                    "Honda", "CB500F", 1950, "ABC12A", 500, "CHASSIS-001"
            );
            when(vehicleService.addVehicle(any())).thenReturn(buildVehicleResponse(1L));

            // Act & Assert
            mockMvc.perform(post("/api/v1/user/vehicles")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isCreated());
        }

        @Test
        @WithMockUser
        @DisplayName("201 - año límite superior 2026 es válido")
        void shouldAcceptYear2026() throws Exception {
            // Arrange — 2026 SÍ pasa @Max(2026)
            CreateVehicleRequestDTO req = new CreateVehicleRequestDTO(
                    "Honda", "CB500F", 2026, "ABC12A", 500, "CHASSIS-001"
            );
            when(vehicleService.addVehicle(any())).thenReturn(buildVehicleResponse(1L));

            // Act & Assert
            mockMvc.perform(post("/api/v1/user/vehicles")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isCreated());
        }

        @Test
        @WithMockUser
        @DisplayName("400 - @NotBlank falla con licensePlate nula")
        void shouldReturn400WhenLicensePlateNull() throws Exception {
            // Arrange
            CreateVehicleRequestDTO req = new CreateVehicleRequestDTO(
                    "Honda", "CB500F", 2022, null, 500, "CHASSIS-001"
            );

            // Act & Assert
            mockMvc.perform(post("/api/v1/user/vehicles")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(vehicleService);
        }

        @Test
        @WithMockUser
        @DisplayName("400 - @Pattern falla con placa en formato incorrecto (minúsculas)")
        void shouldReturn400WhenLicensePlateLowercase() throws Exception {
            // Arrange — "abc123" no pasa @Pattern (requiere mayúsculas)
            CreateVehicleRequestDTO req = new CreateVehicleRequestDTO(
                    "Honda", "CB500F", 2022, "abc12a", 500, "CHASSIS-001"
            );

            // Act & Assert
            mockMvc.perform(post("/api/v1/user/vehicles")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(vehicleService);
        }

        @Test
        @WithMockUser
        @DisplayName("400 - @Pattern falla con placa sin letra final (solo dígitos al final)")
        void shouldReturn400WhenLicensePlateFormatWrong() throws Exception {
            // Arrange — "ABC1234" no pasa el patrón ^[A-Z]{3}\\d{2}[A-Z] (necesita letra al final)
            CreateVehicleRequestDTO req = new CreateVehicleRequestDTO(
                    "Honda", "CB500F", 2022, "ABC123A", 500, "CHASSIS-001"
            );

            // Act & Assert
            mockMvc.perform(post("/api/v1/user/vehicles")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(vehicleService);
        }

        @Test
        @WithMockUser
        @DisplayName("400 - @NotNull falla con cylinderCapacity nulo")
        void shouldReturn400WhenCylinderCapacityNull() throws Exception {
            // Arrange
            CreateVehicleRequestDTO req = new CreateVehicleRequestDTO(
                    "Honda", "CB500F", 2022, "ABC12A", null, "CHASSIS-001"
            );

            // Act & Assert
            mockMvc.perform(post("/api/v1/user/vehicles")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(vehicleService);
        }

        @Test
        @WithMockUser
        @DisplayName("400 - @Min(50) falla con cilindraje menor a 50 cc")
        void shouldReturn400WhenCylinderCapacityTooLow() throws Exception {
            // Arrange — 49 no pasa @Min(50)
            CreateVehicleRequestDTO req = new CreateVehicleRequestDTO(
                    "Honda", "CB500F", 2022, "ABC12A", 49, "CHASSIS-001"
            );

            // Act & Assert
            mockMvc.perform(post("/api/v1/user/vehicles")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(vehicleService);
        }

        @Test
        @WithMockUser
        @DisplayName("400 - @Max(9999) falla con cilindraje mayor a 9999 cc")
        void shouldReturn400WhenCylinderCapacityTooHigh() throws Exception {
            // Arrange — 10000 no pasa @Max(9999)
            CreateVehicleRequestDTO req = new CreateVehicleRequestDTO(
                    "Honda", "CB500F", 2022, "ABC12A", 10000, "CHASSIS-001"
            );

            // Act & Assert
            mockMvc.perform(post("/api/v1/user/vehicles")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(vehicleService);
        }

        @Test
        @WithMockUser
        @DisplayName("201 - cilindraje límite inferior 50 cc es válido")
        void shouldAcceptCylinderCapacity50() throws Exception {
            // Arrange — 50 SÍ pasa @Min(50)
            CreateVehicleRequestDTO req = new CreateVehicleRequestDTO(
                    "Honda", "CB500F", 2022, "ABC12A", 50, "CHASSIS-001"
            );
            when(vehicleService.addVehicle(any())).thenReturn(buildVehicleResponse(1L));

            // Act & Assert
            mockMvc.perform(post("/api/v1/user/vehicles")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isCreated());
        }

        @Test
        @WithMockUser
        @DisplayName("201 - cilindraje límite superior 9999 cc es válido")
        void shouldAcceptCylinderCapacity9999() throws Exception {
            // Arrange — 9999 SÍ pasa @Max(9999)
            CreateVehicleRequestDTO req = new CreateVehicleRequestDTO(
                    "Honda", "CB500F", 2022, "ABC12A", 9999, "CHASSIS-001"
            );
            when(vehicleService.addVehicle(any())).thenReturn(buildVehicleResponse(1L));

            // Act & Assert
            mockMvc.perform(post("/api/v1/user/vehicles")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isCreated());
        }

        @Test
        @WithMockUser
        @DisplayName("400 - @NotBlank falla con chassisNumber nulo")
        void shouldReturn400WhenChassisNumberNull() throws Exception {
            // Arrange
            CreateVehicleRequestDTO req = new CreateVehicleRequestDTO(
                    "Honda", "CB500F", 2022, "ABC12A", 500, null
            );

            // Act & Assert
            mockMvc.perform(post("/api/v1/user/vehicles")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(vehicleService);
        }

        @Test
        @WithMockUser
        @DisplayName("409 - placa ya registrada en el sistema")
        void shouldReturn409WhenLicensePlateAlreadyExists() throws Exception {
            // Arrange
            when(vehicleService.addVehicle(any()))
                    .thenThrow(new VehicleAlreadyOwnedException("La placa ya está registrada en el sistema"));

            // Act & Assert
            mockMvc.perform(post("/api/v1/user/vehicles")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(buildValidCreateRequest())))
                    .andExpect(status().isConflict());
        }

        @Test
        @WithMockUser
        @DisplayName("409 - número de chasis ya registrado en el sistema")
        void shouldReturn409WhenChassisNumberAlreadyExists() throws Exception {
            // Arrange
            when(vehicleService.addVehicle(any()))
                    .thenThrow(new ChasisAlreadyRegisteredException("El número de chasis ya está registrado"));

            // Act & Assert
            mockMvc.perform(post("/api/v1/user/vehicles")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(buildValidCreateRequest())))
                    .andExpect(status().isConflict());
        }
    }

    // ---------------------------------------------------------------
    // GET /api/v1/user/vehicles
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("GET /api/v1/user/vehicles")
    class GetMyVehicles {

        @Test
        @WithMockUser
        @DisplayName("200 - retorna lista de vehículos del cliente")
        void shouldReturnMyVehicles() throws Exception {
            // Arrange
            List<VehicleResponseDTO> vehicles = List.of(
                    buildVehicleResponse(1L),
                    buildVehicleResponse(2L)
            );
            when(vehicleService.getMyVehicles()).thenReturn(vehicles);

            // Act & Assert
            mockMvc.perform(get("/api/v1/user/vehicles"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].id", is(1)))
                    .andExpect(jsonPath("$[1].id", is(2)));

            verify(vehicleService).getMyVehicles();
        }

        @Test
        @WithMockUser
        @DisplayName("200 - retorna lista vacía cuando el cliente no tiene vehículos")
        void shouldReturnEmptyListWhenNoVehicles() throws Exception {
            // Arrange
            when(vehicleService.getMyVehicles()).thenReturn(List.of());

            // Act & Assert
            mockMvc.perform(get("/api/v1/user/vehicles"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));

            verify(vehicleService).getMyVehicles();
        }
    }

    // ---------------------------------------------------------------
    // GET /api/v1/user/vehicles/{vehicleId}
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("GET /api/v1/user/vehicles/{vehicleId}")
    class GetMyVehicleById {

        @Test
        @WithMockUser
        @DisplayName("200 - retorna detalle completo del vehículo")
        void shouldReturnVehicleDetail() throws Exception {
            // Arrange
            VehicleResponseDTO response = buildVehicleResponse(5L);
            when(vehicleService.getMyVehicleById(5L)).thenReturn(response);

            // Act & Assert
            mockMvc.perform(get("/api/v1/user/vehicles/5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(5)))
                    .andExpect(jsonPath("$.brand", is("Honda")))
                    .andExpect(jsonPath("$.model", is("CB500F")))
                    .andExpect(jsonPath("$.yearOfManufacture", is(2022)))
                    .andExpect(jsonPath("$.licensePlate", is("ABC12A")))
                    .andExpect(jsonPath("$.cylinderCapacity", is(500)))
                    .andExpect(jsonPath("$.chassisNumber", is("CHASSIS-001")));

            verify(vehicleService).getMyVehicleById(5L);
        }

        @Test
        @WithMockUser
        @DisplayName("404 - vehículo no encontrado")
        void shouldReturn404WhenVehicleNotFound() throws Exception {
            // Arrange
            when(vehicleService.getMyVehicleById(999L))
                    .thenThrow(new VehicleNotFoundException("Vehículo no encontrado"));

            // Act & Assert
            mockMvc.perform(get("/api/v1/user/vehicles/999"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser
        @DisplayName("403 - el vehículo no pertenece al usuario autenticado")
        void shouldReturn403WhenVehicleNotOwnedByUser() throws Exception {
            // Arrange
            when(vehicleService.getMyVehicleById(3L))
                    .thenThrow(new VehicleDoesntBelongToUserException("El vehículo no pertenece al usuario"));

            // Act & Assert
            mockMvc.perform(get("/api/v1/user/vehicles/3"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/user/vehicles/{vehicleId}")
    class UpdateMyVehicle {

        @Test
        @WithMockUser
        @DisplayName("200 - vehículo actualizado exitosamente")
        void shouldUpdateVehicleSuccessfully() throws Exception {
            // Arrange
            UpdateVehicleRequestDTO req = buildValidUpdateRequest();
            VehicleResponseDTO response = new VehicleResponseDTO(
                    1L, "Yamaha", "MT-07", 2022, "ABC12A", 700,
                    "CHASSIS-001", 10L, "María García", "maria@mail.com",
                    LocalDateTime.of(2025, 1, 10, 8, 0),
                    LocalDateTime.of(2025, 6, 1, 9, 0)
            );
            when(vehicleService.updateMyVehicle(eq(1L), any(UpdateVehicleRequestDTO.class)))
                    .thenReturn(response);

            // Act & Assert
            mockMvc.perform(put("/api/v1/user/vehicles/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.brand", is("Yamaha")))
                    .andExpect(jsonPath("$.model", is("MT-07")))
                    .andExpect(jsonPath("$.cylinderCapacity", is(700)));

            verify(vehicleService).updateMyVehicle(eq(1L), any(UpdateVehicleRequestDTO.class));
        }

        @Test
        @WithMockUser
        @DisplayName("400 - body vacío falla todas las validaciones")
        void shouldReturn400WhenBodyEmpty() throws Exception {
            mockMvc.perform(put("/api/v1/user/vehicles/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(vehicleService);
        }

        @Test
        @WithMockUser
        @DisplayName("400 - @NotBlank falla con brand nulo")
        void shouldReturn400WhenBrandNull() throws Exception {
            // Arrange
            UpdateVehicleRequestDTO req = new UpdateVehicleRequestDTO(null, "MT-07", 700);

            // Act & Assert
            mockMvc.perform(put("/api/v1/user/vehicles/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(vehicleService);
        }

        @Test
        @WithMockUser
        @DisplayName("400 - @NotBlank falla con model nulo")
        void shouldReturn400WhenModelNull() throws Exception {
            // Arrange
            UpdateVehicleRequestDTO req = new UpdateVehicleRequestDTO("Yamaha", null, 700);

            // Act & Assert
            mockMvc.perform(put("/api/v1/user/vehicles/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(vehicleService);
        }

        @Test
        @WithMockUser
        @DisplayName("400 - @NotNull falla con cylinderCapacity nulo")
        void shouldReturn400WhenCylinderCapacityNull() throws Exception {
            // Arrange
            UpdateVehicleRequestDTO req = new UpdateVehicleRequestDTO("Yamaha", "MT-07", null);

            // Act & Assert
            mockMvc.perform(put("/api/v1/user/vehicles/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(vehicleService);
        }

        @Test
        @WithMockUser
        @DisplayName("400 - @Min(50) falla con cilindraje menor a 50 cc")
        void shouldReturn400WhenCylinderCapacityTooLow() throws Exception {
            // Arrange — 49 no pasa @Min(50)
            UpdateVehicleRequestDTO req = new UpdateVehicleRequestDTO("Yamaha", "MT-07", 49);

            // Act & Assert
            mockMvc.perform(put("/api/v1/user/vehicles/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(vehicleService);
        }

        @Test
        @WithMockUser
        @DisplayName("400 - @Max(9999) falla con cilindraje mayor a 9999 cc")
        void shouldReturn400WhenCylinderCapacityTooHigh() throws Exception {
            // Arrange — 10000 no pasa @Max(9999)
            UpdateVehicleRequestDTO req = new UpdateVehicleRequestDTO("Yamaha", "MT-07", 10000);

            // Act & Assert
            mockMvc.perform(put("/api/v1/user/vehicles/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(vehicleService);
        }

        @Test
        @WithMockUser
        @DisplayName("404 - vehículo no encontrado")
        void shouldReturn404WhenVehicleNotFound() throws Exception {
            // Arrange
            when(vehicleService.updateMyVehicle(eq(999L), any()))
                    .thenThrow(new VehicleNotFoundException("Vehículo no encontrado"));

            // Act & Assert
            mockMvc.perform(put("/api/v1/user/vehicles/999")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(buildValidUpdateRequest())))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser
        @DisplayName("403 - el vehículo no pertenece al usuario autenticado")
        void shouldReturn403WhenVehicleNotOwnedByUser() throws Exception {
            // Arrange
            when(vehicleService.updateMyVehicle(eq(3L), any()))
                    .thenThrow(new VehicleDoesntBelongToUserException("El vehículo no pertenece al usuario"));

            // Act & Assert
            mockMvc.perform(put("/api/v1/user/vehicles/3")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(buildValidUpdateRequest())))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/user/vehicles/{vehicleId}")
    class DeleteMyVehicle {

        @Test
        @WithMockUser
        @DisplayName("204 - vehículo eliminado exitosamente")
        void shouldDeleteVehicleSuccessfully() throws Exception {
            // Arrange
            doNothing().when(vehicleService).deleteMyVehicle(1L);

            // Act & Assert
            mockMvc.perform(delete("/api/v1/user/vehicles/1"))
                    .andExpect(status().isNoContent());

            verify(vehicleService).deleteMyVehicle(1L);
        }

        @Test
        @WithMockUser
        @DisplayName("404 - vehículo no encontrado")
        void shouldReturn404WhenVehicleNotFound() throws Exception {
            // Arrange
            doThrow(new VehicleNotFoundException("Vehículo no encontrado"))
                    .when(vehicleService).deleteMyVehicle(999L);

            // Act & Assert
            mockMvc.perform(delete("/api/v1/user/vehicles/999"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser
        @DisplayName("403 - el vehículo no pertenece al usuario autenticado")
        void shouldReturn403WhenVehicleNotOwnedByUser() throws Exception {
            // Arrange
            doThrow(new VehicleDoesntBelongToUserException("El vehículo no pertenece al usuario"))
                    .when(vehicleService).deleteMyVehicle(3L);

            // Act & Assert
            mockMvc.perform(delete("/api/v1/user/vehicles/3"))
                    .andExpect(status().isForbidden());
        }
    }

    // ---------------------------------------------------------------
    // TestConfiguration
    // ---------------------------------------------------------------

    @TestConfiguration
    static class TestConfig {

        @Bean
        @Primary
        IVehicleService vehicleService() {
            return mock(IVehicleService.class);
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