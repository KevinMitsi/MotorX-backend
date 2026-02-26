package com.sparktech.motorx.Services.impl;

import com.sparktech.motorx.Services.ICurrentUserService;
import com.sparktech.motorx.dto.vehicle.CreateVehicleRequestDTO;
import com.sparktech.motorx.dto.vehicle.UpdateVehicleRequestDTO;
import com.sparktech.motorx.dto.vehicle.VehicleResponseDTO;
import com.sparktech.motorx.entity.UserEntity;
import com.sparktech.motorx.entity.VehicleEntity;
import com.sparktech.motorx.exception.VehicleAlreadyOwnedException;
import com.sparktech.motorx.exception.VehicleNotFoundException;
import com.sparktech.motorx.mapper.VehicleMapper;
import com.sparktech.motorx.repository.JpaVehicleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("VehicleServiceImpl - Unit Tests")
class VehicleServiceImplTest {

    // ================================================================
    // MOCKS
    // ================================================================
    @Mock private JpaVehicleRepository vehicleRepository;
    @Mock private VehicleMapper vehicleMapper;
    @Mock private ICurrentUserService currentUserService;

    @InjectMocks
    private VehicleServiceImpl sut;

    @Captor private ArgumentCaptor<VehicleEntity> vehicleCaptor;

    // ================================================================
    // BUILDERS
    // ================================================================

    private UserEntity buildUser(Long id) {
        UserEntity u = new UserEntity();
        u.setId(id);
        u.setName("Usuario " + id);
        u.setEmail("user" + id + "@test.com");
        return u;
    }

    private VehicleEntity buildVehicle(Long id, String plate, UserEntity owner) {
        VehicleEntity v = new VehicleEntity();
        v.setId(id);
        v.setLicensePlate(plate);
        v.setBrand("HONDA");
        v.setModel("CB 190");
        v.setYearOfManufacture(2022);
        v.setCylinderCapacity(190);
        v.setChassisNumber("CHASSIS-" + id);
        v.setOwner(owner);
        return v;
    }

    private CreateVehicleRequestDTO buildCreateRequest(String plate, String chassis) {
        return new CreateVehicleRequestDTO(
                "HONDA", "CB 190", 2022, plate, 190, chassis
        );
    }

    // ================================================================
    // addVehicle()
    // ================================================================

    @Nested
    @DisplayName("addVehicle()")
    class AddVehicleTests {

        @Test
        @DisplayName("Camino feliz: persiste el vehículo con la placa en mayúsculas y owner correcto")
        void givenValidRequest_thenPersistAndReturnDTO() {
            // Arrange
            UserEntity user = buildUser(1L);
            VehicleEntity saved = buildVehicle(10L, "ABC123", user);
            VehicleResponseDTO expected = mock(VehicleResponseDTO.class);

            when(currentUserService.getAuthenticatedUser()).thenReturn(user);
            when(vehicleRepository.existsByLicensePlate("ABC123")).thenReturn(false);
            when(vehicleRepository.existsByChassisNumber("CH-001")).thenReturn(false);
            when(vehicleRepository.save(any())).thenReturn(saved);
            when(vehicleMapper.toResponseDTO(saved)).thenReturn(expected);

            // Act
            VehicleResponseDTO result = sut.addVehicle(buildCreateRequest("abc123", "CH-001"));

            // Assert
            assertThat(result).isEqualTo(expected);
            verify(vehicleRepository).save(vehicleCaptor.capture());
            VehicleEntity persisted = vehicleCaptor.getValue();
            assertThat(persisted.getLicensePlate()).isEqualTo("ABC123"); // uppercase
            assertThat(persisted.getOwner()).isEqualTo(user);
        }

        @Test
        @DisplayName("La placa se normaliza a mayúsculas y sin espacios antes de persistir")
        void givenLowerCasePlateWithSpaces_thenNormalizeBeforePersist() {
            // Arrange
            UserEntity user = buildUser(1L);
            VehicleEntity saved = buildVehicle(10L, "ABC123", user);

            when(currentUserService.getAuthenticatedUser()).thenReturn(user);
            when(vehicleRepository.existsByLicensePlate("ABC123")).thenReturn(false);
            when(vehicleRepository.existsByChassisNumber(any())).thenReturn(false);
            when(vehicleRepository.save(any())).thenReturn(saved);
            when(vehicleMapper.toResponseDTO(any())).thenReturn(mock(VehicleResponseDTO.class));

            // Act
            sut.addVehicle(buildCreateRequest("  abc123  ", "CH-001"));

            // Assert
            verify(vehicleRepository).save(vehicleCaptor.capture());
            assertThat(vehicleCaptor.getValue().getLicensePlate()).isEqualTo("ABC123");
        }

        @Test
        @DisplayName("Lanza IllegalArgumentException si el usuario ya tiene esa placa registrada")
        void givenPlateAlreadyOwnedBySameUser_thenThrowIllegalArgument() {
            // Arrange
            UserEntity user = buildUser(1L);
            VehicleEntity existing = buildVehicle(5L, "ABC123", user); // mismo owner

            when(currentUserService.getAuthenticatedUser()).thenReturn(user);
            when(vehicleRepository.existsByLicensePlate("ABC123")).thenReturn(true);
            when(vehicleRepository.findByLicensePlate("ABC123"))
                    .thenReturn(Optional.of(existing));
            CreateVehicleRequestDTO createVehicleRequestDTO = buildCreateRequest("ABC123", "CH-001");
            // Act + Assert
            assertThatThrownBy(() -> sut.addVehicle(createVehicleRequestDTO))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("ABC123");

            verify(vehicleRepository, never()).save(any());
        }

        @Test
        @DisplayName("Lanza VehicleAlreadyOwnedException si la placa pertenece a otro usuario")
        void givenPlateOwnedByAnotherUser_thenThrowVehicleAlreadyOwned() {
            // Arrange
            UserEntity currentUser = buildUser(1L);
            UserEntity otherUser   = buildUser(2L);
            VehicleEntity existing = buildVehicle(5L, "ABC123", otherUser); // otro owner

            when(currentUserService.getAuthenticatedUser()).thenReturn(currentUser);
            when(vehicleRepository.existsByLicensePlate("ABC123")).thenReturn(true);
            when(vehicleRepository.findByLicensePlate("ABC123"))
                    .thenReturn(Optional.of(existing));
            CreateVehicleRequestDTO createVehicleRequestDTO = buildCreateRequest("ABC123", "CH-001");
            // Act + Assert
            assertThatThrownBy(() -> sut.addVehicle(createVehicleRequestDTO))
                    .isInstanceOf(VehicleAlreadyOwnedException.class);

            verify(vehicleRepository, never()).save(any());
        }

        @Test
        @DisplayName("Lanza IllegalArgumentException si el número de chasis ya existe")
        void givenDuplicateChassisNumber_thenThrow() {
            // Arrange
            UserEntity user = buildUser(1L);
            when(currentUserService.getAuthenticatedUser()).thenReturn(user);
            when(vehicleRepository.existsByLicensePlate("ABC123")).thenReturn(false);
            when(vehicleRepository.existsByChassisNumber("DUPLICATE-CHASSIS")).thenReturn(true);

            CreateVehicleRequestDTO createVehicleRequestDTO = buildCreateRequest("ABC123", "DUPLICATE-CHASSIS");
            // Act + Assert
            assertThatThrownBy(() ->
                    sut.addVehicle(createVehicleRequestDTO))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("DUPLICATE-CHASSIS");

            verify(vehicleRepository, never()).save(any());
        }

        @Test
        @DisplayName("La placa duplicada se busca ya en mayúsculas (consistencia del check)")
        void givenLowerCasePlate_thenExistenceCheckUsesUpperCase() {
            // Arrange
            UserEntity user = buildUser(1L);
            when(currentUserService.getAuthenticatedUser()).thenReturn(user);
            when(vehicleRepository.existsByLicensePlate("ABC123")).thenReturn(false);
            when(vehicleRepository.existsByChassisNumber(any())).thenReturn(false);
            when(vehicleRepository.save(any())).thenReturn(buildVehicle(1L, "ABC123", user));
            when(vehicleMapper.toResponseDTO(any())).thenReturn(mock(VehicleResponseDTO.class));

            // Act
            sut.addVehicle(buildCreateRequest("abc123", "CH-001"));

            // Assert — existsByLicensePlate debe recibir la placa ya en mayúsculas
            verify(vehicleRepository).existsByLicensePlate("ABC123");
        }
    }

    // ================================================================
    // getMyVehicles()
    // ================================================================

    @Nested
    @DisplayName("getMyVehicles()")
    class GetMyVehiclesTests {

        @Test
        @DisplayName("Retorna lista mapeada del usuario autenticado")
        void givenVehiclesExist_thenReturnMappedList() {
            // Arrange
            UserEntity user = buildUser(1L);
            VehicleEntity v1 = buildVehicle(1L, "AAA1AX", user);
            VehicleEntity v2 = buildVehicle(2L, "BBB2BX", user);
            VehicleResponseDTO dto1 = mock(VehicleResponseDTO.class);
            VehicleResponseDTO dto2 = mock(VehicleResponseDTO.class);

            when(currentUserService.getAuthenticatedUser()).thenReturn(user);
            when(vehicleRepository.findByOwnerIdOrderByCreatedAtDesc(1L))
                    .thenReturn(List.of(v1, v2));
            when(vehicleMapper.toResponseDTO(v1)).thenReturn(dto1);
            when(vehicleMapper.toResponseDTO(v2)).thenReturn(dto2);

            // Act
            List<VehicleResponseDTO> result = sut.getMyVehicles();

            // Assert
            assertThat(result).containsExactly(dto1, dto2);
        }

        @Test
        @DisplayName("Retorna lista vacía si el usuario no tiene vehículos")
        void givenNoVehicles_thenReturnEmptyList() {
            // Arrange
            UserEntity user = buildUser(1L);
            when(currentUserService.getAuthenticatedUser()).thenReturn(user);
            when(vehicleRepository.findByOwnerIdOrderByCreatedAtDesc(1L))
                    .thenReturn(List.of());

            // Act + Assert
            assertThat(sut.getMyVehicles()).isEmpty();
        }
    }

    // ================================================================
    // getMyVehicleById()
    // ================================================================

    @Nested
    @DisplayName("getMyVehicleById()")
    class GetMyVehicleByIdTests {

        @Test
        @DisplayName("Retorna DTO cuando el vehículo pertenece al usuario autenticado")
        void givenOwnVehicle_thenReturnDTO() {
            // Arrange
            UserEntity user = buildUser(1L);
            VehicleEntity vehicle = buildVehicle(10L, "ABC123", user);
            VehicleResponseDTO expected = mock(VehicleResponseDTO.class);

            when(currentUserService.getAuthenticatedUser()).thenReturn(user);
            when(vehicleRepository.findById(10L)).thenReturn(Optional.of(vehicle));
            when(vehicleMapper.toResponseDTO(vehicle)).thenReturn(expected);

            // Act
            VehicleResponseDTO result = sut.getMyVehicleById(10L);

            // Assert
            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("Lanza VehicleNotFoundException si el vehículo no existe")
        void givenNonExistentVehicle_thenThrow() {
            // Arrange
            when(currentUserService.getAuthenticatedUser()).thenReturn(buildUser(1L));
            when(vehicleRepository.findById(99L)).thenReturn(Optional.empty());

            // Act + Assert
            assertThatThrownBy(() -> sut.getMyVehicleById(99L))
                    .isInstanceOf(VehicleNotFoundException.class);
        }

        @Test
        @DisplayName("Lanza SecurityException si el vehículo pertenece a otro usuario")
        void givenVehicleOfAnotherUser_thenThrowSecurityException() {
            // Arrange
            UserEntity currentUser = buildUser(1L);
            UserEntity otherUser   = buildUser(2L);
            VehicleEntity vehicle  = buildVehicle(10L, "ABC123", otherUser);

            when(currentUserService.getAuthenticatedUser()).thenReturn(currentUser);
            when(vehicleRepository.findById(10L)).thenReturn(Optional.of(vehicle));

            // Act + Assert
            assertThatThrownBy(() -> sut.getMyVehicleById(10L))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("10");
        }
    }

    // ================================================================
    // updateMyVehicle()
    // ================================================================

    @Nested
    @DisplayName("updateMyVehicle()")
    class UpdateMyVehicleTests {

        private UpdateVehicleRequestDTO buildUpdateRequest() {
            return new UpdateVehicleRequestDTO("YAMAHA", "FZ 25", 250);
        }

        @Test
        @DisplayName("Actualiza marca, modelo y cilindraje; placa y chasis son inmutables")
        void givenValidRequest_thenUpdateMutableFieldsOnly() {
            // Arrange
            UserEntity user    = buildUser(1L);
            VehicleEntity vehicle = buildVehicle(10L, "ABC123", user);
            String originalPlate   = vehicle.getLicensePlate();
            String originalChassis = vehicle.getChassisNumber();

            when(currentUserService.getAuthenticatedUser()).thenReturn(user);
            when(vehicleRepository.findById(10L)).thenReturn(Optional.of(vehicle));
            when(vehicleRepository.save(any())).thenReturn(vehicle);
            when(vehicleMapper.toResponseDTO(vehicle)).thenReturn(mock(VehicleResponseDTO.class));

            // Act
            sut.updateMyVehicle(10L, buildUpdateRequest());

            // Assert — campos mutables actualizados
            verify(vehicleRepository).save(vehicleCaptor.capture());
            VehicleEntity persisted = vehicleCaptor.getValue();
            assertThat(persisted.getBrand()).isEqualTo("YAMAHA");
            assertThat(persisted.getModel()).isEqualTo("FZ 25");
            assertThat(persisted.getCylinderCapacity()).isEqualTo(250);

            // Assert — campos inmutables sin cambiar
            assertThat(persisted.getLicensePlate()).isEqualTo(originalPlate);
            assertThat(persisted.getChassisNumber()).isEqualTo(originalChassis);
        }

        @Test
        @DisplayName("Marca y modelo se trimmean antes de persistir")
        void givenRequestWithSpaces_thenTrimBeforePersist() {
            // Arrange
            UserEntity user = buildUser(1L);
            VehicleEntity vehicle = buildVehicle(10L, "ABC123", user);
            UpdateVehicleRequestDTO request =
                    new UpdateVehicleRequestDTO("  YAMAHA  ", "  FZ 25  ", 250);

            when(currentUserService.getAuthenticatedUser()).thenReturn(user);
            when(vehicleRepository.findById(10L)).thenReturn(Optional.of(vehicle));
            when(vehicleRepository.save(any())).thenReturn(vehicle);
            when(vehicleMapper.toResponseDTO(any())).thenReturn(mock(VehicleResponseDTO.class));

            // Act
            sut.updateMyVehicle(10L, request);

            // Assert
            verify(vehicleRepository).save(vehicleCaptor.capture());
            assertThat(vehicleCaptor.getValue().getBrand()).isEqualTo("YAMAHA");
            assertThat(vehicleCaptor.getValue().getModel()).isEqualTo("FZ 25");
        }

        @Test
        @DisplayName("Lanza VehicleNotFoundException si el vehículo no existe")
        void givenNonExistentVehicle_thenThrow() {
            // Arrange
            when(currentUserService.getAuthenticatedUser()).thenReturn(buildUser(1L));
            when(vehicleRepository.findById(99L)).thenReturn(Optional.empty());
            UpdateVehicleRequestDTO updateVehicleRequestDTO = buildUpdateRequest();
            // Act + Assert
            assertThatThrownBy(() -> sut.updateMyVehicle(99L, updateVehicleRequestDTO))
                    .isInstanceOf(VehicleNotFoundException.class);

            verify(vehicleRepository, never()).save(any());
        }

        @Test
        @DisplayName("Lanza SecurityException si el vehículo pertenece a otro usuario")
        void givenVehicleOfAnotherUser_thenThrow() {
            // Arrange
            UserEntity currentUser = buildUser(1L);
            UserEntity otherUser   = buildUser(2L);
            VehicleEntity vehicle  = buildVehicle(10L, "ABC123", otherUser);

            when(currentUserService.getAuthenticatedUser()).thenReturn(currentUser);
            when(vehicleRepository.findById(10L)).thenReturn(Optional.of(vehicle));

            UpdateVehicleRequestDTO updateVehicleRequestDTO = buildUpdateRequest();
            // Act + Assert
            assertThatThrownBy(() -> sut.updateMyVehicle(10L, updateVehicleRequestDTO))
                    .isInstanceOf(SecurityException.class);

            verify(vehicleRepository, never()).save(any());
        }
    }

    // ================================================================
    // deleteMyVehicle()
    // ================================================================

    @Nested
    @DisplayName("deleteMyVehicle()")
    class DeleteMyVehicleTests {

        @Test
        @DisplayName("Elimina el vehículo correctamente cuando pertenece al usuario")
        void givenOwnVehicle_thenDelete() {
            // Arrange
            UserEntity user = buildUser(1L);
            VehicleEntity vehicle = buildVehicle(10L, "ABC123", user);

            when(currentUserService.getAuthenticatedUser()).thenReturn(user);
            when(vehicleRepository.findById(10L)).thenReturn(Optional.of(vehicle));

            // Act
            assertThatCode(() -> sut.deleteMyVehicle(10L))
                    .doesNotThrowAnyException();

            // Assert
            verify(vehicleRepository, times(1)).delete(vehicle);
        }

        @Test
        @DisplayName("Lanza VehicleNotFoundException si el vehículo no existe")
        void givenNonExistentVehicle_thenThrow() {
            // Arrange
            when(currentUserService.getAuthenticatedUser()).thenReturn(buildUser(1L));
            when(vehicleRepository.findById(99L)).thenReturn(Optional.empty());

            // Act + Assert
            assertThatThrownBy(() -> sut.deleteMyVehicle(99L))
                    .isInstanceOf(VehicleNotFoundException.class);

            verify(vehicleRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Lanza SecurityException si el vehículo pertenece a otro usuario")
        void givenVehicleOfAnotherUser_thenThrow() {
            // Arrange
            UserEntity currentUser = buildUser(1L);
            UserEntity otherUser   = buildUser(2L);
            VehicleEntity vehicle  = buildVehicle(10L, "ABC123", otherUser);

            when(currentUserService.getAuthenticatedUser()).thenReturn(currentUser);
            when(vehicleRepository.findById(10L)).thenReturn(Optional.of(vehicle));

            // Act + Assert
            assertThatThrownBy(() -> sut.deleteMyVehicle(10L))
                    .isInstanceOf(SecurityException.class);

            verify(vehicleRepository, never()).delete(any());
        }
    }
}