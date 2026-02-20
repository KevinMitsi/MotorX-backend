package com.sparktech.motorx.repository;

import com.sparktech.motorx.entity.EmployeeState;
import com.sparktech.motorx.entity.EmployeeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JpaEmployeeRepository extends JpaRepository<EmployeeEntity, Long> {

    // --- Búsqueda por usuario vinculado ---
    Optional<EmployeeEntity> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    // --- Empleados por estado (disponibilidad para asignar citas) ---
    List<EmployeeEntity> findByState(EmployeeState state);

    // --- Empleados disponibles con sus datos de usuario (para asignación en órdenes) ---
    @Query("""
            SELECT e FROM EmployeeEntity e
            JOIN FETCH e.user u
            WHERE e.state = 'AVAILABLE'
            ORDER BY u.name ASC
            """)
    List<EmployeeEntity> findAvailableEmployeesWithUser();

    // --- Búsqueda por posición/cargo ---
    List<EmployeeEntity> findByPositionIgnoreCase(String position);

    // --- Conteo por estado (KPI administrativo) ---
    long countByState(EmployeeState state);

    // --- Empleado por email de usuario (útil para autenticación y asignación) ---
    @Query("""
            SELECT e FROM EmployeeEntity e
            WHERE e.user.email = :email
            """)
    Optional<EmployeeEntity> findByUserEmail(@Param("email") String email);
}
