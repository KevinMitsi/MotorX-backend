package com.sparktech.motorx.Services.impl;

import com.sparktech.motorx.Services.IEmailNotificationService;
import com.sparktech.motorx.dto.notification.EmailDTO;
import com.sparktech.motorx.entity.AppointmentEntity;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.simplejavamail.api.email.Email;
import org.simplejavamail.api.mailer.Mailer;
import org.simplejavamail.api.mailer.config.TransportStrategy;
import org.simplejavamail.email.EmailBuilder;
import org.simplejavamail.mailer.MailerBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationServiceImpl implements IEmailNotificationService {

    @Value("${SMTP_HOST:smtp.gmail.com}")
    private String smtpHost;

    @Value("${SMTP_PORT:587}")
    private int smtpPort;

    @Value("${SMTP_USERNAME:kegarrapala.2003@gmail.com}")
    private String smtpUsername;

    @Value("${SMTP_PASSWORD:tbta upjg guxo oprh}")
    private String smtpPassword;

    @Override
    @Async
    public void sendMail(EmailDTO emailDTO) throws Exception {
        log.info("Sending email to: {} ", emailDTO.recipient());

        Email email = EmailBuilder.startingBlank()
                .from(smtpUsername)
                .to(emailDTO.recipient())
                .withSubject(emailDTO.subject())
                .withPlainText(emailDTO.body())
                .buildEmail();

        try (Mailer mailer = MailerBuilder
                .withSMTPServer(smtpHost, smtpPort, smtpUsername, smtpPassword)
                .withTransportStrategy(TransportStrategy.SMTP_TLS)
                .withDebugLogging(true)
                .buildMailer()) {

            mailer.sendMail(email);
            log.info("Email sent successfully to: {}", emailDTO.recipient());
        } catch (Exception e) {
            log.error("Error sending email to: {}", emailDTO.recipient(), e);
            throw e;
        }
    }

    @Override
    @Async
    public void sendAppointmentCreatedNotification(AppointmentEntity appointment) {
        String clientEmail = appointment.getVehicle().getOwner().getEmail();
        String clientName  = appointment.getVehicle().getOwner().getName();
        String subject = "Cita agendada - " + appointment.getAppointmentDate();
        String body = "Hola " + clientName + ",\n\n" +
                "Tu cita ha sido agendada exitosamente.\n" +
                "Fecha: " + appointment.getAppointmentDate() + "\n" +
                "Hora: " + appointment.getStartTime() + "\n" +
                "Tipo: " + appointment.getAppointmentType() + "\n" +
                "Vehículo: " + appointment.getVehicle().getBrand() + " " +
                appointment.getVehicle().getModel() + " (" + appointment.getVehicle().getLicensePlate() + ")\n\n" +
                "¡Te esperamos!\n\nJmmotoservicio";
        try {
            sendMail(new EmailDTO(clientEmail, subject, body));
        } catch (Exception e) {
            log.error("Error sending appointment created notification to: {}", clientEmail, e);
        }
    }

    @Override
    @Async
    public void sendAppointmentCancelledNotification(AppointmentEntity appointment, String reason) {
        String clientEmail = appointment.getVehicle().getOwner().getEmail();
        String clientName  = appointment.getVehicle().getOwner().getName();
        String subject = "Cita cancelada - " + appointment.getAppointmentDate();
        String body = "Hola " + clientName + ",\n\n" +
                "Lamentamos informarte que tu cita del " + appointment.getAppointmentDate() +
                " a las " + appointment.getStartTime() + " ha sido cancelada.\n" +
                "Motivo: " + reason + "\n\n" +
                "Por favor contáctanos para reagendar.\n\nJmmotoservicio";
        try {
            sendMail(new EmailDTO(clientEmail, subject, body));
        } catch (Exception e) {
            log.error("Error sending appointment cancelled notification to: {}", clientEmail, e);
        }
    }

    @Override
    @Async
    public void sendAppointmentUpdatedNotification(AppointmentEntity appointment) {
        String clientEmail = appointment.getVehicle().getOwner().getEmail();
        String clientName  = appointment.getVehicle().getOwner().getName();
        String techName = appointment.getTechnician() != null
                ? appointment.getTechnician().getUser().getName()
                : "por asignar";
        String subject = "Actualización de tu cita - " + appointment.getAppointmentDate();
        String body = "Hola " + clientName + ",\n\n" +
                "Tu cita del " + appointment.getAppointmentDate() + " a las " +
                appointment.getStartTime() + " ha sido actualizada.\n" +
                "Técnico asignado: " + techName + "\n\n" +
                "¡Te esperamos!\n\nJmmotoservicio";
        try {
            sendMail(new EmailDTO(clientEmail, subject, body));
        } catch (Exception e) {
            log.error("Error sending appointment updated notification to: {}", clientEmail, e);
        }
    }
}
