package pe.edu.nova.java.starters.notifications.spring.boot;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import pe.edu.nova.java.libs.notifications.application.facade.NotificationFacade;
import pe.edu.nova.java.libs.notifications.infrastructure.configuration.NotificationConfiguration;

/**
 * Integration test for the Spring Boot starter when
 * {@code nova.notifications.enabled=false}.
 *
 * <p>Verifies the Spring-Boot-idiomatic behavior:
 * {@link org.springframework.boot.autoconfigure.condition.ConditionalOnProperty}
 * causes the entire {@code NotificationsAutoConfiguration} to back off, so
 * neither {@link NotificationConfiguration} nor {@link NotificationFacade}
 * is auto-wired. This is the contract that distinguishes the Spring Boot
 * starter from the Quarkus and Micronaut starters, which produce a no-op
 * facade in the same situation.
 *
 * <p>Applications that still want a {@code NotificationFacade} bean while
 * the starter is disabled can declare one themselves; the starter will not
 * interfere (no {@code @ConditionalOnMissingBean} conflicts because the
 * starter beans are not created at all).
 */
@SpringBootTest(classes = SpringBootDisabledIntegrationTest.TestApp.class, properties = {
        "nova.notifications.enabled=false",
        "nova.notifications.email.provider=SENDGRID",
        "nova.notifications.email.api-key=test-api-key",
        "nova.notifications.email.default-sender=no-reply@example.com"
})
class SpringBootDisabledIntegrationTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void starterDoesNotAutoWireNotificationConfiguration() {
        assertThat(context.getBeansOfType(NotificationConfiguration.class)).isEmpty();
    }

    @Test
    void starterDoesNotAutoWireNotificationFacade() {
        assertThat(context.getBeansOfType(NotificationFacade.class)).isEmpty();
    }

    @Test
    void attemptingToInjectNotificationFacadeFails() {
        assertThatExceptionOfTypeNoSuchBean();
    }

    private void assertThatExceptionOfTypeNoSuchBean() {
        try {
            context.getBean(NotificationFacade.class);
            assertThat(false).as("expected NoSuchBeanDefinitionException").isTrue();
        } catch (NoSuchBeanDefinitionException expected) {
            assertThat(expected.getBeanType()).isEqualTo(NotificationFacade.class);
        }
    }

    @SpringBootApplication
    static class TestApp {
        // Minimal app — just enables auto-configuration.
    }
}