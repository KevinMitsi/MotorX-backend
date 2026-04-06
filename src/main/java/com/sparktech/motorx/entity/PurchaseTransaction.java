package com.sparktech.motorx.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "purchase_transactions", indexes = {
        @Index(name = "idx_purchase_tx_date", columnList = "transaction_date"),
        @Index(name = "idx_purchase_tx_supplier", columnList = "supplier")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String supplier;

    @Column(name = "transaction_date", nullable = false)
    private LocalDateTime transactionDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private UserEntity createdBy;

    @OneToMany(mappedBy = "purchaseTransaction", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PurchaseTransactionItem> items = new ArrayList<>();

    @PrePersist
    void prePersist() {
        if (transactionDate == null) {
            this.transactionDate = LocalDateTime.now();
        }
    }
}

