package com.sparktech.motorx.repository;

import com.sparktech.motorx.entity.LogActionType;
import com.sparktech.motorx.entity.LogEntity;
import com.sparktech.motorx.entity.LogServiceName;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaLogRepository extends JpaRepository<@NotNull LogEntity, @NotNull Long> {

    long countByServiceNameAndActionType(LogServiceName serviceName, LogActionType actionType);
}

