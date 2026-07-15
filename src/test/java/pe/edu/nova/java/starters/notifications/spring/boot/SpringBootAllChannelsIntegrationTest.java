package pe.edu.nova.java.starters.notifications.spring.boot;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import pe.edu.nova.java.libs.notifications.infrastructure.configuration.NotificationConfiguration;

/**
 * Integration test for the Spring Boot starter when all four channels
 * (email + sms + push + slack) are configured simultaneously.
 *
 * <p>Verifies that the auto-configuration wires every configured channel
 * into the library's {@link NotificationConfiguration}, regardless of
 * which one the user opts into.
 */
@SpringBootTest(classes = SpringBootAllChannelsIntegrationTest.TestApp.class, properties = {
        "nova.notifications.email.provider=SENDGRID",
        "nova.notifications.email.api-key=test-api-key",
        "nova.notifications.email.default-sender=no-reply@example.com",
        "nova.notifications.sms.provider=twilio",
        "nova.notifications.sms.account-sid=AC123",
        "nova.notifications.sms.auth-token=auth-token",
        "nova.notifications.sms.from-number=+15005550006",
        "nova.notifications.push.provider=firebase",
        "nova.notifications.push.project-id=project-1",
        "nova.notifications.push.server-key=server-key-1",
        "nova.notifications.slack.default-webhook-url=https://hooks.slack.com/services/T0/B0/secret",
        "nova.notifications.resilience.max-attempts=1"
})
class SpringBootAllChannelsIntegrationTest {

    @Autowired
    private NotificationConfiguration configuration;

    @Test
    void allFourChannelsArePresent() {
        assertThat(configuration.email()).isPresent();
        assertThat(configuration.sms()).isPresent();
        assertThat(configuration.push()).isPresent();
        assertThat(configuration.slack()).isPresent();
    }

    @SpringBootApplication
    static class TestApp {
        // Minimal app — just enables auto-configuration.
    }
}