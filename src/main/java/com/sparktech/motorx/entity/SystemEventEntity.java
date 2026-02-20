package com.sparktech.motorx.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "system_events",
         indexes = {
              @Index(name = "idx_event_type", columnList = "eventType"),
              @Index(name = "idx_event_date", columnList = "eventDate")
         }
)
public class SystemEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 100)
    private EventType eventType;

    @Column(nullable = false)
    private LocalDateTime eventDate;

    @Column(length = 500)
    private String metadata;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false)
    private EventSeverity severity;
}