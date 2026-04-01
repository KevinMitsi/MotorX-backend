package com.sparktech.motorx.Services;

import com.sparktech.motorx.entity.LogActionType;
import com.sparktech.motorx.entity.LogEntity;
import com.sparktech.motorx.entity.LogServiceName;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ILogService {

    Page<@NotNull LogEntity> findAll(Pageable pageable);

    void logSuccess(LogServiceName serviceName,
                    LogActionType actionType,
                    String actorEmail,
                    Long actorUserId,
                    String message);

    void logFailure(LogServiceName serviceName,
                    LogActionType actionType,
                    String actorEmail,
                    Long actorUserId,
                    String message);
}

