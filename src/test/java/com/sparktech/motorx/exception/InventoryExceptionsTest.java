package com.sparktech.motorx.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Inventory Exceptions - Unit Tests")
class InventoryExceptionsTest {

    @Test
    @DisplayName("SpareNotFoundException arma mensaje con id")
    void spareNotFoundShouldBuildMessage() {
        assertThat(new SpareNotFoundException(9L).getMessage()).contains("9");
    }

    @Test
    @DisplayName("DuplicateSpareCodeException conserva mensaje")
    void duplicateSpareCodeShouldKeepMessage() {
        assertThat(new DuplicateSpareCodeException("dup").getMessage()).isEqualTo("dup");
    }

    @Test
    @DisplayName("InsufficientStockException conserva mensaje")
    void insufficientStockShouldKeepMessage() {
        assertThat(new InsufficientStockException("no stock").getMessage()).isEqualTo("no stock");
    }

    @Test
    @DisplayName("InvalidWarehouseLocationException arma mensaje")
    void invalidWarehouseLocationShouldBuildMessage() {
        assertThat(new InvalidWarehouseLocationException("xx").getMessage()).contains("xx");
    }

    @Test
    @DisplayName("AppointmentNotInProcessException arma mensaje")
    void appointmentNotInProcessShouldBuildMessage() {
        assertThat(new AppointmentNotInProcessException(15L).getMessage()).contains("15");
    }

    @Test
    @DisplayName("InvalidVerificationCodeException conserva mensaje")
    void invalidVerificationCodeShouldKeepMessage() {
        assertThat(new InvalidVerificationCodeException("bad").getMessage()).isEqualTo("bad");
    }

    @Test
    @DisplayName("AppointmentNotEligibleForReceptionException arma mensaje")
    void appointmentNotEligibleShouldBuildMessage() {
        assertThat(new AppointmentNotEligibleForReceptionException(3L).getMessage()).contains("3");
    }
}

