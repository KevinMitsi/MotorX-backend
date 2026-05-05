package com.sparktech.motorx.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "service_orders",
        indexes = {
                @Index(name = "idx_order_appointment", columnList = "appointment_id"),
                @Index(name = "idx_order_employee", columnList = "employee_id"),
                @Index(name = "idx_order_status", columnList = "status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderServiceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "appointment_id", nullable = false, unique = true)
    private AppointmentEntity appointment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private EmployeeEntity employee;

    @Column(nullable = false)
    private LocalDateTime startDate;

    private LocalDateTime endDate;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalServices;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalSpareParts;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalToPay;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus status;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderProcedureEntity> procedures = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderSpareEntity> spares = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    private void prePersist() {
        validateDates();
        if (totalServices == null) {
            totalServices = BigDecimal.ZERO;
        }
        if (totalSpareParts == null) {
            totalSpareParts = BigDecimal.ZERO;
        }
        if (totalToPay == null) {
            totalToPay = BigDecimal.ZERO;
        }
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    private void preUpdate() {
        validateDates();
        this.updatedAt = LocalDateTime.now();
    }

    private void validateDates() {
        if (endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date cannot be before start date");
        }
    }
}