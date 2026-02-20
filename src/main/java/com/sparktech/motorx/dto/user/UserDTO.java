package com.sparktech.motorx.dto.user;

import com.sparktech.motorx.entity.Role;

import java.time.LocalDateTime;

public record UserDTO(
        Long id,
        String name,
        String dni,
        String email,
        String password,
        String phone,
        LocalDateTime createdAt,
        Role role,
        boolean enabled,
        boolean accountLocked,
        LocalDateTime updatedAt
) {
}
