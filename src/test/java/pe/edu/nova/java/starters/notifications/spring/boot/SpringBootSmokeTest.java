package pe.edu.nova.java.starters.notifications.spring.boot;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import pe.edu.nova.java.libs.notifications.application.facade.NotificationFacade;
import pe.edu.nova.java.libs.notifications.domain.model.EmailNotification;
import pe.edu.nova.java.libs.notifications.domain.result.NotificationResult;
import pe.edu.nova.java.libs.notifications.domain.vo.EmailAddress;
import pe.edu.nova.java.libs.notifications.domain.vo.MessageBody;
import pe.edu.nova.java.libs.notifications.domain.vo.Subject;
import pe.edu.nova.java.libs.notifications.infrastructure.configuration.ResilienceConfiguration;

/**
 * Smoke test: when the starter is on the classpath and a single channel is
 * configured, the {@code NotificationFacade} bean is auto-wired and works
 * end-to-end (sends a simulated email and returns a SENT result).
 */
@SpringBootTest(classes = SpringBootSmokeTest.TestApp.class, properties = {
        "nova.notifications.email.provider=SENDGRID",
        "nova.notifications.email.api-key=test-api-key",
        "nova.notifications.email.default-sender=no-reply@example.com",
        "nova.notifications.resilience.max-attempts=1"
})
class SpringBootSmokeTest {

    @Autowired
    private NotificationFacade facade;

    @Autowired
    private ApplicationContext context;

    @Test
    void starterAutoWiresTheNotificationFacadeBean() {
        assertThat(facade).isNotNull();
        assertThat(context.getBean(NotificationFacade.class)).isSameAs(facade);
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
