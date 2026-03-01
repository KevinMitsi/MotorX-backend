package com.sparktech.motorx.Services.impl;

import com.sparktech.motorx.Services.IEmailNotificationService;
import com.sparktech.motorx.dto.notification.EmailDTO;
import com.sparktech.motorx.dto.notification.AppointmentNotificationDTO;

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

    public static final String HOLA = "Hola ";
    @Value("${SMTP_HOST:smtp.gmail.com}")
    private String smtpHost;

    @Value("${SMTP_PORT:587}")
    private int smtpPort;

    // Removed hard-coded default username and password - must be provided by environment (docker)
    @Value("${SMTP_USERNAME}")
    private String smtpUsername;

    @Value("${SMTP_PASSWORD}")
    private String smtpPassword;

    @Override
    public void sendMail(EmailDTO emailDTO) {
        log.info("Sending email to: {} ", emailDTO.recipient());

        // Guard against missing credentials (to avoid runtime NPEs if env not provided)
        if (smtpUsername == null || smtpUsername.isBlank() || smtpPassword == null || smtpPassword.isBlank()) {
            log.error("SMTP credentials are not configured. Set SMTP_USERNAME and SMTP_PASSWORD in the environment before sending emails.");
            return;
        }

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
            // manejar localmente: log y regresar (los métodos que llaman ya están @Async)
        }
    }

    @Override
    @Async
    public void sendAppointmentCreatedNotification(AppointmentNotificationDTO appointment) {
        String clientEmail = appointment.clientEmail();
        String clientName  = appointment.clientName();
        String subject = "Cita agendada - " + appointment.appointmentDate();
        String body = HOLA + clientName + ",\n\n" +
                "Tu cita ha sido agendada exitosamente.\n" +
                "Fecha: " + appointment.appointmentDate() + "\n" +
                "Hora: " + appointment.startTime() + "\n" +
                "Tipo: " + appointment.appointmentType() + "\n" +
                "Vehículo: " + appointment.vehicleBrand() + " " +
                appointment.vehicleModel() + " (" + appointment.licensePlate() + ")\n\n" +
                "¡Te esperamos!\n\nJmmotoservicio";
        try {
            sendMail(new EmailDTO(subject, body, clientEmail));
        } catch (Exception e) {
            log.error("Error sending appointment created notification to: {}", clientEmail, e);
        }
    }

    @Override
    @Async
    public void sendAppointmentCancelledNotification(AppointmentNotificationDTO appointment, String reason) {
        String clientEmail = appointment.clientEmail();
        String clientName  = appointment.clientName();
        String subject = "Cita cancelada - " + appointment.appointmentDate();
        String body = HOLA + clientName + ",\n\n" +
                "Lamentamos informarte que tu cita del " + appointment.appointmentDate() +
                " a las " + appointment.startTime() + " ha sido cancelada.\n" +
                "Motivo: " + reason + "\n\n" +
                "Por favor contáctanos para reagendar.\n\nJmmotoservicio";
        try {
            sendMail(new EmailDTO(subject, body, clientEmail));
        } catch (Exception e) {
            log.error("Error sending appointment cancelled notification to: {}", clientEmail, e);
        }
    }

    @Override
    @Async
    public void sendAppointmentUpdatedNotification(AppointmentNotificationDTO appointment) {
        String clientEmail = appointment.clientEmail();
        String clientName  = appointment.clientName();
        String techName = appointment.technicianName() != null ? appointment.technicianName() : "por asignar";
        String subject = "Actualización de tu cita - " + appointment.appointmentDate();
        String body = HOLA + clientName + ",\n\n" +
                "Tu cita del " + appointment.appointmentDate() + " a las " +
                appointment.startTime() + " ha sido actualizada.\n" +
                "Técnico asignado: " + techName + "\n\n" +
                "¡Te esperamos!\n\nJmmotoservicio";
        try {
            sendMail(new EmailDTO(subject, body, clientEmail));
        } catch (Exception e) {
            log.error("Error sending appointment updated notification to: {}", clientEmail, e);
        }
    }
}
