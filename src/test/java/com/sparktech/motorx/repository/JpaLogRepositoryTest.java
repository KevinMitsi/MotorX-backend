package com.sparktech.motorx.repository;

import com.sparktech.motorx.entity.LogActionType;
import com.sparktech.motorx.entity.LogEntity;
import com.sparktech.motorx.entity.LogResult;
import com.sparktech.motorx.entity.LogServiceName;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
@DisplayName("JpaLogRepository - DataJpaTest")
class JpaLogRepositoryTest {

    @Autowired
    private JpaLogRepository repository;

    @Test
    @DisplayName("countByServiceNameAndActionType cuenta logs segun servicio y accion")
    void countByServiceNameAndActionTypeShouldWork() {
        LogEntity logA = new LogEntity();
        logA.setServiceName(LogServiceName.AUTHENTICATION);
        logA.setActionType(LogActionType.LOGIN);
        logA.setResult(LogResult.SUCCESS);
        logA.setMessage("login ok");

        LogEntity logB = new LogEntity();
        logB.setServiceName(LogServiceName.AUTHENTICATION);
        logB.setActionType(LogActionType.LOGIN);
        logB.setResult(LogResult.FAILURE);
        logB.setMessage("login fail");

        LogEntity logC = new LogEntity();
        logC.setServiceName(LogServiceName.PASSWORD_RESET);
        logC.setActionType(LogActionType.PASSWORD_RESET_REQUEST);
        logC.setResult(LogResult.SUCCESS);
        logC.setMessage("request ok");

        repository.save(logA);
        repository.save(logB);
        repository.save(logC);

        long total = repository.countByServiceNameAndActionType(
                LogServiceName.AUTHENTICATION,
                LogActionType.LOGIN
        );

        assertThat(total).isEqualTo(2L);
    }
}

