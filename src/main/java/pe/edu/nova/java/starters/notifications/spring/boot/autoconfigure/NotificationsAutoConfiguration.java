package pe.edu.nova.java.starters.notifications.spring.boot.autoconfigure;

import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import pe.edu.nova.java.libs.notifications.application.facade.NotificationFacade;
import pe.edu.nova.java.libs.notifications.application.port.out.NotificationEventPublisherPort;
import pe.edu.nova.java.libs.notifications.domain.vo.EmailAddress;
import pe.edu.nova.java.libs.notifications.domain.vo.SlackWebhookUrl;
import pe.edu.nova.java.libs.notifications.infrastructure.configuration.EmailConfiguration;
import pe.edu.nova.java.libs.notifications.infrastructure.configuration.EmailProvider;
import pe.edu.nova.java.libs.notifications.infrastructure.configuration.NotificationConfiguration;
import pe.edu.nova.java.libs.notifications.infrastructure.configuration.PushConfiguration;
import pe.edu.nova.java.libs.notifications.infrastructure.configuration.PushProvider;
import pe.edu.nova.java.libs.notifications.infrastructure.configuration.ResilienceConfiguration;
import pe.edu.nova.java.libs.notifications.infrastructure.configuration.SlackConfiguration;
import pe.edu.nova.java.libs.notifications.infrastructure.configuration.SmsConfiguration;
import pe.edu.nova.java.libs.notifications.infrastructure.configuration.SmsProvider;

/**
 * Spring Boot auto-configuration for the Nova Notifications library.
 *
 * <p>When the starter is on the classpath, the application only needs to:
 * <ol>
 *     <li>Declare a dependency on {@code nova-notifications-spring-boot-starter}.</li>
 *     <li>Configure credentials under {@code galaxy-training.notifications.*} in
 *         {@code application.yml} / {@code application.properties}.</li>
 *     <li>Inject {@code NotificationFacade} anywhere in the app.</li>
 * </ol>
 *
 * <p>The application is free to provide its own {@code NotificationConfiguration}
 * (or override individual beans like {@code NotificationEventPublisherPort}) and
 * Spring's {@code @ConditionalOnMissingBean} will back off — Open/Closed
 * Principle applied to Spring auto-configuration.
 */
@AutoConfiguration
@ConditionalOnClass(NotificationFacade.class)
@ConditionalOnProperty(prefix = "galaxy-training.notifications", name = "enabled",
        havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(NotificationsProperties.class)
public class NotificationsAutoConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationsAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean
    public NotificationConfiguration notificationConfiguration(
            NotificationsProperties properties,
            ObjectProvider<NotificationEventPublisherPort> customEventPublisher,
            ObjectProvider<Executor> customAsyncExecutor) {

        NotificationConfiguration.Builder builder = NotificationConfiguration.builder()
                .resilience(toResilienceConfiguration(properties.getResilience()));

        customEventPublisher.ifAvailable(builder::eventPublisher);
        customAsyncExecutor.ifAvailable(builder::asyncExecutor);

        NotificationsProperties.Email emailProps = properties.getEmail();
        if (hasAll(emailProps.getProvider(), emailProps.getApiKey(), emailProps.getDefaultSender())) {
            builder.email(EmailConfiguration.builder()
                    .provider(EmailProvider.valueOf(emailProps.getProvider().toUpperCase()))
                    .apiKey(emailProps.getApiKey())
                    .defaultSender(new EmailAddress(emailProps.getDefaultSender()))
                    .build());
        }

        NotificationsProperties.Sms smsProps = properties.getSms();
        if (hasAll(smsProps.getProvider(), smsProps.getAccountSid(), smsProps.getAuthToken(), smsProps.getFromNumber())) {
            builder.sms(SmsConfiguration.builder()
                    .provider(SmsProvider.valueOf(smsProps.getProvider().toUpperCase()))
                    .accountSid(smsProps.getAccountSid())
                    .authToken(smsProps.getAuthToken())
                    .fromNumber(smsProps.getFromNumber())
                    .build());
        }

        NotificationsProperties.Push pushProps = properties.getPush();
        if (hasAll(pushProps.getProvider(), pushProps.getProjectId(), pushProps.getServerKey())) {
            builder.push(PushConfiguration.builder()
                    .provider(PushProvider.valueOf(pushProps.getProvider().toUpperCase()))
                    .projectId(pushProps.getProjectId())
                    .serverKey(pushProps.getServerKey())
                    .build());
        }

        NotificationsProperties.Slack slackProps = properties.getSlack();
        if (slackProps.getDefaultWebhookUrl() != null && !slackProps.getDefaultWebhookUrl().isBlank()) {
            builder.slack(SlackConfiguration.builder()
                    .defaultWebhookUrl(SlackWebhookUrl.of(slackProps.getDefaultWebhookUrl()))
                    .build());
        }

        NotificationConfiguration configuration = builder.build();
        LOGGER.info("Nova Notifications auto-configuration initialized. Email configured: {}, SMS: {}, Push: {}, Slack: {}",
                configuration.email().isPresent(),
                configuration.sms().isPresent(),
                configuration.push().isPresent(),
                configuration.slack().isPresent());
        return configuration;
    }

    @Bean
    @ConditionalOnMissingBean
    public NotificationFacade notificationFacade(NotificationConfiguration configuration) {
        return NotificationFacade.create(configuration);
    }

    private static ResilienceConfiguration toResilienceConfiguration(NotificationsProperties.Resilience r) {
        return new ResilienceConfiguration(
                r.getMaxAttempts(),
                NotificationsProperties.durationOfMillis(r.getInitialBackoffMillis()),
                r.getCircuitFailureThreshold(),
                NotificationsProperties.durationOfSeconds(r.getCircuitOpenDurationSeconds()),
                r.getRateLimitPermitsPerSecond());
    }

    private static boolean hasAll(String... values) {
        for (String v : values) {
            if (v == null || v.isBlank()) {
                return false;
            }
        }
        return true;
    }
}
