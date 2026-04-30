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
public class OrderSpareId implements Serializable {

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "spare_id")
    private Long spareId;
}

