package com.sparktech.motorx.Services.impl;

import com.sparktech.motorx.dto.user.AdminUserResponseDTO;
import com.sparktech.motorx.entity.UserEntity;
import com.sparktech.motorx.exception.UserAlreadyBlockedException;
import com.sparktech.motorx.exception.UserAlreadyDeletedException;
import com.sparktech.motorx.exception.UserNotFoundException;
import com.sparktech.motorx.repository.JpaUserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminUserServiceImpl - Unit Tests")
class AdminUserServiceImplTest {

    @Mock
    private JpaUserRepository jpaUserRepository;

    @InjectMocks
    private AdminUserServiceImpl sut;

    // ================================================================
    // BUILDERS
    // ================================================================

    private UserEntity buildUser(Long id) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setName("Usuario " + id);
        user.setDni("123456789" + id);
        user.setEmail("usuario" + id + "@test.com");
        user.setPhone("300000000" + id);
        user.setEnabled(true);
        user.setAccountLocked(false);
        user.setDeletedAt(null);
        user.setCreatedAt(LocalDateTime.of(2024, 1, 1, 0, 0));
        user.setUpdatedAt(LocalDateTime.of(2024, 6, 1, 0, 0));
        return user;
    }

    // ================================================================
    // getAllUsers()
    // ================================================================

    @Nested
    @DisplayName("getAllUsers()")
    class GetAllUsersTests {

        @Test
        @DisplayName("Retorna lista mapeada correctamente cuando hay usuarios")
        void givenUsersExist_thenReturnMappedList() {
            // Arrange
            UserEntity u1 = buildUser(1L);
            UserEntity u2 = buildUser(2L);
            when(jpaUserRepository.findAll()).thenReturn(List.of(u1, u2));

            // Act
            List<AdminUserResponseDTO> result = sut.getAllUsers();

            // Assert
            assertThat(result).hasSize(2);
            assertThat(result.get(0).id()).isEqualTo(1L);
            assertThat(result.get(1).id()).isEqualTo(2L);
            verify(jpaUserRepository, times(1)).findAll();
        }

        @Test
        @DisplayName("Retorna lista vacía cuando no hay usuarios")
        void givenNoUsers_thenReturnEmptyList() {
            // Arrange
            when(jpaUserRepository.findAll()).thenReturn(List.of());

            // Act
            List<AdminUserResponseDTO> result = sut.getAllUsers();

            // Assert
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("El DTO mapeado contiene todos los campos del entity")
        void givenUser_thenDTOContainsAllFields() {
            // Arrange
            UserEntity user = buildUser(1L);
            when(jpaUserRepository.findAll()).thenReturn(List.of(user));

            // Act
            AdminUserResponseDTO dto = sut.getAllUsers().getFirst();

            // Assert — validar que toResponseDTO mapea TODOS los campos
            assertThat(dto.id()).isEqualTo(user.getId());
            assertThat(dto.name()).isEqualTo(user.getName());
            assertThat(dto.dni()).isEqualTo(user.getDni());
            assertThat(dto.email()).isEqualTo(user.getEmail());
            assertThat(dto.phone()).isEqualTo(user.getPhone());
            assertThat(dto.role()).isEqualTo(user.getRole());
            assertThat(dto.enabled()).isEqualTo(user.isEnabled());
            assertThat(dto.accountLocked()).isEqualTo(user.isAccountLocked());
            assertThat(dto.createdAt()).isEqualTo(user.getCreatedAt());
            assertThat(dto.updatedAt()).isEqualTo(user.getUpdatedAt());
            assertThat(dto.deletedAt()).isNull();
        }
    }

    // ================================================================
    // getUserById()
    // ================================================================

    @Nested
    @DisplayName("getUserById()")
    class GetUserByIdTests {

        @Test
        @DisplayName("Retorna DTO cuando el usuario existe")
        void givenExistingUser_thenReturnDTO() {
            // Arrange
            UserEntity user = buildUser(1L);
            when(jpaUserRepository.findById(1L)).thenReturn(Optional.of(user));

            // Act
            AdminUserResponseDTO result = sut.getUserById(1L);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.email()).isEqualTo("usuario1@test.com");
        }

        @Test
        @DisplayName("Lanza UserNotFoundException cuando el usuario no existe")
        void givenNonExistentUser_thenThrowUserNotFoundException() {
            // Arrange
            when(jpaUserRepository.findById(99L)).thenReturn(Optional.empty());

            // Act + Assert
            assertThatThrownBy(() -> sut.getUserById(99L))
                    .isInstanceOf(UserNotFoundException.class);

            verify(jpaUserRepository, never()).save(any());
        }
    }

    // ================================================================
    // blockUser()
    // ================================================================

    @Nested
    @DisplayName("blockUser()")
    class BlockUserTests {

        @Test
        @DisplayName("Bloquea exitosamente un usuario no bloqueado")
        void givenUnblockedUser_thenBlockAndReturnDTO() {
            // Arrange
            UserEntity user = buildUser(1L);
            user.setAccountLocked(false);
            when(jpaUserRepository.findById(1L)).thenReturn(Optional.of(user));
            when(jpaUserRepository.save(user)).thenReturn(user);

            // Act
            AdminUserResponseDTO result = sut.blockUser(1L);

            // Assert
            assertThat(result.accountLocked()).isTrue();
            verify(jpaUserRepository, times(1)).save(user);
        }

        @Test
        @DisplayName("El entity queda con accountLocked=true tras blockUser()")
        void givenUnblockedUser_thenEntityIsPersistedAsLocked() {
            // Arrange
            UserEntity user = buildUser(1L);
            user.setAccountLocked(false);
            when(jpaUserRepository.findById(1L)).thenReturn(Optional.of(user));
            when(jpaUserRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // Act
            sut.blockUser(1L);

            // Assert — verificar que lo que se persistió tiene accountLocked=true
            verify(jpaUserRepository).save(argThat(UserEntity::isAccountLocked));
        }

        @Test
        @DisplayName("Lanza UserAlreadyBlockedException si el usuario ya está bloqueado")
        void givenAlreadyBlockedUser_thenThrowUserAlreadyBlockedException() {
            // Arrange
            UserEntity user = buildUser(1L);
            user.setAccountLocked(true);
            when(jpaUserRepository.findById(1L)).thenReturn(Optional.of(user));

            // Act + Assert
            assertThatThrownBy(() -> sut.blockUser(1L))
                    .isInstanceOf(UserAlreadyBlockedException.class);

            verify(jpaUserRepository, never()).save(any());
        }

        @Test
        @DisplayName("Lanza UserNotFoundException si el usuario no existe")
        void givenNonExistentUser_thenThrowUserNotFoundException() {
            // Arrange
            when(jpaUserRepository.findById(99L)).thenReturn(Optional.empty());

            // Act + Assert
            assertThatThrownBy(() -> sut.blockUser(99L))
                    .isInstanceOf(UserNotFoundException.class);

            verify(jpaUserRepository, never()).save(any());
        }
    }

    // ================================================================
    // unblockUser()
    // ================================================================

    @Nested
    @DisplayName("unblockUser()")
    class UnblockUserTests {

        @Test
        @DisplayName("Desbloquea exitosamente un usuario bloqueado")
        void givenBlockedUser_thenUnblockAndReturnDTO() {
            // Arrange
            UserEntity user = buildUser(1L);
            user.setAccountLocked(true);
            when(jpaUserRepository.findById(1L)).thenReturn(Optional.of(user));
            when(jpaUserRepository.save(user)).thenReturn(user);

            // Act
            AdminUserResponseDTO result = sut.unblockUser(1L);

            // Assert
            assertThat(result.accountLocked()).isFalse();
            verify(jpaUserRepository, times(1)).save(user);
        }

        @Test
        @DisplayName("Desbloquear usuario ya desbloqueado no lanza excepción (idempotente)")
        void givenAlreadyUnblockedUser_thenNoExceptionThrown() {
            // Arrange — unblockUser no valida si ya estaba desbloqueado, es idempotente
            UserEntity user = buildUser(1L);
            user.setAccountLocked(false);
            when(jpaUserRepository.findById(1L)).thenReturn(Optional.of(user));
            when(jpaUserRepository.save(user)).thenReturn(user);

            // Act + Assert
            assertThatCode(() -> sut.unblockUser(1L))
                    .doesNotThrowAnyException();

            verify(jpaUserRepository, times(1)).save(user);
        }

        @Test
        @DisplayName("El entity queda con accountLocked=false tras unblockUser()")
        void givenBlockedUser_thenEntityIsPersistedAsUnlocked() {
            // Arrange
            UserEntity user = buildUser(1L);
            user.setAccountLocked(true);
            when(jpaUserRepository.findById(1L)).thenReturn(Optional.of(user));
            when(jpaUserRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // Act
            sut.unblockUser(1L);

            // Assert
            verify(jpaUserRepository).save(argThat(u -> !u.isAccountLocked()));
        }

        @Test
        @DisplayName("Lanza UserNotFoundException si el usuario no existe")
        void givenNonExistentUser_thenThrowUserNotFoundException() {
            // Arrange
            when(jpaUserRepository.findById(99L)).thenReturn(Optional.empty());

            // Act + Assert
            assertThatThrownBy(() -> sut.unblockUser(99L))
                    .isInstanceOf(UserNotFoundException.class);

            verify(jpaUserRepository, never()).save(any());
        }
    }

    // ================================================================
    // deleteUser()
    // ================================================================

    @Nested
    @DisplayName("deleteUser()")
    class DeleteUserTests {

        @Test
        @DisplayName("Soft-delete exitoso: setea deletedAt, deshabilita y bloquea")
        void givenActiveUser_thenSoftDeleteCorrectly() {
            // Arrange
            UserEntity user = buildUser(1L);
            user.setDeletedAt(null);
            user.setEnabled(true);
            user.setAccountLocked(false);
            when(jpaUserRepository.findById(1L)).thenReturn(Optional.of(user));
            when(jpaUserRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // Act
            sut.deleteUser(1L);

            // Assert — verificar los tres campos que cambia el soft-delete
            verify(jpaUserRepository).save(argThat(u ->
                    u.getDeletedAt() != null &&
                            !u.isEnabled() &&
                            u.isAccountLocked()
            ));
        }

        @Test
        @DisplayName("deleteUser() no retorna valor (void) y no lanza excepción en el camino feliz")
        void givenActiveUser_thenNoExceptionThrown() {
            // Arrange
            UserEntity user = buildUser(1L);
            when(jpaUserRepository.findById(1L)).thenReturn(Optional.of(user));
            when(jpaUserRepository.save(any())).thenReturn(user);

            // Act + Assert
            assertThatCode(() -> sut.deleteUser(1L))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("deletedAt queda con una fecha cercana al momento actual")
        void givenActiveUser_thenDeletedAtIsSetToNow() {
            // Arrange
            UserEntity user = buildUser(1L);
            LocalDateTime before = LocalDateTime.now().minusSeconds(1);
            when(jpaUserRepository.findById(1L)).thenReturn(Optional.of(user));
            when(jpaUserRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // Act
            sut.deleteUser(1L);

            LocalDateTime after = LocalDateTime.now().plusSeconds(1);

            // Assert — deletedAt debe estar entre before y after
            assertThat(user.getDeletedAt())
                    .isAfterOrEqualTo(before)
                    .isBeforeOrEqualTo(after);
        }

        @Test
        @DisplayName("Lanza UserAlreadyDeletedException si el usuario ya fue eliminado")
        void givenAlreadyDeletedUser_thenThrowUserAlreadyDeletedException() {
            // Arrange
            UserEntity user = buildUser(1L);
            user.setDeletedAt(LocalDateTime.of(2024, 1, 1, 12, 0)); // ya eliminado
            when(jpaUserRepository.findById(1L)).thenReturn(Optional.of(user));

            // Act + Assert
            assertThatThrownBy(() -> sut.deleteUser(1L))
                    .isInstanceOf(UserAlreadyDeletedException.class);

            verify(jpaUserRepository, never()).save(any());
        }

        @Test
        @DisplayName("Lanza UserNotFoundException si el usuario no existe")
        void givenNonExistentUser_thenThrowUserNotFoundException() {
            // Arrange
            when(jpaUserRepository.findById(99L)).thenReturn(Optional.empty());

            // Act + Assert
            assertThatThrownBy(() -> sut.deleteUser(99L))
                    .isInstanceOf(UserNotFoundException.class);

            verify(jpaUserRepository, never()).save(any());
        }
    }
}