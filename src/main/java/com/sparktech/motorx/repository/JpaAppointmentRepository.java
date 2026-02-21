package com.sparktech.motorx.repository;


import com.sparktech.motorx.entity.AppointmentStatus;
import com.sparktech.motorx.entity.AppointmentEntity;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface JpaAppointmentRepository extends JpaRepository<@NotNull AppointmentEntity, @NotNull Long> {

    // --- Historial de citas por vehículo (Proceso 2 - historial de citas) ---
    List<AppointmentEntity> findByVehicleIdOrderByAppointmentDateDesc(Long vehicleId);

    // --- Historial de citas por cliente a través del vehículo (Proceso 1 - historial de servicios) ---
    @Query("""
            SELECT a FROM AppointmentEntity a
            WHERE a.vehicle.owner.id = :ownerId
            ORDER BY a.appointmentDate DESC, a.startTime DESC
            """)
    List<AppointmentEntity> findByClientIdOrderByDateDesc(@Param("ownerId") Long ownerId);

    // --- Citas por fecha (vista de agenda diaria) ---
    List<AppointmentEntity> findByAppointmentDateOrderByStartTime(LocalDate date);

    // --- Citas por fecha y estado ---
    List<AppointmentEntity> findByAppointmentDateAndStatus(LocalDate date, AppointmentStatus status);

    // --- Métrica: Tasa de Conflictos de Horario Detectados ---
    // Verifica si existe una cita en ese rango horario para ese servicio/día
    // (El sistema debe llamar esto antes de crear una cita nueva)
    @Query("""
            SELECT COUNT(a) > 0 FROM AppointmentEntity a
            WHERE a.appointmentDate = :date
              AND a.status NOT IN ('CANCELLED', 'REJECTED', 'NO_SHOW')
              AND (
                  (a.startTime < :endTime AND a.endTime > :startTime)
              )
            """)
    boolean existsConflict(
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime
    );

    // --- Métrica: Porcentaje de Ocupación del Taller ---
    // Suma de minutos reservados en un rango de fechas
    @Query("""
            SELECT COALESCE(SUM(
                FUNCTION('TIMESTAMPDIFF', MINUTE, a.startTime, a.endTime)
            ), 0)
            FROM AppointmentEntity a
            WHERE a.appointmentDate BETWEEN :start AND :end
              AND a.status NOT IN ('CANCELLED', 'REJECTED', 'NO_SHOW')
            """)
    long sumReservedMinutesBetween(
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    // --- Métrica: Tasa de cancelación de citas ---
    long countByStatus(AppointmentStatus status);

    @Query("""
            SELECT COUNT(a) FROM AppointmentEntity a
            WHERE a.status = :status
              AND a.appointmentDate BETWEEN :start AND :end
            """)
    long countByStatusBetween(
            @Param("status") AppointmentStatus status,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    @Query("""
            SELECT COUNT(a) FROM AppointmentEntity a
            WHERE a.appointmentDate BETWEEN :start AND :end
            """)
    long countAllBetween(
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    // --- Métrica: Número de citas por día ---
    @Query("""
            SELECT COUNT(a) FROM AppointmentEntity a
            WHERE a.appointmentDate = :date
              AND a.status NOT IN ('CANCELLED', 'REJECTED')
            """)
    long countActiveAppointmentsByDate(@Param("date") LocalDate date);

    // --- Métrica: Tiempo medio de creación de cita ---
    // Citas creadas en un rango para calcular (appointmentDate - createdAt)
    @Query("""
            SELECT a FROM AppointmentEntity a
            WHERE a.createdAt BETWEEN :start AND :end
            ORDER BY a.createdAt ASC
            """)
    List<AppointmentEntity> findAppointmentsCreatedBetween(
            @Param("start") java.time.LocalDateTime start,
            @Param("end") java.time.LocalDateTime end
    );

    // --- Citas activas de un vehículo (para evitar doble agendamiento) ---
    @Query("""
            SELECT a FROM AppointmentEntity a
            WHERE a.vehicle.id = :vehicleId
              AND a.status IN ('SCHEDULED', 'IN_PROGRESS')
            """)
    List<AppointmentEntity> findActiveAppointmentsByVehicle(@Param("vehicleId") Long vehicleId);

    // --- Citas por rango de fechas (vista de calendario) ---
    @Query("""
            SELECT a FROM AppointmentEntity a
            WHERE a.appointmentDate BETWEEN :start AND :end
            ORDER BY a.appointmentDate ASC, a.startTime ASC
            """)
    List<AppointmentEntity> findByDateRange(
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );
}