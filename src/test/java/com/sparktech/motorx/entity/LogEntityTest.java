package com.sparktech.motorx.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LogEntity - Unit Tests")
class LogEntityTest {

    @Test
    @DisplayName("onCreate asigna createdAt cuando esta null")
    void onCreateShouldSetCreatedAtWhenNull() throws Exception {
        LogEntity entity = new LogEntity();

        Method onCreate = LogEntity.class.getDeclaredMethod("onCreate");
        onCreate.setAccessible(true);
        onCreate.invoke(entity);

        assertThat(entity.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("onCreate respeta createdAt cuando ya existe")
    void onCreateShouldKeepExistingCreatedAt() throws Exception {
        LocalDateTime fixedDate = LocalDateTime.of(2026, 3, 30, 10, 30);
        LogEntity entity = new LogEntity();
        entity.setCreatedAt(fixedDate);

        Method onCreate = LogEntity.class.getDeclaredMethod("onCreate");
        onCreate.setAccessible(true);
        onCreate.invoke(entity);

        assertThat(entity.getCreatedAt()).isEqualTo(fixedDate);
    }
}

