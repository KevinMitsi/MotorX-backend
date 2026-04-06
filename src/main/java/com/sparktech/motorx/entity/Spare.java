package com.sparktech.motorx.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "spares", indexes = {
        @Index(name = "idx_spares_name", columnList = "name"),
        @Index(name = "idx_spares_supplier", columnList = "supplier"),
        @Index(name = "idx_spares_quantity", columnList = "quantity")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Spare {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "compatible_motorcycles", nullable = false, length = 500)
    private String compatibleMotorcycles;

    @Column(name = "sav_code", nullable = false, unique = true, length = 80)
    private String savCode;

    @Column(name = "spare_code", nullable = false, unique = true, length = 80)
    private String spareCode;

    @Column(name = "purchase_price_with_vat", nullable = false, precision = 12, scale = 2)
    private BigDecimal purchasePriceWithVat;

    @Column(name = "is_oil", nullable = false)
    @Builder.Default
    private Boolean isOil = false;

    @Column(nullable = false, length = 150)
    private String supplier;

    @Column(nullable = false)
    @Builder.Default
    private Integer quantity = 0;

    @Column(name = "warehouse_location", nullable = false, length = 20)
    private String warehouseLocation;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

