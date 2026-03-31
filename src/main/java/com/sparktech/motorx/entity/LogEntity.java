package com.sparktech.motorx.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "logs",
        indexes = {
                @Index(name = "idx_logs_created_at", columnList = "createdAt"),
                @Index(name = "idx_logs_service_action", columnList = "serviceName,actionType"),
                @Index(name = "idx_logs_actor_email", columnList = "actorEmail")
        })
public class LogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private LogServiceName serviceName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 60)
    private LogActionType actionType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LogResult result;

    @Column(length = 150)
    private String actorEmail;

    @Column
    private Long actorUserId;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}

