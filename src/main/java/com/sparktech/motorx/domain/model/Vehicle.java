package com.sparktech.motorx.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class Vehicle {
    private Long id;
    private String brand;
    private String model;
    private String licensePlate;
    private Integer cylinderCapacity;
    private User owner;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;


}

