package com.sparktech.motorx.Services.impl;

import com.sparktech.motorx.dto.auth.RegisterUserDTO;
import com.sparktech.motorx.dto.employee.CreateEmployeeRequestDTO;
import com.sparktech.motorx.dto.employee.EmployeeResponseDTO;
import com.sparktech.motorx.dto.employee.UpdateEmployeeRequestDTO;
import com.sparktech.motorx.dto.vehicle.TransferVehicleOwnershipRequestDTO;
import com.sparktech.motorx.dto.vehicle.VehicleResponseDTO;
import com.sparktech.motorx.entity.*;
import com.sparktech.motorx.exception.EmployeeNotFoundException;
import com.sparktech.motorx.exception.VehicleNotFoundException;
import com.sparktech.motorx.mapper.EmployeeMapper;
import com.sparktech.motorx.mapper.VehicleMapper;
import com.sparktech.motorx.repository.JpaEmployeeRepository;
import com.sparktech.motorx.repository.JpaUserRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmployeeServiceImpl - Unit Tests")
class EmployeeServiceImplTest {

    // ================================================================
    // MOCKS
    // ================================================================
    @Mock private JpaEmployeeRepository employeeRepository;
    @Mock private JpaUserRepository userRepository;
    @Mock private JpaVehicleRepository vehicleRepository;
    @Mock private EmployeeMapper employeeMapper;
    @Mock private VehicleMapper vehicleMapper;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private EmployeeServiceImpl sut;

    @Captor private ArgumentCaptor<UserEntity> userCaptor;
    @Captor private ArgumentCaptor<EmployeeEntity> employeeCaptor;

    // ================================================================
    // BUILDERS
    // ================================================================

    private UserEntity buildUser(Long id, Role role, boolean enabled) {
        UserEntity u = new UserEntity();
        u.setId(id);
        u.setName("Usuario " + id);
        u.setEmail("user" + id + "@test.com");
        u.setDni("DNI" + id);
        u.setPhone("300000000" + id);
        u.setRole(role);
        u.setEnabled(enabled);
        u.setAccountLocked(false);
        return u;
    }

    private EmployeeEntity buildEmployee(Long id) {
        EmployeeEntity e = new EmployeeEntity();
        e.setId(id);
        e.setPosition(EmployeePosition.MECANICO);
        e.setState(EmployeeState.AVAILABLE);
        e.setUser(buildUser(id, Role.EMPLOYEE, true));
        return e;
    }

    private VehicleEntity buildVehicle(Long id, String plate, UserEntity owner) {
        VehicleEntity v = new VehicleEntity();
        v.setId(id);
        v.setLicensePlate(plate);
        v.setBrand("HONDA");
        v.setModel("CB 190");
        v.setOwner(owner);
        return v;
    }

    /** DTO mínimo para crear empleado */
    private CreateEmployeeRequestDTO buildCreateRequest(String email, String dni) {
        // 1. Instanciamos el DTO de usuario (asegúrate que los campos coincidan con tu RegisterUserDTO)
        RegisterUserDTO userData = new RegisterUserDTO(
                "Nuevo Empleado",
                dni,
                email,
                "pass123",
                "3001234567"
        );

        // 2. Retornamos el record respetando el orden: (Position, User)
        return new CreateEmployeeRequestDTO(EmployeePosition.MECANICO, userData);
    }

    // ================================================================
    // createEmployee()
    // ================================================================

    @Nested
    @DisplayName("createEmployee()")
    class CreateEmployeeTests {

        @Test
        @DisplayName("Crea usuario y empleado correctamente en el camino feliz")
        void givenValidRequest_thenPersistUserAndEmployee() {
            // Arrange
            CreateEmployeeRequestDTO request = buildCreateRequest("nuevo@test.com", "987654321");
            UserEntity savedUser = buildUser(5L, Role.EMPLOYEE, true);
            EmployeeEntity savedEmployee = buildEmployee(10L);
            EmployeeResponseDTO expectedDTO = mock(EmployeeResponseDTO.class);

            when(userRepository.existsByEmail("nuevo@test.com")).thenReturn(false);
            when(userRepository.existsByDni("987654321")).thenReturn(false);
            when(passwordEncoder.encode("pass123")).thenReturn("encoded-pass");
            when(userRepository.save(any(UserEntity.class))).thenReturn(savedUser);
            when(employeeRepository.save(any(EmployeeEntity.class))).thenReturn(savedEmployee);
            when(employeeMapper.toResponseDTO(savedEmployee)).thenReturn(expectedDTO);

            // Act
            EmployeeResponseDTO result = sut.createEmployee(request);

            // Assert
            assertThat(result).isEqualTo(expectedDTO);
            verify(userRepository, times(1)).save(any());
            verify(employeeRepository, times(1)).save(any());
        }

        @Test
        @DisplayName("El UserEntity persistido tiene rol EMPLOYEE, enabled=true y password encriptada")
        void givenValidRequest_thenUserEntityHasCorrectFields() {
            // Arrange
            CreateEmployeeRequestDTO request = buildCreateRequest("nuevo@test.com", "987654321");
            UserEntity savedUser = buildUser(5L, Role.EMPLOYEE, true);

            when(userRepository.existsByEmail(any())).thenReturn(false);
            when(userRepository.existsByDni(any())).thenReturn(false);
            when(passwordEncoder.encode("pass123")).thenReturn("hashed");
            when(userRepository.save(any())).thenReturn(savedUser);
            when(employeeRepository.save(any())).thenReturn(buildEmployee(1L));
            when(employeeMapper.toResponseDTO(any())).thenReturn(mock(EmployeeResponseDTO.class));

            // Act
            sut.createEmployee(request);

            // Assert
            verify(userRepository).save(userCaptor.capture());
            UserEntity captured = userCaptor.getValue();
            assertThat(captured.getRole()).isEqualTo(Role.EMPLOYEE);
            assertThat(captured.isEnabled()).isTrue();
            assertThat(captured.isAccountLocked()).isFalse();
            assertThat(captured.getPassword()).isEqualTo("hashed");
        }

        @Test
        @DisplayName("El EmployeeEntity persistido tiene estado AVAILABLE y posición del request")
        void givenValidRequest_thenEmployeeEntityHasCorrectFields() {
            // Arrange
            CreateEmployeeRequestDTO request = buildCreateRequest("nuevo@test.com", "123456789");
            UserEntity savedUser = buildUser(5L, Role.EMPLOYEE, true);

            when(userRepository.existsByEmail(any())).thenReturn(false);
            when(userRepository.existsByDni(any())).thenReturn(false);
            when(passwordEncoder.encode(any())).thenReturn("hashed");
            when(userRepository.save(any())).thenReturn(savedUser);
            when(employeeRepository.save(any())).thenReturn(buildEmployee(1L));
            when(employeeMapper.toResponseDTO(any())).thenReturn(mock(EmployeeResponseDTO.class));

            // Act
            sut.createEmployee(request);

            // Assert
            verify(employeeRepository).save(employeeCaptor.capture());
            EmployeeEntity captured = employeeCaptor.getValue();
            assertThat(captured.getState()).isEqualTo(EmployeeState.AVAILABLE);
            assertThat(captured.getPosition()).isEqualTo(EmployeePosition.MECANICO);
            assertThat(captured.getUser()).isEqualTo(savedUser);
        }

        @Test
        @DisplayName("Lanza IllegalArgumentException si el email ya existe")
        void givenDuplicateEmail_thenThrow() {
            // Arrange
            CreateEmployeeRequestDTO request = buildCreateRequest("dup@test.com", "111111111");
            when(userRepository.existsByEmail("dup@test.com")).thenReturn(true);

            // Act + Assert
            assertThatThrownBy(() -> sut.createEmployee(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("dup@test.com");

            verifyNoInteractions(employeeRepository, passwordEncoder);
        }

        @Test
        @DisplayName("Lanza IllegalArgumentException si el DNI ya existe")
        void givenDuplicateDni_thenThrow() {
            // Arrange
            CreateEmployeeRequestDTO request = buildCreateRequest("new@test.com", "DUP-DNI");
            when(userRepository.existsByEmail(any())).thenReturn(false);
            when(userRepository.existsByDni("DUP-DNI")).thenReturn(true);

            // Act + Assert
            assertThatThrownBy(() -> sut.createEmployee(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("DUP-DNI");

            verifyNoInteractions(employeeRepository, passwordEncoder);
        }
    }

    // ================================================================
    // getAllEmployees()
    // ================================================================

    @Nested
    @DisplayName("getAllEmployees()")
    class GetAllEmployeesTests {

        @Test
        @DisplayName("Retorna lista mapeada correctamente")
        void givenEmployeesExist_thenReturnMappedList() {
            // Arrange
            EmployeeEntity e1 = buildEmployee(1L);
            EmployeeEntity e2 = buildEmployee(2L);
            EmployeeResponseDTO dto1 = mock(EmployeeResponseDTO.class);
            EmployeeResponseDTO dto2 = mock(EmployeeResponseDTO.class);

            when(employeeRepository.findAll()).thenReturn(List.of(e1, e2));
            when(employeeMapper.toResponseDTO(e1)).thenReturn(dto1);
            when(employeeMapper.toResponseDTO(e2)).thenReturn(dto2);

            // Act
            List<EmployeeResponseDTO> result = sut.getAllEmployees();

            // Assert
            assertThat(result).containsExactly(dto1, dto2);
        }

        @Test
        @DisplayName("Retorna lista vacía cuando no hay empleados")
        void givenNoEmployees_thenReturnEmptyList() {
            // Arrange
            when(employeeRepository.findAll()).thenReturn(List.of());

            // Act + Assert
            assertThat(sut.getAllEmployees()).isEmpty();
        }
    }

    // ================================================================
    // getEmployeeById()
    // ================================================================

    @Nested
    @DisplayName("getEmployeeById()")
    class GetEmployeeByIdTests {

        @Test
        @DisplayName("Retorna DTO cuando el empleado existe")
        void givenExistingEmployee_thenReturnDTO() {
            // Arrange
            EmployeeEntity emp = buildEmployee(1L);
            EmployeeResponseDTO dto = mock(EmployeeResponseDTO.class);
            when(employeeRepository.findById(1L)).thenReturn(Optional.of(emp));
            when(employeeMapper.toResponseDTO(emp)).thenReturn(dto);

            // Act
            EmployeeResponseDTO result = sut.getEmployeeById(1L);

            // Assert
            assertThat(result).isEqualTo(dto);
        }

        @Test
        @DisplayName("Lanza EmployeeNotFoundException si no existe")
        void givenNonExistentEmployee_thenThrow() {
            // Arrange
            when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

            // Act + Assert
            assertThatThrownBy(() -> sut.getEmployeeById(99L))
                    .isInstanceOf(EmployeeNotFoundException.class);
        }
    }

    // ================================================================
    // updateEmployee()
    // ================================================================

    @Nested
    @DisplayName("updateEmployee()")
    class UpdateEmployeeTests {

        @Test
        @DisplayName("Actualiza posición y estado correctamente")
        void givenValidRequest_thenUpdateAndReturnDTO() {
            // Arrange
            EmployeeEntity emp = buildEmployee(1L);
            UpdateEmployeeRequestDTO request =
                    new UpdateEmployeeRequestDTO(EmployeePosition.MECANICO, EmployeeState.NOT_AVAILABLE);
            EmployeeResponseDTO dto = mock(EmployeeResponseDTO.class);

            when(employeeRepository.findById(1L)).thenReturn(Optional.of(emp));
            when(employeeRepository.save(emp)).thenReturn(emp);
            when(employeeMapper.toResponseDTO(emp)).thenReturn(dto);

            // Act
            EmployeeResponseDTO result = sut.updateEmployee(1L, request);

            // Assert
            assertThat(result).isEqualTo(dto);
            verify(employeeRepository).save(argThat(e ->
                    e.getState() == EmployeeState.NOT_AVAILABLE &&
                            e.getPosition() == EmployeePosition.MECANICO
            ));
        }

        @Test
        @DisplayName("Lanza EmployeeNotFoundException si el empleado no existe")
        void givenNonExistentEmployee_thenThrow() {
            // Arrange
            Long nonExistentId = 99L;
            UpdateEmployeeRequestDTO request = new UpdateEmployeeRequestDTO(
                    EmployeePosition.MECANICO,
                    EmployeeState.AVAILABLE
            );

            when(employeeRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // Act + Assert
            assertThatThrownBy(() -> sut.updateEmployee(nonExistentId, request))
                    .isInstanceOf(EmployeeNotFoundException.class);

            verify(employeeRepository, never()).save(any());
        }
    }

    // ================================================================
    // deleteEmployee()
    // ================================================================

    @Nested
    @DisplayName("deleteEmployee()")
    class DeleteEmployeeTests {

        @Test
        @DisplayName("Elimina el empleado correctamente (cascade al user lo maneja la DB)")
        void givenExistingEmployee_thenDelete() {
            // Arrange
            EmployeeEntity emp = buildEmployee(1L);
            when(employeeRepository.findById(1L)).thenReturn(Optional.of(emp));

            // Act
            assertThatCode(() -> sut.deleteEmployee(1L))
                    .doesNotThrowAnyException();

            // Assert
            verify(employeeRepository, times(1)).delete(emp);
            // userRepository NO debe ser llamado — cascade está en la DB
            verifyNoInteractions(userRepository);
        }

        @Test
        @DisplayName("Lanza EmployeeNotFoundException si no existe")
        void givenNonExistentEmployee_thenThrow() {
            // Arrange
            when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

            // Act + Assert
            assertThatThrownBy(() -> sut.deleteEmployee(99L))
                    .isInstanceOf(EmployeeNotFoundException.class);

            verify(employeeRepository, never()).delete(any());
        }
    }

    // ================================================================
    // transferVehicleOwnership()
    // ================================================================

    @Nested
    @DisplayName("transferVehicleOwnership()")
    class TransferVehicleOwnershipTests {

        private TransferVehicleOwnershipRequestDTO buildTransferRequest(Long newOwnerId) {
            return new TransferVehicleOwnershipRequestDTO(newOwnerId);
        }

        @Test
        @DisplayName("Transfiere correctamente cuando todos los datos son válidos")
        void givenValidTransfer_thenOwnerIsUpdatedAndDTOReturned() {
            // Arrange
            UserEntity currentOwner = buildUser(1L, Role.CLIENT, true);
            UserEntity newOwner = buildUser(2L, Role.CLIENT, true);
            VehicleEntity vehicle = buildVehicle(10L, "ABC3AX", currentOwner);
            VehicleResponseDTO dto = mock(VehicleResponseDTO.class);

            when(vehicleRepository.findById(10L)).thenReturn(Optional.of(vehicle));
            when(userRepository.findById(2L)).thenReturn(Optional.of(newOwner));
            when(vehicleRepository.findByOwnerId(2L)).thenReturn(List.of()); // sin moto previa
            when(vehicleRepository.save(any())).thenReturn(vehicle);
            when(vehicleMapper.toResponseDTO(vehicle)).thenReturn(dto);

            // Act
            VehicleResponseDTO result = sut.transferVehicleOwnership(10L, buildTransferRequest(2L));

            // Assert
            assertThat(result).isEqualTo(dto);
            verify(vehicleRepository).save(argThat(v ->
                    v.getOwner().getId().equals(2L)
            ));
        }
        @Test
        @DisplayName("Lanza VehicleNotFoundException si el vehículo no existe")
        void givenNonExistentVehicle_thenThrow() {
            // Arrange
            Long vehicleId = 99L;
            var request = buildTransferRequest(2L); // Extraído de la lambda

            when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.empty());

            // Act + Assert
            // La lambda ahora contiene ÚNICAMENTE la ejecución del método bajo prueba
            assertThatThrownBy(() -> sut.transferVehicleOwnership(vehicleId, request))
                    .isInstanceOf(VehicleNotFoundException.class);

            verifyNoInteractions(userRepository);
            verify(vehicleRepository, never()).save(any()); // Opcional: refuerza que no hubo cambios
        }

        @Test
        @DisplayName("Lanza IllegalArgumentException si el nuevo propietario no existe")
        void givenNonExistentNewOwner_thenThrow() {
            // Arrange
            Long vehicleId = 10L;
            Long nonExistentOwnerId = 99L;

            UserEntity currentOwner = buildUser(1L, Role.CLIENT, true);
            VehicleEntity vehicle = buildVehicle(vehicleId, "ABC3AX", currentOwner);
            var request = buildTransferRequest(nonExistentOwnerId); // Extraído para limpiar la lambda

            when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
            when(userRepository.findById(nonExistentOwnerId)).thenReturn(Optional.empty());

            // Act + Assert
            assertThatThrownBy(() -> sut.transferVehicleOwnership(vehicleId, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(String.valueOf(nonExistentOwnerId));
        }

        @Test
        @DisplayName("Lanza IllegalArgumentException si el nuevo propietario no tiene rol CLIENT")
        void givenNewOwnerNotClient_thenThrow() {
            // Arrange
            Long vehicleId = 10L;
            Long nonClientId = 2L;

            UserEntity currentOwner = buildUser(1L, Role.CLIENT, true);
            UserEntity nonClient = buildUser(nonClientId, Role.EMPLOYEE, true);
            VehicleEntity vehicle = buildVehicle(vehicleId, "ABC3AX", currentOwner);
            var request = buildTransferRequest(nonClientId); // Extraído de la lambda

            when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
            when(userRepository.findById(nonClientId)).thenReturn(Optional.of(nonClient));

            // Act + Assert
            // La lambda ahora es una "Single Statement Lambda"
            assertThatThrownBy(() -> sut.transferVehicleOwnership(vehicleId, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("CLIENT");

            verify(vehicleRepository, never()).save(any());
        }

        @Test
        @DisplayName("Lanza IllegalArgumentException si el nuevo propietario está deshabilitado")
        void givenDisabledNewOwner_thenThrow() {
            // Arrange
            Long vehicleId = 10L;
            Long disabledOwnerId = 2L;
            UserEntity currentOwner = buildUser(1L, Role.CLIENT, true);
            UserEntity disabledOwner = buildUser(disabledOwnerId, Role.CLIENT, false);
            VehicleEntity vehicle = buildVehicle(vehicleId, "ABC3AX", currentOwner);
            var request = buildTransferRequest(disabledOwnerId);

            when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
            when(userRepository.findById(disabledOwnerId)).thenReturn(Optional.of(disabledOwner));

            // Act + Assert
            assertThatThrownBy(() -> sut.transferVehicleOwnership(vehicleId, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("deshabilitada");

            verify(vehicleRepository, never()).save(any());
        }

        @Test
        @DisplayName("Lanza IllegalArgumentException si el vehículo ya pertenece al mismo usuario")
        void givenSameOwner_thenThrow() {
            // Arrange
            Long vehicleId = 10L;
            Long ownerId = 1L;
            UserEntity owner = buildUser(ownerId, Role.CLIENT, true);
            VehicleEntity vehicle = buildVehicle(vehicleId, "ABC3AX", owner);
            var request = buildTransferRequest(ownerId);

            when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
            when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));

            // Act + Assert
            assertThatThrownBy(() -> sut.transferVehicleOwnership(vehicleId, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("ya pertenece");

            verify(vehicleRepository, never()).save(any());
        }

        @Test
        @DisplayName("Lanza IllegalArgumentException si el nuevo propietario ya tiene una moto con la misma placa")
        void givenNewOwnerAlreadyHasPlate_thenThrow() {
            // Arrange
            Long vehicleId = 10L;
            Long newOwnerId = 2L;
            String plate = "ABC3AX";

            UserEntity currentOwner = buildUser(1L, Role.CLIENT, true);
            UserEntity newOwner = buildUser(newOwnerId, Role.CLIENT, true);
            VehicleEntity vehicle = buildVehicle(vehicleId, plate, currentOwner);
            VehicleEntity existingVehicle = buildVehicle(20L, plate, newOwner);
            var request = buildTransferRequest(newOwnerId);

            when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
            when(userRepository.findById(newOwnerId)).thenReturn(Optional.of(newOwner));
            when(vehicleRepository.findByOwnerId(newOwnerId)).thenReturn(List.of(existingVehicle));

            // Act + Assert
            assertThatThrownBy(() -> sut.transferVehicleOwnership(vehicleId, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(plate);

            verify(vehicleRepository, never()).save(any());
        }
    }

    // ================================================================
    // getAllVehicles() / getVehicleById()
    // ================================================================

    @Nested
    @DisplayName("Consultas de vehículos")
    class VehicleQueryTests {

        @Test
        @DisplayName("getAllVehicles() retorna lista mapeada correctamente")
        void givenVehiclesExist_thenReturnMappedList() {
            // Arrange
            UserEntity owner = buildUser(1L, Role.CLIENT, true);
            VehicleEntity v1 = buildVehicle(1L, "AAA1AX", owner);
            VehicleEntity v2 = buildVehicle(2L, "BBB2BX", owner);
            VehicleResponseDTO dto1 = mock(VehicleResponseDTO.class);
            VehicleResponseDTO dto2 = mock(VehicleResponseDTO.class);

            when(vehicleRepository.findAll()).thenReturn(List.of(v1, v2));
            when(vehicleMapper.toResponseDTO(v1)).thenReturn(dto1);
            when(vehicleMapper.toResponseDTO(v2)).thenReturn(dto2);

            // Act
            List<VehicleResponseDTO> result = sut.getAllVehicles();

            // Assert
            assertThat(result).containsExactly(dto1, dto2);
        }

        @Test
        @DisplayName("getAllVehicles() retorna lista vacía cuando no hay vehículos")
        void givenNoVehicles_thenReturnEmptyList() {
            // Arrange
            when(vehicleRepository.findAll()).thenReturn(List.of());

            // Act + Assert
            assertThat(sut.getAllVehicles()).isEmpty();
        }

        @Test
        @DisplayName("getVehicleById() retorna DTO cuando existe")
        void givenExistingVehicle_thenReturnDTO() {
            // Arrange
            UserEntity owner = buildUser(1L, Role.CLIENT, true);
            VehicleEntity vehicle = buildVehicle(5L, "ABC3AX", owner);
            VehicleResponseDTO dto = mock(VehicleResponseDTO.class);

            when(vehicleRepository.findById(5L)).thenReturn(Optional.of(vehicle));
            when(vehicleMapper.toResponseDTO(vehicle)).thenReturn(dto);

            // Act
            VehicleResponseDTO result = sut.getVehicleById(5L);

            // Assert
            assertThat(result).isEqualTo(dto);
        }

        @Test
        @DisplayName("getVehicleById() lanza VehicleNotFoundException si no existe")
        void givenNonExistentVehicle_thenThrow() {
            // Arrange
            when(vehicleRepository.findById(99L)).thenReturn(Optional.empty());

            // Act + Assert
            assertThatThrownBy(() -> sut.getVehicleById(99L))
                    .isInstanceOf(VehicleNotFoundException.class);
        }
    }
}