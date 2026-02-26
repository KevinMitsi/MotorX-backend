package com.sparktech.motorx.Services.impl;

import com.sparktech.motorx.dto.notification.AppointmentNotificationDTO;
import com.sparktech.motorx.dto.notification.EmailDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailNotificationServiceImpl - Unit Tests")
class EmailNotificationServiceImplTest {

    /*
     * Usamos @Spy en lugar de @InjectMocks para poder hacer
     * doNothing() / doThrow() sobre sendMail() sin levantar
     * una conexión SMTP real.
     *
     * Patrón:
     *   doNothing().when(sut).sendMail(any())  → intercepta la llamada
     *   sut.sendAppointmentXxx(...)            → ejecuta lógica real
     *   verify + captor                        → valida el EmailDTO construido
     */
    @Spy
    private EmailNotificationServiceImpl sut;

    @Captor
    private ArgumentCaptor<EmailDTO> emailCaptor;

    // ================================================================
    // FIXTURE — DTO de notificación reutilizable
    // ================================================================

    private AppointmentNotificationDTO buildNotificationDTO(String techName) {
        return new AppointmentNotificationDTO(
                "cliente@test.com",
                "Carlos Pérez",
                LocalDate.of(2099, 6, 15),
                LocalTime.of(9, 0),
                "OIL_CHANGE",
                "HONDA",
                "CB 190",
                "ABC3AX",
                techName
        );
    }

    @BeforeEach
    void injectSmtpProps() {
        // Inyectar los @Value manualmente para que el Spy tenga valores coherentes
        ReflectionTestUtils.setField(sut, "smtpHost",     "smtp.test.com");
        ReflectionTestUtils.setField(sut, "smtpPort",     587);
        ReflectionTestUtils.setField(sut, "smtpUsername", "no-reply@test.com");
        ReflectionTestUtils.setField(sut, "smtpPassword", "secret");
    }

    // ================================================================
    // sendAppointmentCreatedNotification()
    // ================================================================

    @Nested
    @DisplayName("sendAppointmentCreatedNotification()")
    class CreatedNotificationTests {

        @Test
        @DisplayName("Llama a sendMail() con subject que contiene la fecha de la cita")
        void givenValidDTO_thenSendMailWithCorrectSubject() {
            // Arrange
            doNothing().when(sut).sendMail(any());
            AppointmentNotificationDTO dto = buildNotificationDTO("Juan Técnico");

            // Act
            sut.sendAppointmentCreatedNotification(dto);

            // Assert
            verify(sut, times(1)).sendMail(emailCaptor.capture());
            assertThat(emailCaptor.getValue().subject())
                    .contains("2099-06-15")
                    .contains("agendada");
        }

        @Test
        @DisplayName("El body contiene nombre del cliente, fecha, hora, tipo y datos del vehículo")
        void givenValidDTO_thenBodyContainsAllRelevantData() {
            // Arrange
            doNothing().when(sut).sendMail(any());
            AppointmentNotificationDTO dto = buildNotificationDTO("Juan Técnico");

            // Act
            sut.sendAppointmentCreatedNotification(dto);

            // Assert
            verify(sut).sendMail(emailCaptor.capture());
            String body = emailCaptor.getValue().body();

            assertThat(body)
                    .contains("Carlos Pérez")
                    .contains("2099-06-15")
                    .contains("09:00")
                    .contains("OIL_CHANGE")
                    .contains("HONDA")
                    .contains("CB 190")
                    .contains("ABC3AX");
        }

        @Test
        @DisplayName("El recipient del EmailDTO es el email del cliente")
        void givenValidDTO_thenRecipientIsClientEmail() {
            // Arrange
            doNothing().when(sut).sendMail(any());

            // Act
            sut.sendAppointmentCreatedNotification(buildNotificationDTO("Juan"));

            // Assert
            verify(sut).sendMail(emailCaptor.capture());
            assertThat(emailCaptor.getValue().recipient())
                    .isEqualTo("cliente@test.com");
        }

        @Test
        @DisplayName("Si sendMail() lanza excepción, no se propaga (se captura internamente)")
        void givenSendMailThrows_thenNoExceptionPropagates() {
            // Arrange
            doThrow(new RuntimeException("SMTP caído"))
                    .when(sut).sendMail(any());

            // Act + Assert — la excepción queda atrapada en el catch interno
            assertThatCode(() ->
                    sut.sendAppointmentCreatedNotification(buildNotificationDTO("Juan")))
                    .doesNotThrowAnyException();
        }
    }

    // ================================================================
    // sendAppointmentCancelledNotification()
    // ================================================================

    @Nested
    @DisplayName("sendAppointmentCancelledNotification()")
    class CancelledNotificationTests {

        private static final String REASON = "El técnico no podrá asistir";

        @Test
        @DisplayName("Subject contiene la fecha de la cita cancelada")
        void givenValidDTO_thenSubjectContainsDate() {
            // Arrange
            doNothing().when(sut).sendMail(any());

            // Act
            sut.sendAppointmentCancelledNotification(buildNotificationDTO("Juan"), REASON);

            // Assert
            verify(sut).sendMail(emailCaptor.capture());
            assertThat(emailCaptor.getValue().subject())
                    .contains("2099-06-15")
                    .contains("cancelada");
        }

        @Test
        @DisplayName("Body contiene nombre del cliente, fecha, hora y motivo de cancelación")
        void givenValidDTO_thenBodyContainsCancellationData() {
            // Arrange
            doNothing().when(sut).sendMail(any());

            // Act
            sut.sendAppointmentCancelledNotification(buildNotificationDTO("Juan"), REASON);

            // Assert
            verify(sut).sendMail(emailCaptor.capture());
            String body = emailCaptor.getValue().body();

            assertThat(body)
                    .contains("Carlos Pérez")
                    .contains("2099-06-15")
                    .contains("09:00")
                    .contains(REASON);
        }

        @Test
        @DisplayName("El recipient es el email del cliente")
        void givenValidDTO_thenRecipientIsClientEmail() {
            // Arrange
            doNothing().when(sut).sendMail(any());

            // Act
            sut.sendAppointmentCancelledNotification(buildNotificationDTO(null), REASON);

            // Assert
            verify(sut).sendMail(emailCaptor.capture());
            assertThat(emailCaptor.getValue().recipient())
                    .isEqualTo("cliente@test.com");
        }

        @Test
        @DisplayName("Si sendMail() lanza excepción, no se propaga")
        void givenSendMailThrows_thenNoExceptionPropagates() {
            // Arrange
            doThrow(new RuntimeException("SMTP caído")).when(sut).sendMail(any());

            // Act + Assert
            assertThatCode(() ->
                    sut.sendAppointmentCancelledNotification(
                            buildNotificationDTO(null), REASON))
                    .doesNotThrowAnyException();
        }
    }

    // ================================================================
    // sendAppointmentUpdatedNotification()
    // ================================================================

    @Nested
    @DisplayName("sendAppointmentUpdatedNotification()")
    class UpdatedNotificationTests {

        @Test
        @DisplayName("Subject contiene la fecha de la cita actualizada")
        void givenValidDTO_thenSubjectContainsDate() {
            // Arrange
            doNothing().when(sut).sendMail(any());

            // Act
            sut.sendAppointmentUpdatedNotification(buildNotificationDTO("Maria"));

            // Assert
            verify(sut).sendMail(emailCaptor.capture());
            assertThat(emailCaptor.getValue().subject())
                    .contains("2099-06-15")
                    .contains("Actualización");
        }

        @Test
        @DisplayName("Body contiene nombre del cliente, fecha, hora y nombre del técnico")
        void givenValidDTO_thenBodyContainsUpdatedData() {
            // Arrange
            doNothing().when(sut).sendMail(any());

            // Act
            sut.sendAppointmentUpdatedNotification(buildNotificationDTO("Maria Técnica"));

            // Assert
            verify(sut).sendMail(emailCaptor.capture());
            String body = emailCaptor.getValue().body();

            assertThat(body)
                    .contains("Carlos Pérez")
                    .contains("2099-06-15")
                    .contains("09:00")
                    .contains("Maria Técnica");
        }

        @Test
        @DisplayName("Cuando technicianName es null, el body muestra 'por asignar'")
        void givenNullTechnicianName_thenBodyShowsFallback() {
            // Arrange
            doNothing().when(sut).sendMail(any());
            AppointmentNotificationDTO dto = buildNotificationDTO(null); // techName = null

            // Act
            sut.sendAppointmentUpdatedNotification(dto);

            // Assert
            verify(sut).sendMail(emailCaptor.capture());
            assertThat(emailCaptor.getValue().body())
                    .contains("por asignar");
        }

        @Test
        @DisplayName("El recipient es el email del cliente")
        void givenValidDTO_thenRecipientIsClientEmail() {
            // Arrange
            doNothing().when(sut).sendMail(any());

            // Act
            sut.sendAppointmentUpdatedNotification(buildNotificationDTO("Pedro"));

            // Assert
            verify(sut).sendMail(emailCaptor.capture());
            assertThat(emailCaptor.getValue().recipient())
                    .isEqualTo("cliente@test.com");
        }

        @Test
        @DisplayName("Si sendMail() lanza excepción, no se propaga")
        void givenSendMailThrows_thenNoExceptionPropagates() {
            // Arrange
            doThrow(new RuntimeException("SMTP caído")).when(sut).sendMail(any());

            // Act + Assert
            assertThatCode(() ->
                    sut.sendAppointmentUpdatedNotification(buildNotificationDTO("Pedro")))
                    .doesNotThrowAnyException();
        }
    }

    // ================================================================
    // sendMail() — constante HOLA
    // ================================================================

    @Nested
    @DisplayName("Constante HOLA en bodies")
    class GreetingConstantTests {

        @Test
        @DisplayName("Todos los bodies comienzan con 'Hola ' seguido del nombre del cliente")
        void givenAnyNotification_thenBodyStartsWithHola() {
            // Arrange
            doNothing().when(sut).sendMail(any());
            AppointmentNotificationDTO dto = buildNotificationDTO("Juan");

            ArgumentCaptor<EmailDTO> captor = ArgumentCaptor.forClass(EmailDTO.class);

            // Act — los tres tipos de notificación
            sut.sendAppointmentCreatedNotification(dto);
            sut.sendAppointmentCancelledNotification(dto, "Motivo");
            sut.sendAppointmentUpdatedNotification(dto);

            // Assert
            verify(sut, times(3)).sendMail(captor.capture());
            captor.getAllValues().forEach(emailDTO ->
                    assertThat(emailDTO.body())
                            .startsWith("Hola Carlos Pérez"));
        }
    }
}