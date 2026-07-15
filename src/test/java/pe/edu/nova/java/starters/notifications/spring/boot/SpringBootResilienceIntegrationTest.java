package pe.edu.nova.java.starters.notifications.spring.boot;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import pe.edu.nova.java.libs.notifications.infrastructure.configuration.NotificationConfiguration;
import pe.edu.nova.java.libs.notifications.infrastructure.configuration.ResilienceConfiguration;

/**
 * Integration test for the Spring Boot starter when the resilience
 * configuration is fully customized via {@code nova.notifications.resilience.*}
 * properties. Verifies that every individual property in the nested
 * resilience config flows through to the library's
 * {@link ResilienceConfiguration}.
 */
@SpringBootTest(classes = SpringBootResilienceIntegrationTest.TestApp.class, properties = {
        "nova.notifications.email.provider=SENDGRID",
        "nova.notifications.email.api-key=test-api-key",
        "nova.notifications.email.default-sender=no-reply@example.com",
        "nova.notifications.resilience.max-attempts=7",
        "nova.notifications.resilience.initial-backoff-millis=500",
        "nova.notifications.resilience.circuit-failure-threshold=11",
        "nova.notifications.resilience.circuit-open-duration-seconds=42",
        "nova.notifications.resilience.rate-limit-permits-per-second=3"
})
class SpringBootResilienceIntegrationTest {

    @Autowired
    private NotificationConfiguration configuration;

    @Test
    void resilienceConfigurationIsPropagatedToTheLibrary() {
        ResilienceConfiguration resilience = configuration.resilience();
        assertThat(resilience.maxAttempts()).isEqualTo(7);
        assertThat(resilience.initialBackoff()).hasMillis(500);
        assertThat(resilience.circuitFailureThreshold()).isEqualTo(11);
        assertThat(resilience.circuitOpenDuration()).hasSeconds(42);
        assertThat(resilience.rateLimitPermitsPerSecond()).isEqualTo(3);
    }

    @SpringBootApplication
    static class TestApp {
        // Minimal app — just enables auto-configuration.
    }
}