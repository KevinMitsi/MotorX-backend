package com.sparktech.motorx.Services;

import com.sparktech.motorx.dto.notification.EmailDTO;

public interface IEmailNotificationService {
    void sendMail(EmailDTO emailDTO) throws Exception;
}