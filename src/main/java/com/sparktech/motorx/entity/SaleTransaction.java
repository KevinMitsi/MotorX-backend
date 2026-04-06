package com.sparktech.motorx.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sale_transactions", indexes = {
        @Index(name = "idx_sale_tx_date", columnList = "transaction_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaleTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_date", nullable = false)
    private LocalDateTime transactionDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id")
    private AppointmentEntity appointment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private UserEntity createdBy;

    @OneToMany(mappedBy = "saleTransaction", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SaleTransactionItem> items = new ArrayList<>();

    @PrePersist
    void prePersist() {
        if (transactionDate == null) {
            this.transactionDate = LocalDateTime.now();
        }
    }
}

