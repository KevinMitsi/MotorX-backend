package com.sparktech.motorx.Services;

import com.sparktech.motorx.dto.notification.EmailDTO;
import com.sparktech.motorx.dto.notification.AppointmentNotificationDTO;

public interface IEmailNotificationService {
    void sendMail(EmailDTO emailDTO);

    void sendAppointmentCreatedNotification(AppointmentNotificationDTO appointment);

    void sendAppointmentCancelledNotification(AppointmentNotificationDTO appointment, String reason);

    void sendAppointmentUpdatedNotification(AppointmentNotificationDTO appointment);
}