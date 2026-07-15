# nova-java-notifications-spring-boot-starter

Spring Boot 4.1.0 auto-configuration for the
[`nova-notifications`](../nova-java-notifications) library. When the
starter is on the classpath, a `NotificationFacade` bean is auto-wired
into the application context and ready to inject anywhere.

This is the **Nivel 1 → Nivel 2** adapter for the Spring Boot
ecosystem in Nova's meta-framework
(`docs/adrs/shared/ADR-001-arquitectura-meta-framework-cinco-niveles.md`).
The library is framework-agnostic; the starter is the only piece that
knows about Spring Boot.

## Install

The starter is published to GitHub Packages (and locally via
`mavenLocal` for development). The pure library is brought in
transitively.

```kotlin
// build.gradle.kts
dependencies {
    implementation("pe.edu.nova.java.starters:nova-notifications-spring-boot-starter:1.0.0")
}
```

```xml
<!-- pom.xml -->
<dependency>
    <groupId>pe.edu.nova.java.starters</groupId>
    <artifactId>nova-notifications-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Quick start

`application.yml`:

```yaml
nova.notifications:
  enabled: true
  email:
    provider: sendgrid
    api-key: ${SENDGRID_API_KEY}
    default-sender: no-reply@example.com
  resilience:
    max-attempts: 3
```

`NotificationsController.java`:

```java
@RestController
@RequiredArgsConstructor
public class NotificationsController {
    private final NotificationFacade facade;

    @GetMapping("/api/notifications/email/welcome")
    public NotificationResult welcome() {
        return facade.send(EmailNotification.builder()
                .from(new EmailAddress("no-reply@example.com"))
                .to(new EmailAddress("customer@example.com"))
                .subject(new Subject("Welcome"))
                .body(new MessageBody("Thanks for signing up to Nova."))
                .build());
    }
}
```

The `NotificationFacade` is auto-wired by
`NotificationsAutoConfiguration` (registered via
`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`).

## Configuration reference

| Property | Type | Default | Description |
|---|---|---|---|
| `nova.notifications.enabled` | `boolean` | `true` | Master switch. When `false`, the entire auto-configuration backs off and no `NotificationFacade` bean is created (idiomatic Spring Boot `@ConditionalOnProperty` behavior — see "Disabling the library" below). |
| `nova.notifications.email.provider` | `String` | _(none)_ | `sendgrid` or `mailgun`. Required to enable the email channel. |
| `nova.notifications.email.api-key` | `String` | _(none)_ | Provider API key. Inject from env / vault in production. |
| `nova.notifications.email.default-sender` | `String` | _(none)_ | Verified `EmailAddress`. |
| `nova.notifications.sms.provider` | `String` | `twilio` | Currently only `twilio`. |
| `nova.notifications.sms.account-sid` | `String` | _(none)_ | Twilio account SID. |
| `nova.notifications.sms.auth-token` | `String` | _(none)_ | Twilio auth token. |
| `nova.notifications.sms.from-number` | `String` | _(none)_ | Verified Twilio phone number in E.164 format. |
| `nova.notifications.push.provider` | `String` | `firebase` | Currently only `firebase`. |
| `nova.notifications.push.project-id` | `String` | _(none)_ | Firebase project ID. |
| `nova.notifications.push.server-key` | `String` | _(none)_ | Firebase server key (legacy FCM). |
| `nova.notifications.slack.default-webhook-url` | `String` | _(none)_ | Slack Incoming Webhook URL. |
| `nova.notifications.resilience.max-attempts` | `int` | `3` | Retry attempts for transient errors. `1` disables retry. |
| `nova.notifications.resilience.initial-backoff-millis` | `long` | `200` | Initial backoff between retries. |
| `nova.notifications.resilience.circuit-failure-threshold` | `int` | `5` | Failures within the window that trip the circuit. |
| `nova.notifications.resilience.circuit-open-duration-seconds` | `long` | `30` | How long the circuit stays OPEN before HALF_OPEN. |
| `nova.notifications.resilience.rate-limit-permits-per-second` | `int` | `0` | Token-bucket capacity. `0` disables rate limiting. |

Each channel is independent: only configure the ones you use. An empty
or partial config for a channel is treated as "channel not enabled" and
the corresponding `SendNotificationPort` is not registered; the
library's `NotificationFacade.create()` throws a `ConfigurationException`
if no channel is enabled.

## Disabling the library

```yaml
nova.notifications.enabled: false
```

The entire `NotificationsAutoConfiguration` backs off
(`@ConditionalOnProperty(havingValue="true")`). This means **no
`NotificationConfiguration` bean and no `NotificationFacade` bean are
created**. Attempting to inject `NotificationFacade` results in
`NoSuchBeanDefinitionException`.

This is different from the Quarkus and Micronaut starters, which
produce a no-op facade (every `send` returns `FAILED` with
`ErrorCode.DISABLED`) in the same situation. The Spring Boot
auto-configuration follows the Spring-idiomatic pattern of "not
registering" the bean. To get the no-op behavior, the user can
declare their own `NotificationFacade` bean (the starter does not
interfere because the auto-config is not active).

## API reference

The starter does NOT add new public types beyond the auto-configuration
class. The injected `NotificationFacade` and `NotificationConfiguration`
are pure-library types; see
[`nova-java-notifications/README.md`](../nova-java-notifications/README.md)
for the full API reference (domain model, value objects, result type,
events, resilience, templates, pub/sub).

## Testing

```bash
./gradlew check
```

The starter ships with 4 integration test classes (11 tests total) that
boot a real Spring `ApplicationContext` with the starter on the
classpath:

- `SpringBootEnabledIntegrationTest` — `@ConditionalOnProperty` passes,
  bean is wired, end-to-end send returns SENT.
- `SpringBootAllChannelsIntegrationTest` — all four channels configured
  simultaneously are present in the built `NotificationConfiguration`.
- `SpringBootDisabledIntegrationTest` — `enabled=false` causes the
  entire auto-config to back off (no beans of either type).
- `SpringBootResilienceIntegrationTest` — every individual
  `nova.notifications.resilience.*` property is propagated to the
  library's `ResilienceConfiguration`.

## Build

```bash
./gradlew build              # compile + test + jar
./gradlew publishToMavenLocal  # for the demo / other consumers
```

The starter depends on Spring Boot types at `compileOnly` scope
(`org.springframework.boot:spring-boot-autoconfigure`,
`spring-boot-starter-validation`, `spring-context`) so the consumer
controls the Spring Boot version. The Spring Boot 4.1.0 BOM is the
current pin (locked at the workspace level).

## Versioning

- `1.0.0` — initial release aligned with `nova-notifications:1.0.0`.
- Property prefix: `nova.notifications.*` (Nova convention; older
  legacy starters still use `galaxy-training.*` — migration tracked
  in the meta-framework backlog).
- Java 25 toolchain (the workspace's standard pin).

## Related

- [`nova-java-notifications`](../nova-java-notifications) — pure library.
- [`nova-java-notifications-quarkus-extension`](../nova-java-notifications-quarkus-extension) — Quarkus colloquial extension.
- [`nova-java-notifications-micronaut-module`](../nova-java-notifications-micronaut-module) — Micronaut colloquial module.
- [`examples/demo-notifications-spring-boot`](../../examples/demo-notifications-spring-boot) — example app consuming this starter.

---

## AI Assistance Attribution

This work was created through human-AI collaboration. The human author
(Angel Eduardo Hincho Jove, `ahincho@unsa.edu.pe`, UNSA) retains full
responsibility for the final artifact.

**AI tools used**: GitHub Copilot (Claude Opus 4.8, Sonnet 5), MiniMax
(MiniMax-M3 via paid Token Plan), OpenCode (the interactive CLI
harness used to host the session), NotebookLM, Perplexity.
Methodology: OpenSpec spec-driven development.

**Important legal note**: this artifact is **not an "AI system"** under
Article 3(1) of Regulation (EU) 2024/1689 (the EU AI Act). Article 50
transparency obligations therefore do not directly apply. This
disclosure is made voluntarily, aligned with UNESCO Principle 6
(transparency and explainability) and the R-AI requirement of the
originating challenge.

The canonical, full AI-ATTRIBUTION.md (covering the entire Nova
Platform workspace) lives at the workspace root:
[`../../AI-ATTRIBUTION.md`](../../AI-ATTRIBUTION.md).
