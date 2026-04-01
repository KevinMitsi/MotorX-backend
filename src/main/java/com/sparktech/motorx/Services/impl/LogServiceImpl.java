package com.sparktech.motorx.Services.impl;

import com.sparktech.motorx.Services.ILogService;
import com.sparktech.motorx.dto.log.LogFilterRequestDTO;
import com.sparktech.motorx.entity.LogActionType;
import com.sparktech.motorx.entity.LogEntity;
import com.sparktech.motorx.entity.LogResult;
import com.sparktech.motorx.entity.LogServiceName;
import com.sparktech.motorx.repository.JpaLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class LogServiceImpl implements ILogService {

    private final JpaLogRepository logRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<@NotNull LogEntity> findLogs(LogFilterRequestDTO filter, Pageable pageable) {
        String actorEmail = normalizeFilterEmail(filter.actorEmail());
        return logRepository.findAllByFilters(
                filter.serviceName(),
                filter.actionType(),
                filter.result(),
                actorEmail,
                filter.actorUserId(),
                filter.from(),
                filter.to(),
                pageable
        );
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logSuccess(LogServiceName serviceName,
                           LogActionType actionType,
                           String actorEmail,
                           Long actorUserId,
                           String message) {
        saveLog(serviceName, actionType, LogResult.SUCCESS, actorEmail, actorUserId, message);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logFailure(LogServiceName serviceName,
                           LogActionType actionType,
                           String actorEmail,
                           Long actorUserId,
                           String message) {
        saveLog(serviceName, actionType, LogResult.FAILURE, actorEmail, actorUserId, message);
    }

    private void saveLog(LogServiceName serviceName,
                         LogActionType actionType,
                         LogResult result,
                         String actorEmail,
                         Long actorUserId,
                         String message) {
        try {
            LogEntity logEntity = new LogEntity();
            logEntity.setServiceName(serviceName);
            logEntity.setActionType(actionType);
            logEntity.setResult(result);
            logEntity.setActorEmail(actorEmail);
            logEntity.setActorUserId(actorUserId);
            logEntity.setMessage(normalizeMessage(message));
            logRepository.save(logEntity);
        } catch (Exception ex) {
            log.warn("No fue posible persistir el log de {}:{} - {}", serviceName, actionType, ex.getMessage());
        }
    }

    private String normalizeMessage(String message) {
        if (message == null || message.isBlank()) {
            return "Sin detalle";
        }
        if (message.length() <= 500) {
            return message;
        }
        return message.substring(0, 500);
    }

    private String normalizeFilterEmail(String actorEmail) {
        if (actorEmail == null || actorEmail.isBlank()) {
            return null;
        }
        return actorEmail.trim().toLowerCase();
    }
}

