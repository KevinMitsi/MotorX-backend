package com.sparktech.motorx.Services.impl;

import com.sparktech.motorx.Services.ICurrentUserService;
import com.sparktech.motorx.Services.ILogService;
import com.sparktech.motorx.Services.INotificationService;
import com.sparktech.motorx.dto.notification.CreateNotificationDTO;
import com.sparktech.motorx.dto.notification.NotificationResponseDTO;
import com.sparktech.motorx.entity.*;
import com.sparktech.motorx.exception.NotificationNotFoundException;
import com.sparktech.motorx.exception.UserNotFoundException;
import com.sparktech.motorx.repository.JpaNotificationRepository;
import com.sparktech.motorx.repository.JpaUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements INotificationService {

    private final JpaNotificationRepository notificationRepository;
    private final JpaUserRepository userRepository;
    private final ICurrentUserService currentUserService;
    private final ILogService logService;

    @Override
    @Transactional
    public NotificationResponseDTO createNotification(CreateNotificationDTO dto) {
        UserEntity actor = currentUserService.getAuthenticatedUser();
        try {
            UserEntity user = userRepository.findById(dto.userId())
                    .orElseThrow(() -> new UserNotFoundException(dto.userId()));

            NotificationEntity notification = NotificationEntity.builder()
                    .user(user)
                    .title(dto.title())
                    .description(dto.description())
                    .urgency(dto.urgency())
                    .source(dto.source())
                    .isRead(false)
                    .build();

            NotificationEntity saved = notificationRepository.save(notification);
            logService.logSuccess(
                    LogServiceName.NOTIFICATION,
                    LogActionType.CREATE_NOTIFICATION,
                    actor.getEmail(),
                    actor.getId(),
                    "Notificacion creada para usuario " + user.getId()
            );
            return toResponse(saved);
        } catch (RuntimeException ex) {
            logService.logFailure(
                    LogServiceName.NOTIFICATION,
                    LogActionType.CREATE_NOTIFICATION,
                    actor.getEmail(),
                    actor.getId(),
                    ex.getMessage()
            );
            throw ex;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponseDTO> getMyNotifications(boolean onlyUnread) {
        UserEntity actor = currentUserService.getAuthenticatedUser();
        List<NotificationEntity> notifications = onlyUnread
                ? notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(actor.getId())
                : notificationRepository.findByUserIdOrderByCreatedAtDesc(actor.getId());
        return notifications.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponseDTO> getNotificationsByUserId(Long userId) {
        userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public NotificationResponseDTO markAsRead(Long notificationId) {
        UserEntity actor = currentUserService.getAuthenticatedUser();
        try {
            NotificationEntity notification = notificationRepository.findByIdAndUserId(notificationId, actor.getId())
                    .orElseThrow(() -> new NotificationNotFoundException(notificationId));

            if (!Boolean.TRUE.equals(notification.getIsRead())) {
                notification.setIsRead(true);
                notification.setReadAt(LocalDateTime.now());
                notification = notificationRepository.save(notification);
            }

            logService.logSuccess(
                    LogServiceName.NOTIFICATION,
                    LogActionType.READ_NOTIFICATION,
                    actor.getEmail(),
                    actor.getId(),
                    "Notificacion marcada como leida: " + notificationId
            );
            return toResponse(notification);
        } catch (RuntimeException ex) {
            logService.logFailure(
                    LogServiceName.NOTIFICATION,
                    LogActionType.READ_NOTIFICATION,
                    actor.getEmail(),
                    actor.getId(),
                    ex.getMessage()
            );
            throw ex;
        }
    }

    @Override
    @Transactional
    public long markAllAsRead() {
        UserEntity actor = currentUserService.getAuthenticatedUser();
        try {
            List<NotificationEntity> unread = notificationRepository
                    .findByUserIdAndIsReadFalseOrderByCreatedAtDesc(actor.getId());

            if (unread.isEmpty()) {
                return 0;
            }

            LocalDateTime now = LocalDateTime.now();
            unread.forEach(notification -> {
                notification.setIsRead(true);
                notification.setReadAt(now);
            });
            notificationRepository.saveAll(unread);

            logService.logSuccess(
                    LogServiceName.NOTIFICATION,
                    LogActionType.READ_ALL_NOTIFICATIONS,
                    actor.getEmail(),
                    actor.getId(),
                    "Notificaciones marcadas como leidas: " + unread.size()
            );
            return unread.size();
        } catch (RuntimeException ex) {
            logService.logFailure(
                    LogServiceName.NOTIFICATION,
                    LogActionType.READ_ALL_NOTIFICATIONS,
                    actor.getEmail(),
                    actor.getId(),
                    ex.getMessage()
            );
            throw ex;
        }
    }

    private NotificationResponseDTO toResponse(NotificationEntity notification) {
        return new NotificationResponseDTO(
                notification.getId(),
                notification.getUser().getId(),
                notification.getTitle(),
                notification.getDescription(),
                notification.getUrgency(),
                notification.getIsRead(),
                notification.getSource(),
                notification.getCreatedAt(),
                notification.getReadAt()
        );
    }
}

