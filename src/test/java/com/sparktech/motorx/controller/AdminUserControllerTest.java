package com.sparktech.motorx.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sparktech.motorx.Services.IAdminUserService;
import com.sparktech.motorx.controller.error.GlobalControllerAdvice;
import com.sparktech.motorx.dto.user.AdminUserResponseDTO;
import com.sparktech.motorx.entity.Role;
import com.sparktech.motorx.exception.UserAlreadyBlockedException;
import com.sparktech.motorx.exception.UserAlreadyDeletedException;
import com.sparktech.motorx.exception.UserNotFoundException;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AdminUserController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalControllerAdvice.class, AdminUserControllerTest.TestConfig.class})
@DisplayName("AdminUserController - Tests")
class AdminUserControllerTest {

    @Resource
    private MockMvc mockMvc;

    @Resource
    private IAdminUserService adminUserService;

    @BeforeEach
    void setUp() {
        reset(adminUserService);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    // ---------------------------------------------------------------
    // Fixtures
    // ---------------------------------------------------------------

    private AdminUserResponseDTO buildActiveUser(Long id) {
        return new AdminUserResponseDTO(
                id,
                "María García",
                "87654321",
                "maria.garcia@mail.com",
                "555-9876",
                Role.CLIENT,
                true,
                false,
                LocalDateTime.of(2025, 1, 10, 8, 0),
                LocalDateTime.of(2025, 1, 10, 8, 0),
                null
        );
    }

    private AdminUserResponseDTO buildBlockedUser(Long id) {
        return new AdminUserResponseDTO(
                id,
                "Carlos López",
                "11223344",
                "carlos.lopez@mail.com",
                "555-1111",
                Role.CLIENT,
                false,
                true,
                LocalDateTime.of(2025, 2, 1, 9, 0),
                LocalDateTime.of(2025, 3, 1, 9, 0),
                null
        );
    }

    private AdminUserResponseDTO buildDeletedUser() {
        return new AdminUserResponseDTO(
                3L,
                "Pedro Ruiz",
                "99887766",
                "pedro.ruiz@mail.com",
                "555-2222",
                Role.CLIENT,
                false,
                true,
                LocalDateTime.of(2025, 1, 5, 10, 0),
                LocalDateTime.of(2025, 4, 1, 10, 0),
                LocalDateTime.of(2025, 4, 1, 10, 0)
        );
    }

    // ---------------------------------------------------------------
    // GET /api/v1/admin/users
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("GET /api/v1/admin/users")
    class GetAllUsers {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("200 - retorna lista completa incluyendo eliminados")
        void shouldReturnAllUsers() throws Exception {
            // Arrange
            List<AdminUserResponseDTO> users = List.of(
                    buildActiveUser(1L),
                    buildBlockedUser(2L),
                    buildDeletedUser()
            );
            when(adminUserService.getAllUsers()).thenReturn(users);

            // Act & Assert
            mockMvc.perform(get("/api/v1/admin/users"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(3)))
                    .andExpect(jsonPath("$[0].id", is(1)))
                    .andExpect(jsonPath("$[0].enabled", is(true)))
                    .andExpect(jsonPath("$[0].accountLocked", is(false)))
                    .andExpect(jsonPath("$[1].accountLocked", is(true)))
                    .andExpect(jsonPath("$[2].deletedAt", notNullValue()));

            verify(adminUserService).getAllUsers();
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("200 - retorna lista vacía cuando no hay usuarios")
        void shouldReturnEmptyListWhenNoUsers() throws Exception {
            // Arrange
            when(adminUserService.getAllUsers()).thenReturn(List.of());

            // Act & Assert
            mockMvc.perform(get("/api/v1/admin/users"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));

            verify(adminUserService).getAllUsers();
        }
    }


    @Nested
    @DisplayName("GET /api/v1/admin/users/{userId}")
    class GetUserById {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("200 - retorna detalle de usuario activo")
        void shouldReturnActiveUserDetail() throws Exception {
            // Arrange
            AdminUserResponseDTO response = buildActiveUser(1L);
            when(adminUserService.getUserById(1L)).thenReturn(response);

            // Act & Assert
            mockMvc.perform(get("/api/v1/admin/users/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.name", is("María García")))
                    .andExpect(jsonPath("$.email", is("maria.garcia@mail.com")))
                    .andExpect(jsonPath("$.role", is("CLIENT")))
                    .andExpect(jsonPath("$.enabled", is(true)))
                    .andExpect(jsonPath("$.accountLocked", is(false)))
                    .andExpect(jsonPath("$.deletedAt", nullValue()));

            verify(adminUserService).getUserById(1L);
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("200 - retorna detalle de usuario eliminado (soft delete)")
        void shouldReturnDeletedUserDetail() throws Exception {
            // Arrange
            AdminUserResponseDTO response = buildDeletedUser();
            when(adminUserService.getUserById(3L)).thenReturn(response);

            // Act & Assert
            mockMvc.perform(get("/api/v1/admin/users/3"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(3)))
                    .andExpect(jsonPath("$.deletedAt", notNullValue()))
                    .andExpect(jsonPath("$.enabled", is(false)));

            verify(adminUserService).getUserById(3L);
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("404 - usuario no encontrado")
        void shouldReturn404WhenUserNotFound() throws Exception {
            // Arrange
            when(adminUserService.getUserById(999L))
                    .thenThrow(new UserNotFoundException("Usuario no encontrado"));

            // Act & Assert
            mockMvc.perform(get("/api/v1/admin/users/999"))
                    .andExpect(status().isNotFound());

            verify(adminUserService).getUserById(999L);
        }
    }

    // ---------------------------------------------------------------
    // PATCH /api/v1/admin/users/{userId}/block
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("PATCH /api/v1/admin/users/{userId}/block")
    class BlockUser {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("200 - usuario bloqueado exitosamente")
        void shouldBlockUserSuccessfully() throws Exception {
            // Arrange
            AdminUserResponseDTO response = buildBlockedUser(1L);
            when(adminUserService.blockUser(1L)).thenReturn(response);

            // Act & Assert
            mockMvc.perform(patch("/api/v1/admin/users/1/block"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.accountLocked", is(true)))
                    .andExpect(jsonPath("$.enabled", is(false)));

            verify(adminUserService).blockUser(1L);
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("404 - usuario no encontrado al bloquear")
        void shouldReturn404WhenUserNotFound() throws Exception {
            // Arrange
            when(adminUserService.blockUser(999L))
                    .thenThrow(new UserNotFoundException("Usuario no encontrado"));

            // Act & Assert
            mockMvc.perform(patch("/api/v1/admin/users/999/block"))
                    .andExpect(status().isNotFound());

            verify(adminUserService).blockUser(999L);
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("409 - usuario ya estaba bloqueado")
        void shouldReturn409WhenUserAlreadyBlocked() throws Exception {
            // Arrange
            when(adminUserService.blockUser(2L))
                    .thenThrow(new UserAlreadyBlockedException(2L));

            // Act & Assert
            mockMvc.perform(patch("/api/v1/admin/users/2/block"))
                    .andExpect(status().isConflict());

            verify(adminUserService).blockUser(2L);
        }
    }

    // ---------------------------------------------------------------
    // PATCH /api/v1/admin/users/{userId}/unblock
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("PATCH /api/v1/admin/users/{userId}/unblock")
    class UnblockUser {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("200 - usuario desbloqueado exitosamente")
        void shouldUnblockUserSuccessfully() throws Exception {
            // Arrange
            AdminUserResponseDTO response = buildActiveUser(2L);
            when(adminUserService.unblockUser(2L)).thenReturn(response);

            // Act & Assert
            mockMvc.perform(patch("/api/v1/admin/users/2/unblock"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(2)))
                    .andExpect(jsonPath("$.accountLocked", is(false)))
                    .andExpect(jsonPath("$.enabled", is(true)));

            verify(adminUserService).unblockUser(2L);
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("404 - usuario no encontrado al desbloquear")
        void shouldReturn404WhenUserNotFound() throws Exception {
            // Arrange
            when(adminUserService.unblockUser(999L))
                    .thenThrow(new UserNotFoundException("Usuario no encontrado"));

            // Act & Assert
            mockMvc.perform(patch("/api/v1/admin/users/999/unblock"))
                    .andExpect(status().isNotFound());

            verify(adminUserService).unblockUser(999L);
        }
    }


    @Nested
    @DisplayName("DELETE /api/v1/admin/users/{userId}")
    class DeleteUser {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("204 - usuario eliminado lógicamente (soft delete)")
        void shouldSoftDeleteUserSuccessfully() throws Exception {
            // Arrange
            doNothing().when(adminUserService).deleteUser(1L);

            // Act & Assert
            mockMvc.perform(delete("/api/v1/admin/users/1"))
                    .andExpect(status().isNoContent());

            verify(adminUserService).deleteUser(1L);
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("404 - usuario no encontrado al eliminar")
        void shouldReturn404WhenUserNotFound() throws Exception {
            // Arrange
            doThrow(new UserNotFoundException("Usuario no encontrado"))
                    .when(adminUserService).deleteUser(999L);

            // Act & Assert
            mockMvc.perform(delete("/api/v1/admin/users/999"))
                    .andExpect(status().isNotFound());

            verify(adminUserService).deleteUser(999L);
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("409 - usuario ya fue eliminado previamente")
        void shouldReturn409WhenUserAlreadyDeleted() throws Exception {
            // Arrange
            doThrow(new UserAlreadyDeletedException(3L))
                    .when(adminUserService).deleteUser(3L);

            // Act & Assert
            mockMvc.perform(delete("/api/v1/admin/users/3"))
                    .andExpect(status().isConflict());

            verify(adminUserService).deleteUser(3L);
        }
    }

    // ---------------------------------------------------------------
    // TestConfiguration
    // ---------------------------------------------------------------

    @TestConfiguration
    static class TestConfig {

        @Bean
        @Primary
        IAdminUserService adminUserService() {
            return mock(IAdminUserService.class);
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