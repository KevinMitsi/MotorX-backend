package com.sparktech.motorx.dto.auth;



import com.sparktech.motorx.entity.Role;
import lombok.Builder;


@Builder
public record AuthResponseDTO(
        String token,
        String type,
        Long userId,
        String email,
        String name,
        Role role

) {
    public AuthResponseDTO(String token, Long userId, String email, String name, Role roles) {
        this(token, "Bearer", userId, email, name, roles);
    }
}
