package com.sparktech.motorx.Services;

import com.sparktech.motorx.entity.LogActionType;
import com.sparktech.motorx.entity.LogServiceName;

public interface ILogService {

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

