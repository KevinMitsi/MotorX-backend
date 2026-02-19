package com.sparktech.motorx.domain.model;

import com.sparktech.motorx.domain.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private Long id;
    private String name;
    private String dni;
    private String email;
    private String password;
    private LocalDateTime createdAt;
    private Role role;
    private boolean enabled;
    private boolean accountLocked;
    private LocalDateTime updatedAt;
}

