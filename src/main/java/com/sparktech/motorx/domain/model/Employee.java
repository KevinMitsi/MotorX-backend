package com.sparktech.motorx.domain.model;

import com.sparktech.motorx.domain.enums.EmployeeState;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Employee {
    private Long id;
    private LocalDateTime hireDate;
    private String position;
    private EmployeeState state;
    private User user;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

