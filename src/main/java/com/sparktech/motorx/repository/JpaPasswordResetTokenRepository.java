package com.sparktech.motorx.repository;

import com.sparktech.motorx.entity.PasswordResetTokenEntity;
import com.sparktech.motorx.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface JpaPasswordResetTokenRepository extends JpaRepository<PasswordResetTokenEntity, Long> {

    // --- Validación del token ---
    Optional<PasswordResetTokenEntity> findByTokenHash(String tokenHash);

    // Busca token válido: no usado y no expirado
    @Query("""
            SELECT t FROM PasswordResetTokenEntity t
            WHERE t.tokenHash = :tokenHash
              AND t.used = false
              AND t.expiresAt > :now
            """)
    Optional<PasswordResetTokenEntity> findValidToken(
            @Param("tokenHash") String tokenHash,
            @Param("now") LocalDateTime now
    );

    // --- Métrica: Tiempo promedio de recuperación de contraseña ---
    // Trae tokens usados en un rango para calcular (usedAt - createdAt)
    @Query("""
            SELECT t FROM PasswordResetTokenEntity t
            WHERE t.used = true
              AND t.usedAt BETWEEN :start AND :end
            """)
    List<PasswordResetTokenEntity> findUsedTokensBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    // --- Tokens activos de un usuario (para invalidar anteriores al generar uno nuevo) ---
    @Query("""
            SELECT t FROM PasswordResetTokenEntity t
            WHERE t.user = :user
              AND t.used = false
              AND t.expiresAt > :now
            """)
    List<PasswordResetTokenEntity> findActiveTokensByUser(
            @Param("user") UserEntity user,
            @Param("now") LocalDateTime now
    );

    // --- Limpieza de tokens expirados (tarea programada) ---
    @Modifying
    @Query("DELETE FROM PasswordResetTokenEntity t WHERE t.expiresAt < :now AND t.used = false")
    void deleteExpiredTokens(@Param("now") LocalDateTime now);

    // --- Conteo de solicitudes de recuperación por usuario (detección de abuso) ---
    @Query("""
            SELECT COUNT(t) FROM PasswordResetTokenEntity t
            WHERE t.user = :user
              AND t.createdAt BETWEEN :start AND :end
            """)
    long countRequestsByUserBetween(
            @Param("user") UserEntity user,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}