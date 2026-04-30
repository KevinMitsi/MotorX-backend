package com.sparktech.motorx.repository;

import com.sparktech.motorx.entity.OrderServiceEntity;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JpaOrderServiceRepository extends JpaRepository<@NotNull OrderServiceEntity, @NotNull Long> {

    Optional<OrderServiceEntity> findByAppointmentId(Long appointmentId);

    @Query("""
            SELECT o FROM OrderServiceEntity o
            LEFT JOIN FETCH o.procedures op
            LEFT JOIN FETCH op.procedure
            LEFT JOIN FETCH o.spares os
            LEFT JOIN FETCH os.spare
            WHERE o.appointment.id = :appointmentId
            """)
    Optional<OrderServiceEntity> findDetailedByAppointmentId(@Param("appointmentId") Long appointmentId);

    @Query("""
            SELECT o FROM OrderServiceEntity o
            LEFT JOIN FETCH o.procedures op
            LEFT JOIN FETCH op.procedure
            LEFT JOIN FETCH o.spares os
            LEFT JOIN FETCH os.spare
            WHERE o.id = :orderId
            """)
    Optional<OrderServiceEntity> findDetailedById(@Param("orderId") Long orderId);
}

