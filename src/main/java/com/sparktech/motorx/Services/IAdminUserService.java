package com.sparktech.motorx.Services;

import com.sparktech.motorx.dto.user.AdminUserResponseDTO;

import java.util.List;

/**
 * Servicio de acciones administrativas sobre usuarios (clientes).
 * El administrador puede listar, consultar, bloquear y eliminar (soft delete) usuarios.
 */
public interface IAdminUserService {

    /**
     * Lista todos los usuarios registrados en el sistema (incluyendo eliminados).
     */
    List<AdminUserResponseDTO> getAllUsers();

    /**
     * Obtiene la información detallada de un usuario por su ID.
     */
    AdminUserResponseDTO getUserById(Long userId);

    /**
     * Bloquea la cuenta de un usuario impidiendo que pueda iniciar sesión.
     * Operación idempotente: lanza {@link com.sparktech.motorx.exception.UserAlreadyBlockedException}
     * si el usuario ya estaba bloqueado.
     *
     * @throws com.sparktech.motorx.exception.UserNotFoundException      si el usuario no existe.
     * @throws com.sparktech.motorx.exception.UserAlreadyBlockedException si ya está bloqueado.
     */
    AdminUserResponseDTO blockUser(Long userId);

    /**
     * Desbloquea la cuenta de un usuario permitiéndole volver a iniciar sesión.
     */
    AdminUserResponseDTO unblockUser(Long userId);

    /**
     * Elimina lógicamente un usuario (soft delete): establece {@code deletedAt} con la fecha actual,
     * desactiva la cuenta y la bloquea para evitar accesos posteriores.
     * No elimina el registro de la base de datos para preservar el historial.
     *
     * @throws com.sparktech.motorx.exception.UserNotFoundException      si el usuario no existe.
     * @throws com.sparktech.motorx.exception.UserAlreadyDeletedException si ya fue eliminado.
     */
    void deleteUser(Long userId);
}
