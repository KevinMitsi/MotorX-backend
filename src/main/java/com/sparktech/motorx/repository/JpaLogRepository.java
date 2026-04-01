package com.sparktech.motorx.repository;

import com.sparktech.motorx.entity.LogActionType;
import com.sparktech.motorx.entity.LogEntity;
import com.sparktech.motorx.entity.LogResult;
import com.sparktech.motorx.entity.LogServiceName;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface JpaLogRepository extends JpaRepository<@NotNull LogEntity, @NotNull Long> {

    long countByServiceNameAndActionType(LogServiceName serviceName, LogActionType actionType);

    @Query("""
            SELECT l FROM LogEntity l
            WHERE (:serviceName IS NULL OR l.serviceName = :serviceName)
              AND (:actionType IS NULL OR l.actionType = :actionType)
              AND (:result IS NULL OR l.result = :result)
              AND (:actorEmail IS NULL OR LOWER(CAST(l.actorEmail AS string)) LIKE CONCAT('%', :actorEmail, '%'))
              AND (:actorUserId IS NULL OR l.actorUserId = :actorUserId)
              AND (:fromDate IS NULL OR l.createdAt >= :fromDate)
              AND (:toDate IS NULL OR l.createdAt <= :toDate)
            """)
    Page<@NotNull LogEntity> findAllByFilters(@Param("serviceName") LogServiceName serviceName,
                                              @Param("actionType") LogActionType actionType,
                                              @Param("result") LogResult result,
                                              @Param("actorEmail") String actorEmail,
                                              @Param("actorUserId") Long actorUserId,
                                              @Param("fromDate") LocalDateTime fromDate,
                                              @Param("toDate") LocalDateTime toDate,
                                              Pageable pageable);
}

