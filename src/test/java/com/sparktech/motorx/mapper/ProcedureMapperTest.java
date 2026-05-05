package com.sparktech.motorx.mapper;

import com.sparktech.motorx.dto.procedure.CreateProcedureDTO;
import com.sparktech.motorx.dto.procedure.ProcedureResponseDTO;
import com.sparktech.motorx.dto.procedure.UpdateProcedureDTO;
import com.sparktech.motorx.entity.ProcedureEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ProcedureMapper - Unit Tests")
class ProcedureMapperTest {

    private final ProcedureMapper mapper = new ProcedureMapper();

    @Test
    @DisplayName("toEntity asigna active true cuando viene null")
    void toEntityShouldDefaultActiveWhenNull() {
        CreateProcedureDTO dto = new CreateProcedureDTO("Lavado", "Desc", null);

        ProcedureEntity entity = mapper.toEntity(dto);

        assertThat(entity.getName()).isEqualTo("Lavado");
        assertThat(entity.getActive()).isTrue();
    }

    @Test
    @DisplayName("updateEntity respeta active cuando es null")
    void updateEntityShouldKeepActiveWhenNull() {
        ProcedureEntity entity = new ProcedureEntity();
        entity.setActive(true);
        UpdateProcedureDTO dto = new UpdateProcedureDTO("Lavado", "Desc", null);

        mapper.updateEntity(entity, dto);

        assertThat(entity.getName()).isEqualTo("Lavado");
        assertThat(entity.getActive()).isTrue();
    }

    @Test
    @DisplayName("updateEntity actualiza active cuando se envia")
    void updateEntityShouldUpdateActiveWhenProvided() {
        ProcedureEntity entity = new ProcedureEntity();
        entity.setActive(true);
        UpdateProcedureDTO dto = new UpdateProcedureDTO("Lavado", "Desc", false);

        mapper.updateEntity(entity, dto);

        assertThat(entity.getActive()).isFalse();
    }

    @Test
    @DisplayName("toResponseDTO mapea campos principales")
    void toResponseDTOShouldMapFields() {
        ProcedureEntity entity = new ProcedureEntity();
        entity.setId(5L);
        entity.setName("Lavado");
        entity.setDescription("Desc");
        entity.setActive(true);

        ProcedureResponseDTO dto = mapper.toResponseDTO(entity);

        assertThat(dto.id()).isEqualTo(5L);
        assertThat(dto.name()).isEqualTo("Lavado");
        assertThat(dto.description()).isEqualTo("Desc");
        assertThat(dto.active()).isTrue();
    }
}

