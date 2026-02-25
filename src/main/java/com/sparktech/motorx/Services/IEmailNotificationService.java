package com.sparktech.motorx.Services;

import com.sparktech.motorx.dto.notification.EmailDTO;
import com.sparktech.motorx.entity.AppointmentEntity;

public interface IEmailNotificationService {
    void sendMail(EmailDTO emailDTO) throws Exception;

    void sendAppointmentCreatedNotification(AppointmentEntity appointment);

    void sendAppointmentCancelledNotification(AppointmentEntity appointment, String reason);

    void sendAppointmentUpdatedNotification(AppointmentEntity appointment);
}