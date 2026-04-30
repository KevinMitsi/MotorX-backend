package com.sparktech.motorx.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "order_spares")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderSpareEntity {

    @EmbeddedId
    private OrderSpareId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("orderId")
    @JoinColumn(name = "order_id", nullable = false)
    private OrderServiceEntity order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("spareId")
    @JoinColumn(name = "spare_id", nullable = false)
    private Spare spare;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;
}

