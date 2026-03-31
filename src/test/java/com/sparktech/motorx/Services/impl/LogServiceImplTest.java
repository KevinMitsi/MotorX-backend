package com.sparktech.motorx.Services.impl;

import com.sparktech.motorx.entity.LogActionType;
import com.sparktech.motorx.entity.LogEntity;
import com.sparktech.motorx.entity.LogResult;
import com.sparktech.motorx.entity.LogServiceName;
import com.sparktech.motorx.repository.JpaLogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("LogServiceImpl - Unit Tests")
class LogServiceImplTest {

    @Mock
    private JpaLogRepository logRepository;

    @InjectMocks
    private LogServiceImpl sut;

    @Captor
    private ArgumentCaptor<LogEntity> logCaptor;

    @Test
    @DisplayName("logSuccess persiste log con resultado SUCCESS")
    void logSuccessShouldPersistLog() {
        sut.logSuccess(LogServiceName.AUTHENTICATION, LogActionType.LOGIN, "user@test.com", 1L, "ok");

        verify(logRepository).save(logCaptor.capture());
        LogEntity captured = logCaptor.getValue();
        assertThat(captured.getServiceName()).isEqualTo(LogServiceName.AUTHENTICATION);
        assertThat(captured.getActionType()).isEqualTo(LogActionType.LOGIN);
        assertThat(captured.getResult()).isEqualTo(LogResult.SUCCESS);
        assertThat(captured.getActorEmail()).isEqualTo("user@test.com");
        assertThat(captured.getActorUserId()).isEqualTo(1L);
        assertThat(captured.getMessage()).isEqualTo("ok");
    }

    @Test
    @DisplayName("logFailure persiste log con resultado FAILURE")
    void logFailureShouldPersistLog() {
        sut.logFailure(LogServiceName.PASSWORD_RESET, LogActionType.PASSWORD_RESET_CONFIRM, null, null, "error");

        verify(logRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getResult()).isEqualTo(LogResult.FAILURE);
    }

    @Test
    @DisplayName("Si message es null, usa texto por defecto")
    void nullMessageShouldUseDefault() {
        sut.logSuccess(LogServiceName.USER, LogActionType.UPDATE_USER_PROFILE, "a@b.com", 7L, null);

        verify(logRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getMessage()).isEqualTo("Sin detalle");
    }

    @Test
    @DisplayName("Si message supera 500 caracteres, se recorta")
    void longMessageShouldBeTrimmed() {
        String longMessage = "x".repeat(550);

        sut.logFailure(LogServiceName.USER, LogActionType.REGISTER, "a@b.com", null, longMessage);

        verify(logRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getMessage()).hasSize(500);
    }

    @Test
    @DisplayName("Si falla el repositorio, no propaga excepcion")
    void repositoryFailureShouldNotPropagate() {
        doThrow(new RuntimeException("db down")).when(logRepository).save(org.mockito.ArgumentMatchers.any(LogEntity.class));

        assertThatCode(() -> sut.logSuccess(LogServiceName.ADMIN, LogActionType.LOGIN, null, null, "x"))
                .doesNotThrowAnyException();
    }
}

