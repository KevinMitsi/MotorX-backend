package com.sparktech.motorx.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class OrderProcedureId implements Serializable {

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "procedure_id")
    private Long procedureId;
}

