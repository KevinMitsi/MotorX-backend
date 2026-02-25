package com.sparktech.motorx.dto.user;

import com.sparktech.motorx.entity.Role;

import java.time.LocalDateTime;

/**
 * DTO de respuesta para las operaciones administrativas sobre usuarios.
 * No expone el campo password por seguridad.
 */
public record AdminUserResponseDTO(
        Long id,
        String name,
        String dni,
        String email,
        String phone,
        Role role,
        boolean enabled,
        boolean accountLocked,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime deletedAt
) {
}

