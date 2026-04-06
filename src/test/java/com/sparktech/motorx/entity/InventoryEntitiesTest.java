package com.sparktech.motorx.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Inventory Entities - Unit Tests")
class InventoryEntitiesTest {

    @Test
    @DisplayName("Spare prePersist asigna createdAt y updatedAt")
    void sparePrePersistShouldSetTimestamps() throws Exception {
        Spare spare = new Spare();

        Method prePersist = Spare.class.getDeclaredMethod("prePersist");
        prePersist.setAccessible(true);
        prePersist.invoke(spare);

        assertThat(spare.getCreatedAt()).isNotNull();
        assertThat(spare.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Spare preUpdate actualiza updatedAt")
    void sparePreUpdateShouldSetUpdatedAt() throws Exception {
        Spare spare = new Spare();

        Method preUpdate = Spare.class.getDeclaredMethod("preUpdate");
        preUpdate.setAccessible(true);
        preUpdate.invoke(spare);

        assertThat(spare.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("PurchaseTransaction prePersist asigna transactionDate cuando está null")
    void purchaseTransactionPrePersistShouldSetDateWhenNull() throws Exception {
        PurchaseTransaction tx = new PurchaseTransaction();

        Method prePersist = PurchaseTransaction.class.getDeclaredMethod("prePersist");
        prePersist.setAccessible(true);
        prePersist.invoke(tx);

        assertThat(tx.getTransactionDate()).isNotNull();
    }

    @Test
    @DisplayName("PurchaseTransaction prePersist respeta transactionDate existente")
    void purchaseTransactionPrePersistShouldKeepDateWhenAlreadySet() throws Exception {
        LocalDateTime fixed = LocalDateTime.of(2026, 4, 1, 10, 0);
        PurchaseTransaction tx = new PurchaseTransaction();
        tx.setTransactionDate(fixed);

        Method prePersist = PurchaseTransaction.class.getDeclaredMethod("prePersist");
        prePersist.setAccessible(true);
        prePersist.invoke(tx);

        assertThat(tx.getTransactionDate()).isEqualTo(fixed);
    }

    @Test
    @DisplayName("SaleTransaction prePersist asigna transactionDate cuando está null")
    void saleTransactionPrePersistShouldSetDateWhenNull() throws Exception {
        SaleTransaction tx = new SaleTransaction();

        Method prePersist = SaleTransaction.class.getDeclaredMethod("prePersist");
        prePersist.setAccessible(true);
        prePersist.invoke(tx);

        assertThat(tx.getTransactionDate()).isNotNull();
    }

    @Test
    @DisplayName("SaleTransaction prePersist respeta transactionDate existente")
    void saleTransactionPrePersistShouldKeepDateWhenAlreadySet() throws Exception {
        LocalDateTime fixed = LocalDateTime.of(2026, 4, 1, 11, 0);
        SaleTransaction tx = new SaleTransaction();
        tx.setTransactionDate(fixed);

        Method prePersist = SaleTransaction.class.getDeclaredMethod("prePersist");
        prePersist.setAccessible(true);
        prePersist.invoke(tx);

        assertThat(tx.getTransactionDate()).isEqualTo(fixed);
    }

    @Test
    @DisplayName("AppointmentEntity prePersist valida horarios y setea createdAt")
    void appointmentPrePersistShouldValidateAndSetCreatedAt() throws Exception {
        AppointmentEntity appointment = validAppointment();

        Method prePersist = AppointmentEntity.class.getDeclaredMethod("prePersist");
        prePersist.setAccessible(true);
        prePersist.invoke(appointment);

        assertThat(appointment.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("AppointmentEntity preUpdate setea updatedAt")
    void appointmentPreUpdateShouldSetUpdatedAt() throws Exception {
        AppointmentEntity appointment = validAppointment();

        Method preUpdate = AppointmentEntity.class.getDeclaredMethod("preUpdate");
        preUpdate.setAccessible(true);
        preUpdate.invoke(appointment);

        assertThat(appointment.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("AppointmentEntity valida que endTime sea mayor que startTime")
    void appointmentShouldFailWhenEndTimeBeforeOrEqualStartTime() throws Exception {
        AppointmentEntity appointment = validAppointment();
        appointment.setStartTime(LocalTime.of(9, 0));
        appointment.setEndTime(LocalTime.of(9, 0));

        Method prePersist = AppointmentEntity.class.getDeclaredMethod("prePersist");
        prePersist.setAccessible(true);

        assertThatThrownBy(() -> prePersist.invoke(appointment))
                .hasRootCauseInstanceOf(IllegalArgumentException.class)
                .rootCause()
                .hasMessageContaining("End time must be after start time");
    }

    @Test
    @DisplayName("EmployeePosition incluye WAREHOUSE_WORKER")
    void employeePositionShouldContainWarehouseWorker() {
        assertThat(EmployeePosition.valueOf("WAREHOUSE_WORKER")).isEqualTo(EmployeePosition.WAREHOUSE_WORKER);
    }

    @Test
    @DisplayName("AppointmentStatus incluye AWAITING_CONFIRMATION")
    void appointmentStatusShouldContainAwaitingConfirmation() {
        assertThat(AppointmentStatus.valueOf("AWAITING_CONFIRMATION")).isEqualTo(AppointmentStatus.AWAITING_CONFIRMATION);
    }

    private AppointmentEntity validAppointment() {
        AppointmentEntity appointment = new AppointmentEntity();
        appointment.setAppointmentType(AppointmentType.MAINTENANCE);
        appointment.setAppointmentDate(LocalDate.now().plusDays(1));
        appointment.setStartTime(LocalTime.of(9, 0));
        appointment.setEndTime(LocalTime.of(10, 0));
        appointment.setStatus(AppointmentStatus.SCHEDULED);
        appointment.setCurrentMileage(1200);

        VehicleEntity vehicle = new VehicleEntity();
        vehicle.setId(1L);
        appointment.setVehicle(vehicle);
        return appointment;
    }
}

