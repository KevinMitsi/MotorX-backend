package com.sparktech.motorx.repository;

import com.sparktech.motorx.entity.NotificationEntity;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JpaNotificationRepository extends JpaRepository<@NotNull NotificationEntity, @NotNull Long> {

    List<NotificationEntity> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<NotificationEntity> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(Long userId);

    Optional<NotificationEntity> findByIdAndUserId(Long notificationId, Long userId);
}

