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

## What was built for this technical challenge

This repository is the **Spring Boot adapter** (Nivel 2) of the
Nova Platform notifications module. It exposes the pure library
(`pe.edu.nova.java.libs:nova-notifications:1.0.0`, in the sibling
`nova-java-notifications` repo) as a Spring Boot auto-configuration
starter, ready to inject anywhere in a Spring Boot 4.1 application.

### Role in the Nova Platform

- **Nivel**: 2 (framework adapter).
- **Depends on**: `pe.edu.nova.java.libs:nova-notifications:1.0.0`
  (Nivel 1, in the sibling repo).
- **Consumed by**: `examples/demo-notifications-spring-boot` (Nivel 3,
  in `examples/`).

### What this repository delivers

- **Spring Boot 4.1.0 auto-configuration** under
  `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.
  Drop the starter on the classpath and a fully wired
  `NotificationFacade` bean is available via constructor injection.
- **`@ConfigurationProperties` binding** under the `nova.notifications.*`
  prefix, mirroring the same property names as the Quarkus and
  Micronaut adapters in this module (one namespace, three idiomatic
  binding strategies).
- **REST exposure** of the email / SMS / push / Slack send via
  Spring MVC controllers backed by `RestClient` (Spring 7 / Spring
  Boot 4's preferred replacement for `RestTemplate`).
- **`NotificationAutoConfiguration`** auto-disables itself when
  `nova.notifications.enabled=false`, returning a no-op facade
  (every `send` returns `FAILED` with `ErrorCode.DISABLED`) — no
  startup `ConfigurationException`.
- **Health indicator** (`NotificationHealthIndicator`) reporting the
  state of each configured channel (PROVIDER_OK / PROVIDER_DISABLED /
  PROVIDER_NOT_CONFIGURED).
- **24 unit tests + 8 Spring Boot integration tests** that boot a
  minimal `@SpringBootTest` context and verify auto-wiring, the
  `nova.notifications.*` binding, the disabled-mode no-op facade,
  and the health indicator.

### Quality gates verified

- **Gradle build + checkstyle + 24 + 8 tests = green** in the CI/CD
  pipeline (`ahincho/nova-java-notifications-spring-boot-starter`
  GitHub Actions).
- **Published to GitHub Packages** with full sources + javadoc jars:
  `pe.edu.nova.java.starters:nova-notifications-spring-boot-starter:1.0.0`.

### How to reproduce the build

```bash
./gradlew clean build           # compile + test + jar
./gradlew publishToMavenLocal   # for the demo and other consumers
```

### How to consume from a Spring Boot app

```kotlin
// build.gradle.kts
dependencies {
    implementation("pe.edu.nova.java.starters:nova-notifications-spring-boot-starter:1.0.0")
}
```

```java
@RestController
@RequiredArgsConstructor
public class NotificationsController {
    private final NotificationFacade facade; // auto-wired by the starter

    @GetMapping("/api/notifications/email/welcome")
    public NotificationResult welcome() {
        return facade.send(EmailNotification.builder()
                .from(new EmailAddress("no-reply@example.com"))
                .to(new EmailAddress("customer@example.com"))
                .subject(new Subject("Welcome"))
                .body(new MessageBody("Thanks for signing up."))
                .build());
    }
}
```

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

This repository was produced through human-AI collaboration. The human
author (Angel Eduardo Hincho Jove, `ahincho@unsa.edu.pe`, Universidad
Nacional de San Agustín de Arequipa — UNSA) retains full responsibility
for the final artifact and for every commit accepted into the repository.

### Challenge context

This work was produced in response to the technical challenge
described in `Reto-Tecnico-Backend.pdf`. Section 2.5 of the challenge
mandates an explicit AI disclosure in the README when AI is used. This
section fulfils that requirement (R-AI / **R**esponsible **AI**
disclosure) and is also aligned with:

- **Regulation (EU) 2024/1689** ("EU AI Act"), Article 3(1)
  (definition of "AI system") and Article 50 (transparency obligations
  for deployers of certain AI systems).
- **UNESCO Recommendation on the Ethics of Artificial Intelligence**
  (2021), adopted by 193 Member States, **Principle 6: Transparency and
  explainability**.

### AI tools used in this repository

| Tool | Provider | Model / Role | Access tier |
|---|---|---|---|
| GitHub Copilot | GitHub / Anthropic | Claude Opus 4.8, Claude Sonnet 5 (in-editor suggestions) | Licensed |
| MiniMax Token Plan | MiniMax | MiniMax-M3 (the model used for long-form generation and refactoring in the OpenCode session) | Paid (personal) |
| OpenCode | anomalyco (`opencode.ai`) | Interactive CLI harness — **not a model**, only the session/UI | Free (CLI) |
| OpenSpec | Fission AI | Spec-driven development framework (used for the meta-framework backlog) | Licensed |
| NotebookLM | Google | Gemini (cross-document synthesis of the challenge PDF and ADRs) | Free |
| Perplexity | Perplexity AI | Sonar / Pro Search (lookup of latest framework versions and release dates) | Free |

> *Important distinction*: OpenCode is the interactive CLI harness in
> which the AI-assisted development session took place (with MiniMax-M3
> as the underlying model). OpenCode is **not a model** and **not a
> license/subscription manager** — the subscription providing access to
> the model is the **MiniMax Token Plan** listed above. The two rows
> are kept deliberately separate so that anyone reading the disclosure
> can identify exactly which entity provides the model and which entity
> provides the session/UI.

### Scope of AI assistance in this repository

- Drafting the **initial code skeletons** (sealed interfaces, value
  objects, port interfaces, provider stubs).
- Drafting **unit tests** for value objects, the error hierarchy, the
  template resolver, the rate-limiter, the circuit-breaker state
  machine, and the i18n message bundle (Spanish / English).
- **Documentation drafts** of this README and the inline Javadoc.
- **Build infrastructure** snippets for the reusable CI/CD workflows
  in `ahincho/nova-devops`.
- **Cross-checking** the published provider documentation (SendGrid,
  Mailgun, Twilio, Firebase) for the authentication-header / payload
  shape used in the per-provider adapters (no live API calls are made).

### Human contributions (author: Angel Eduardo Hincho Jove)

The following decisions and artifacts are **authored and approved by
the human**, not delegated to AI:

- **Architecture**: hexagonal / ports-and-adapters layout, framework
  isolation in the core library, the five-level meta-framework
  (Nivel 1 = pure library, Nivel 2 = starter/extension,
  Nivel 3 = application) per `ADR-001` and `ADR-015`.
- **Scope**: which channels and providers are in scope for the
  challenge (Email / SMS / Push mandatory, Slack optional) and which
  features are deferred.
- **Version pinning**: Java 25, Spring Boot 4.1.0, Quarkus 3.33.2.1
  LTS, Micronaut 5.0.4, Gradle 9.5.1, Maven 3.9.x. Each pin was
  cross-checked against the latest stable release and the framework
  vendor's LTS roadmap.
- **Quality gates**: 80 % JaCoCo coverage, Checkstyle Nova style,
  ArchUnit test enforcing zero framework leakage in the core library.
- **Build infrastructure**: Maven for the core (T-02 of the challenge
  mandates Maven), Gradle 9.5.1 for the framework starters and demos
  (consistency with the rest of the Nova Platform).
- **Final review and approval** of every commit, including a final
  end-to-end run of `./mvnw verify` and `./gradlew build` against
  JDK 25 before tagging the release.
- **Legal classification**: the determination that the artifacts
  shipped here (a deterministic Java library + framework adapters +
  example apps) are **not "AI systems"** under EU AI Act Article 3(1)
  and therefore do not directly attract Article 50 obligations (see
  the legal clarification below).

### Methodology

The work followed a **Spec-Driven Development** approach using
OpenSpec:

1. Requirements were captured as structured specifications before any
   code was written (`CHALLENGE.md`, `REQUIREMENTS.md`, the ADRs).
2. AI assistance operated against those specifications, not in the
   abstract.
3. The human author reviewed and approved each artifact (build, test,
   commit) before it was accepted into the repository.

### Legal clarification (EU AI Act)

A deterministic Java notifications library does not "infer" outputs,
does not generate predictions / recommendations / decisions, and does
not exhibit autonomy or adaptiveness after deployment. Therefore the
artifacts shipped in this repository are **not "AI systems"** within
the meaning of Article 3(1) of Regulation (EU) 2024/1689 (the EU AI
Act), and Article 50 does not directly impose obligations on them.

This disclosure is nevertheless made:

- **By contractual / academic requirement**: per the R-AI requirement
  of the originating technical challenge (challenge PDF §2.5).
- **Voluntarily**, in alignment with the spirit of the EU AI Act
  transparency principles and UNESCO Principle 6.
- **In the interest of authorship transparency** for the open-source
  community.

### Canonical disclosure

The full Nova Platform AI attribution (covering every repository in
the workspace — pure libraries, framework adapters, demos, tooling
and documentation) lives in a single canonical file at the workspace
root:

[`../../AI-ATTRIBUTION.md`](../../AI-ATTRIBUTION.md)

This per-repository section is a compact summary that points back to
that canonical file as the source of truth for the full disclosure;
the legal analysis and the human-contributions audit are not
duplicated in every repository on purpose.

### Change log

- **2026-07-15** — Initial disclosure created.