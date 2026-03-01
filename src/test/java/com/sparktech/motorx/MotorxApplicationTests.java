package com.sparktech.motorx;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class MotorxApplicationTests {

    @Test
    @DisabledIfEnvironmentVariable(named = "CI", matches = "true")
    void contextLoads() {
    }

}
