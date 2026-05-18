package com.sparktech.motorx.mapper;

import com.sparktech.motorx.dto.service.CreateServiceDTO;
import com.sparktech.motorx.dto.service.ServiceResponseDTO;
import com.sparktech.motorx.dto.service.UpdateServiceDTO;
import com.sparktech.motorx.entity.ServiceEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ServiceMapper {

    private final ProcedureMapper procedureMapper;

    public ServiceEntity toEntity(CreateServiceDTO dto) {
        ServiceEntity entity = new ServiceEntity();
        entity.setName(dto.name());
        entity.setDescription(dto.description());
        entity.setEstimatedDurationMinutes(dto.estimatedDurationMinutes());
        entity.setBasePrice(dto.basePrice());
        entity.setActive(dto.active() != null ? dto.active() : Boolean.TRUE);
        return entity;
    }

    public void updateEntity(ServiceEntity entity, UpdateServiceDTO dto) {
        entity.setName(dto.name());
        entity.setDescription(dto.description());
        entity.setEstimatedDurationMinutes(dto.estimatedDurationMinutes());
        entity.setBasePrice(dto.basePrice());
        if (dto.active() != null) {
            entity.setActive(dto.active());
        }
    }

    public ServiceResponseDTO toResponseDTO(ServiceEntity entity) {
        List<com.sparktech.motorx.dto.procedure.ProcedureResponseDTO> procedures = entity.getBaseProcedures().stream()
                .map(procedureMapper::toResponseDTO)
                .toList();

        return new ServiceResponseDTO(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getEstimatedDurationMinutes(),
                entity.getBasePrice(),
                entity.getActive(),
                procedures,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}

