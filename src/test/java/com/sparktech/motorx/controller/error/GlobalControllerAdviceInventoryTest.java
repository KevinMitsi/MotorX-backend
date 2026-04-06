package com.sparktech.motorx.controller.error;

import com.sparktech.motorx.dto.error.ResponseErrorDTO;
import com.sparktech.motorx.exception.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GlobalControllerAdvice - Inventory Exceptions")
class GlobalControllerAdviceInventoryTest {

    private final GlobalControllerAdvice advice = new GlobalControllerAdvice();

    @Test
    @DisplayName("maneja SpareNotFoundException con 404")
    void shouldHandleSpareNotFound() {
        ResponseEntity<ResponseErrorDTO> response = advice.handleSpareNotFoundException(new SpareNotFoundException(10L));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().message()).contains("Repuesto no encontrado");
    }

    @Test
    @DisplayName("maneja DuplicateSpareCodeException con 409")
    void shouldHandleDuplicateSpareCode() {
        ResponseEntity<ResponseErrorDTO> response = advice.handleDuplicateSpareCodeException(new DuplicateSpareCodeException("dup"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().code()).isEqualTo(409);
    }

    @Test
    @DisplayName("maneja InsufficientStockException con 422")
    void shouldHandleInsufficientStock() {
        ResponseEntity<ResponseErrorDTO> response = advice.handleInsufficientStockException(new InsufficientStockException("sin stock"));

        assertThat(response.getStatusCode().value()).isEqualTo(422);
        assertThat(response.getBody().message()).contains("Stock insuficiente");
    }

    @Test
    @DisplayName("maneja InvalidWarehouseLocationException con 400")
    void shouldHandleInvalidWarehouseLocation() {
        ResponseEntity<ResponseErrorDTO> response = advice.handleInvalidWarehouseLocationException(new InvalidWarehouseLocationException("xx"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message()).contains("Ubicacion de bodega invalida");
    }

    @Test
    @DisplayName("maneja AppointmentNotInProcessException con 422")
    void shouldHandleAppointmentNotInProcess() {
        ResponseEntity<ResponseErrorDTO> response = advice.handleAppointmentNotInProcessException(new AppointmentNotInProcessException(1L));

        assertThat(response.getStatusCode().value()).isEqualTo(422);
        assertThat(response.getBody().message()).contains("no esta en proceso");
    }

    @Test
    @DisplayName("maneja InvalidVerificationCodeException con 400")
    void shouldHandleInvalidVerificationCode() {
        ResponseEntity<ResponseErrorDTO> response = advice.handleInvalidVerificationCodeException(new InvalidVerificationCodeException("bad"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message()).contains("Codigo de verificacion invalido");
    }

    @Test
    @DisplayName("maneja AppointmentNotEligibleForReceptionException con 422")
    void shouldHandleAppointmentNotEligibleForReception() {
        ResponseEntity<ResponseErrorDTO> response = advice.handleAppointmentNotEligibleForReceptionException(new AppointmentNotEligibleForReceptionException(1L));

        assertThat(response.getStatusCode().value()).isEqualTo(422);
        assertThat(response.getBody().message()).contains("no es elegible para recepcion");
    }
}

