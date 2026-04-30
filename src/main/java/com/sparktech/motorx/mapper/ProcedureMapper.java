package com.sparktech.motorx.mapper;

import com.sparktech.motorx.dto.procedure.CreateProcedureDTO;
import com.sparktech.motorx.dto.procedure.ProcedureResponseDTO;
import com.sparktech.motorx.dto.procedure.UpdateProcedureDTO;
import com.sparktech.motorx.entity.ProcedureEntity;
import org.springframework.stereotype.Component;

@Component
public class ProcedureMapper {

    public ProcedureEntity toEntity(CreateProcedureDTO dto) {
        ProcedureEntity entity = new ProcedureEntity();
        entity.setName(dto.name());
        entity.setDescription(dto.description());
        entity.setActive(dto.active() != null ? dto.active() : Boolean.TRUE);
        return entity;
    }

    public void updateEntity(ProcedureEntity entity, UpdateProcedureDTO dto) {
        entity.setName(dto.name());
        entity.setDescription(dto.description());
        if (dto.active() != null) {
            entity.setActive(dto.active());
        }
    }

    public ProcedureResponseDTO toResponseDTO(ProcedureEntity entity) {
        return new ProcedureResponseDTO(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}

