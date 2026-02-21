package com.sparktech.motorx.repository;

import com.sparktech.motorx.entity.PasswordResetTokenEntity;
import com.sparktech.motorx.entity.UserEntity;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface JpaPasswordResetTokenRepository extends JpaRepository<@NotNull PasswordResetTokenEntity, @NotNull Long> {
        // Busca un token por su hash que no haya sido usado

        Optional<PasswordResetTokenEntity> findByTokenHashAndUsedFalse(String tokenHash);

        // Busca todos los tokens no usados de un usuario

        List<PasswordResetTokenEntity> findByUserAndUsedFalse(UserEntity user);



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