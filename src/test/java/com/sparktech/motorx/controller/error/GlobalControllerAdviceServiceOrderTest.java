package com.sparktech.motorx.controller.error;

import com.sparktech.motorx.dto.error.ResponseErrorDTO;
import com.sparktech.motorx.exception.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GlobalControllerAdvice - Service Order Exceptions")
class GlobalControllerAdviceServiceOrderTest {

    private final GlobalControllerAdvice advice = new GlobalControllerAdvice();

    @Test
    @DisplayName("maneja ServiceNotFoundException con 404")
    void shouldHandleServiceNotFound() {
        ResponseEntity<ResponseErrorDTO> response = advice.handleServiceNotFoundException(new ServiceNotFoundException(1L));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().message()).contains("Servicio no encontrado");
    }

    @Test
    @DisplayName("maneja ProcedureNotFoundException con 404")
    void shouldHandleProcedureNotFound() {
        ResponseEntity<ResponseErrorDTO> response = advice.handleProcedureNotFoundException(new ProcedureNotFoundException(2L));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().message()).contains("Procedimiento no encontrado");
    }

    @Test
    @DisplayName("maneja OrderServiceNotFoundException con 404")
    void shouldHandleOrderServiceNotFound() {
        ResponseEntity<ResponseErrorDTO> response = advice.handleOrderServiceNotFoundException(new OrderServiceNotFoundException(3L));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().message()).contains("Orden de servicio no encontrada");
    }

    @Test
    @DisplayName("maneja DuplicateProcedureNameException con 409")
    void shouldHandleDuplicateProcedureName() {
        ResponseEntity<ResponseErrorDTO> response = advice.handleDuplicateProcedureNameException(new DuplicateProcedureNameException("Dup"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().message()).contains("Nombre de procedimiento duplicado");
    }

    @Test
    @DisplayName("maneja TechnicianNotAssignedException con 403")
    void shouldHandleTechnicianNotAssigned() {
        ResponseEntity<ResponseErrorDTO> response = advice.handleTechnicianNotAssignedException(new TechnicianNotAssignedException(8L));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().message()).contains("tecnico no esta asignado");
    }
}

