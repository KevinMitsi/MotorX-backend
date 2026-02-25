package com.sparktech.motorx.mapper;

import com.sparktech.motorx.dto.employee.EmployeeResponseDTO;
import com.sparktech.motorx.entity.EmployeeEntity;
import org.springframework.stereotype.Component;

@Component
public class EmployeeMapper {

    public EmployeeResponseDTO toResponseDTO(EmployeeEntity entity) {
        return new EmployeeResponseDTO(
                entity.getId(),
                entity.getPosition(),
                entity.getState(),
                entity.getHireDate(),
                entity.getUser().getId(),
                entity.getUser().getName(),
                entity.getUser().getEmail(),
                entity.getUser().getDni(),
                entity.getUser().getPhone(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}

