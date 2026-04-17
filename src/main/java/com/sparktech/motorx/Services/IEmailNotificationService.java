package com.sparktech.motorx.Services;

import com.sparktech.motorx.dto.notification.EmailDTO;
import com.sparktech.motorx.dto.notification.AppointmentNotificationDTO;

import java.util.Map;

public interface IEmailNotificationService {
    void sendMail(EmailDTO emailDTO);

    void sendTemplatedMail(String recipient, String subject, String templateName, Map<String, String> placeholders);

    void sendAppointmentCreatedNotification(AppointmentNotificationDTO appointment);

    void sendAppointmentCancelledNotification(AppointmentNotificationDTO appointment, String reason);

    void sendAppointmentUpdatedNotification(AppointmentNotificationDTO appointment);
}