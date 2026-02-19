package com.sparktech.motorx.domain.model;

import lombok.*;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PasswordResetToken {
    private Long id;
    private String tokenHash;
    private LocalDateTime expiresAt;
    private Boolean used;
    private LocalDateTime createdAt;
    private LocalDateTime usedAt;
    private User user;

}
