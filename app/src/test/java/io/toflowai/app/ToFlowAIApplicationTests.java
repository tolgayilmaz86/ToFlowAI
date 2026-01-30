package io.toflowai.app;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class ToFlowAIApplicationTests {

    @Test
    void contextLoads() {
        // Test that the Spring context loads successfully
        assertThat(true).isTrue();
    }
}