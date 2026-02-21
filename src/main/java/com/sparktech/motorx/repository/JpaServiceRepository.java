package com.sparktech.motorx.repository;

import com.sparktech.motorx.entity.ServiceEntity;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JpaServiceRepository extends JpaRepository<@NotNull ServiceEntity, @NotNull Long> {

    // --- Búsqueda básica ---
    Optional<ServiceEntity> findByName(String name);

    boolean existsByName(String name);

    // --- Solo servicios activos (para el cliente al agendar cita) ---
    List<ServiceEntity> findByActiveTrue();

    // --- Búsqueda por nombre parcial (soporte para selector en formulario de cita) ---
    @Query("""
            SELECT s FROM ServiceEntity s
            WHERE s.active = true
              AND LOWER(s.name) LIKE LOWER(CONCAT('%', :name, '%'))
            ORDER BY s.name ASC
            """)
    List<ServiceEntity> findActiveByNameContaining(@Param("name") String name);

    // --- Servicios ordenados por duración (útil para gestión de agenda) ---
    List<ServiceEntity> findByActiveTrueOrderByEstimatedDurationMinutesAsc();

    // --- Servicios más agendados (KPI futuro - soporte desde ya) ---
    @Query("""
            SELECT s, COUNT(a) as total
            FROM ServiceEntity s
            LEFT JOIN AppointmentEntity a ON a.service = s
            WHERE s.active = true
            GROUP BY s
            ORDER BY total DESC
            """)
    List<Object[]> findMostRequestedServices();
}