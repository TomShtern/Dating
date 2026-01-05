package com.datingapp;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Basic application context test.
 * Verifies Spring Boot application can start.
 */
@SpringBootTest
@ActiveProfiles("test")
class DatingApplicationTests {

    @Test
    void contextLoads() {
        // Test that Spring context loads successfully
    }
}
