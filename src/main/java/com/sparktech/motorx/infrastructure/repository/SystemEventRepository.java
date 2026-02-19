package com.sparktech.motorx.infrastructure.repository;

import com.sparktech.motorx.domain.enums.EventSeverity;
import com.sparktech.motorx.domain.enums.EventType;
import com.sparktech.motorx.infrastructure.entity.SystemEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SystemEventRepository extends JpaRepository<SystemEventEntity, Long> {

    // --- Métrica: Tasa de Registros Fallidos ---
    // Intentos totales vs fallidos
    long countByEventType(EventType eventType);

    @Query("""
            SELECT COUNT(e) FROM SystemEventEntity e
            WHERE e.eventType = :eventType
              AND e.eventDate BETWEEN :start AND :end
            """)
    long countByEventTypeBetween(
            @Param("eventType") EventType eventType,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    // --- Métrica: Tasa de Conflictos de Horario Detectados ---
    // APPOINTMENT_CONFLICT / total APPOINTMENT_CREATED + APPOINTMENT_CONFLICT
    @Query("""
            SELECT COUNT(e) FROM SystemEventEntity e
            WHERE e.eventType IN ('APPOINTMENT_CONFLICT', 'APPOINTMENT_CREATED')
              AND e.eventDate BETWEEN :start AND :end
            """)
    long countAppointmentAttemptsBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    // --- Eventos por usuario (trazabilidad de acciones) ---
    @Query("""
            SELECT e FROM SystemEventEntity e
            WHERE e.user.id = :userId
            ORDER BY e.eventDate DESC
            """)
    List<SystemEventEntity> findByUserIdOrderByDateDesc(@Param("userId") Long userId);

    // --- Eventos por severidad (monitoreo de errores) ---
    @Query("""
            SELECT e FROM SystemEventEntity e
            WHERE e.severity = :severity
              AND e.eventDate BETWEEN :start AND :end
            ORDER BY e.eventDate DESC
            """)
    List<SystemEventEntity> findBySeverityBetween(
            @Param("severity") EventSeverity severity,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    // --- Eventos recientes por tipo (debug / monitoreo) ---
    @Query("""
            SELECT e FROM SystemEventEntity e
            WHERE e.eventType = :eventType
            ORDER BY e.eventDate DESC
            """)
    List<SystemEventEntity> findRecentByEventType(@Param("eventType") EventType eventType);

    // --- Detección de abuso: múltiples intentos fallidos de login por usuario ---
    @Query("""
            SELECT COUNT(e) FROM SystemEventEntity e
            WHERE e.eventType = 'LOGIN_FAILED'
              AND e.user.id = :userId
              AND e.eventDate >= :since
            """)
    long countFailedLoginAttemptsSince(
            @Param("userId") Long userId,
            @Param("since") LocalDateTime since
    );
}