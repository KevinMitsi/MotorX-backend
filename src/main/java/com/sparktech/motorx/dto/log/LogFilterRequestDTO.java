package com.sparktech.motorx.dto.log;

import com.sparktech.motorx.entity.LogActionType;

import com.sparktech.motorx.entity.LogServiceName;

import java.time.LocalDateTime;

public record LogFilterRequestDTO(
        LogServiceName serviceName,
        LogActionType actionType,
        Long actorUserId,
        LocalDateTime from,
        LocalDateTime to
) {
}

