package com.sparktech.motorx.infrastructure.repository;

import com.sparktech.motorx.domain.enums.Role;
import com.sparktech.motorx.infrastructure.entity.UserEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    // --- Consultas básicas de unicidad y búsqueda ---

    Optional<UserEntity> findByEmail(String email);

    Optional<UserEntity> findByDni(String dni);

    boolean existsByEmail(String email);

    boolean existsByDni(String dni);

    // --- Métrica: Tasa de Duplicidad de Usuarios ---
    // Detecta emails duplicados (no debería ocurrir por constraint, pero sirve para auditoría)
    @Query("SELECT u.email FROM UserEntity u GROUP BY u.email HAVING COUNT(u.email) > 1")
    List<String> findDuplicatedEmails();

    @Query("SELECT u.dni FROM UserEntity u GROUP BY u.dni HAVING COUNT(u.dni) > 1")
    List<String> findDuplicatedDnis();

    // --- Métrica: Cantidad de usuarios registrados por rol ---
    long countByRole(Role role);

    // --- Métrica: Tiempo promedio de registro ---
    // Usuarios registrados en un rango de fechas (para calcular volumen mensual)
    @Query("SELECT u FROM UserEntity u WHERE u.createdAt BETWEEN :start AND :end")
    List<UserEntity> findUsersRegisteredBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    // --- Gestión de estado de cuenta ---
    List<UserEntity> findByEnabledFalse();

    List<UserEntity> findByAccountLockedTrue();

    // --- Búsqueda por nombre parcial (soporte para consulta de cliente) ---
    @Query("SELECT u FROM UserEntity u WHERE LOWER(u.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<UserEntity> findByNameContainingIgnoreCase(@Param("name") String name);

    // --- Total de usuarios registrados (KPI general) ---
    @Query("SELECT COUNT(u) FROM UserEntity u WHERE u.createdAt BETWEEN :start AND :end")
    long countUsersRegisteredBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}