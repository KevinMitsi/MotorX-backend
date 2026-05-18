package com.sparktech.motorx.dto.service;

import com.sparktech.motorx.dto.procedure.ProcedureResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Service DTOs - Unit Tests")
class ServiceDtoTest {

    @Test
    @DisplayName("records exponen correctamente sus propiedades")
    void recordsShouldExposeProperties() {
        CreateServiceDTO createServiceDTO = new CreateServiceDTO(
                "Mantenimiento",
                "Desc",
                60,
                new BigDecimal("120.00"),
                true,
                List.of(1L, 2L)
        );

        UpdateServiceDTO updateServiceDTO = new UpdateServiceDTO(
                "Mantenimiento",
                "Desc",
                60,
                new BigDecimal("120.00"),
                false
        );

        ServiceResponseDTO responseDTO = new ServiceResponseDTO(
                10L,
                "Mantenimiento",
                "Desc",
                60,
                new BigDecimal("120.00"),
                true,
                List.of(new ProcedureResponseDTO(1L, "Proc", "Desc", true, null, null)),
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        assertThat(createServiceDTO.name()).isEqualTo("Mantenimiento");
        assertThat(createServiceDTO.procedureIds()).hasSize(2);
        assertThat(updateServiceDTO.active()).isFalse();
        assertThat(responseDTO.baseProcedures()).hasSize(1);
    }
}

