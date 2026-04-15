package com.sparktech.motorx.Services;

import com.sparktech.motorx.dto.notification.CreateNotificationDTO;
import com.sparktech.motorx.dto.notification.NotificationResponseDTO;

import java.util.List;

public interface INotificationService {

    NotificationResponseDTO createNotification(CreateNotificationDTO dto);

    List<NotificationResponseDTO> getMyNotifications(boolean onlyUnread);

    List<NotificationResponseDTO> getNotificationsByUserId(Long userId);

    NotificationResponseDTO markAsRead(Long notificationId);

    long markAllAsRead();
}

