package com.sparktech.motorx.dto.procedure;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Procedure DTOs - Unit Tests")
class ProcedureDtoTest {

    @Test
    @DisplayName("records exponen correctamente sus propiedades")
    void recordsShouldExposeProperties() {
        CreateProcedureDTO createProcedureDTO = new CreateProcedureDTO("Lavado", "Desc", true);
        UpdateProcedureDTO updateProcedureDTO = new UpdateProcedureDTO("Lavado2", "Desc2", false);
        UpdateServiceProceduresDTO updateServiceProceduresDTO = new UpdateServiceProceduresDTO(List.of(1L, 2L));
        ProcedureResponseDTO responseDTO = new ProcedureResponseDTO(10L, "Lavado", "Desc", true, LocalDateTime.now(), LocalDateTime.now());

        assertThat(createProcedureDTO.name()).isEqualTo("Lavado");
        assertThat(updateProcedureDTO.active()).isFalse();
        assertThat(updateServiceProceduresDTO.procedureIds()).hasSize(2);
        assertThat(responseDTO.id()).isEqualTo(10L);
    }
}

