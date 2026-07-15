package pe.edu.nova.java.starters.notifications.spring.boot;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import pe.edu.nova.java.libs.notifications.application.facade.NotificationFacade;
import pe.edu.nova.java.libs.notifications.domain.model.EmailNotification;
import pe.edu.nova.java.libs.notifications.domain.result.NotificationResult;
import pe.edu.nova.java.libs.notifications.domain.vo.EmailAddress;
import pe.edu.nova.java.libs.notifications.domain.vo.MessageBody;
import pe.edu.nova.java.libs.notifications.domain.vo.Subject;
import pe.edu.nova.java.libs.notifications.infrastructure.configuration.NotificationConfiguration;

/**
 * Integration test for the Spring Boot starter when
 * {@code nova.notifications.*} is configured with the email channel.
 *
 * <p>Boots a real Spring application context via {@code @SpringBootTest},
 * which exercises the full
 * {@link org.springframework.boot.autoconfigure.AutoConfiguration} chain
 * (binding, {@code @ConditionalOnMissingBean}, etc.).
 */
@SpringBootTest(classes = SpringBootEnabledIntegrationTest.TestApp.class, properties = {
        "nova.notifications.email.provider=SENDGRID",
        "nova.notifications.email.api-key=test-api-key",
        "nova.notifications.email.default-sender=no-reply@example.com",
        "nova.notifications.resilience.max-attempts=1"
})
class SpringBootEnabledIntegrationTest {

    @Autowired
    private NotificationFacade facade;

    @Autowired
    private NotificationConfiguration configuration;

    @Test
    void starterAutoWiresTheNotificationFacadeBean() {
        assertThat(facade).isNotNull();
    }

    @Test
    void configurationBeanHasEmailChannelFromProperties() {
        assertThat(configuration.email()).isPresent();
        assertThat(configuration.email().get().provider().name()).isEqualTo("SENDGRID");
        assertThat(configuration.email().get().apiKey()).isEqualTo("test-api-key");
    }

    @Test
    void otherChannelsAreNotConfigured() {
        assertThat(configuration.sms()).isEmpty();
        assertThat(configuration.push()).isEmpty();
        assertThat(configuration.slack()).isEmpty();
    }

    @Test
    void sendAnEmailAndGetASentResult() {
        EmailNotification email = EmailNotification.builder()
                .from(new EmailAddress("no-reply@example.com"))
                .to(new EmailAddress("customer@example.com"))
                .subject(new Subject("hi"))
                .body(new MessageBody("body"))
                .build();

        NotificationResult result = facade.send(email);

        assertThat(result.isSent()).isTrue();
        assertThat(result.providerMessageId()).isPresent();
    }

    @SpringBootApplication
    static class TestApp {
        // Minimal app — just enables auto-configuration.
    }
}