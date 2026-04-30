package com.sparktech.motorx.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "order_procedures")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderProcedureEntity {

    @EmbeddedId
    private OrderProcedureId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("orderId")
    @JoinColumn(name = "order_id", nullable = false)
    private OrderServiceEntity order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("procedureId")
    @JoinColumn(name = "procedure_id", nullable = false)
    private ProcedureEntity procedure;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal cost;
}

