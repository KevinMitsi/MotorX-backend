package com.sparktech.motorx.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "sale_transaction_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaleTransactionItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sale_transaction_id", nullable = false)
    private SaleTransaction saleTransaction;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "spare_id", nullable = false)
    private Spare spare;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "sale_price_at_moment", nullable = false, precision = 12, scale = 2)
    private BigDecimal salePriceAtMoment;
}

