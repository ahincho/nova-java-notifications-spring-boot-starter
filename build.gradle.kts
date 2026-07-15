plugins {
    id("java-library")
    id("maven-publish")
    jacoco
    checkstyle
}

group = "pe.edu.nova.java.starters"
version = findProperty("version") as String

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
    withSourcesJar()
    withJavadocJar()
}

repositories {
    mavenLocal()
    mavenCentral()
    // GitHub Packages of nova-java-notifications (the pure library this starter
    // adapts to Spring Boot). Without this entry, ./gradlew publish fails at
    // compileJava because it cannot resolve pe.edu.nova.java.libs:nova-notifications
    // from Maven Central (which only mirrors Maven-published artifacts, not
    // GitHub Packages). NOVA_PACKAGES_READ_TOKEN is a PAT with packages:read
    // scope; falls back to GITHUB_TOKEN if not set (GITHUB_TOKEN can read
    // packages within the same repo but not across repos, so the cross-repo
    // dependency on nova-java-notifications would fail without the PAT).
    maven {
        name = "GitHubPackages-NovaNotifications"
        url = uri("https://maven.pkg.github.com/ahincho/nova-java-notifications")
        val token = System.getenv("NOVA_PACKAGES_READ_TOKEN")
            ?: System.getenv("NOVA_RELEASE_PAT")
            ?: System.getenv("GITHUB_TOKEN")
        if (!token.isNullOrBlank()) {
            credentials {
                username = System.getenv("GITHUB_ACTOR") ?: "x-access-token"
                password = token
            }
        }
    }
}

val springBootVersion = "4.1.0"
val junitVersion = "6.0.0"
val assertjVersion = "3.26.3"
val mockitoVersion = "5.18.0"
val logbackVersion = "1.5.12"
val slf4jVersion = "2.0.16"

dependencies {
    // The pure library this starter adapts to Spring Boot.
    api("pe.edu.nova.java.libs:nova-notifications:1.0.0")

    // Spring Boot is a compile-only API: this starter exposes NotificationFacade
    // and properties to the auto-configuration machinery, but it does NOT pull
    // Spring into the pure library. Other frameworks (Quarkus, Micronaut) can
    // also have their own starters that depend on the same pure library.
    compileOnly("org.springframework.boot:spring-boot-autoconfigure:$springBootVersion")
    compileOnly("org.springframework.boot:spring-boot-starter-validation:$springBootVersion")
    compileOnly("org.springframework:spring-context:$springBootVersion")

    testImplementation("org.springframework.boot:spring-boot-starter:$springBootVersion")
    testImplementation("org.springframework.boot:spring-boot-starter-test:$springBootVersion")
    testImplementation("org.junit.jupiter:junit-jupiter:$junitVersion")
    testImplementation("org.junit.platform:junit-platform-launcher:$junitVersion")
    testImplementation("org.assertj:assertj-core:$assertjVersion")
    testImplementation("org.mockito:mockito-junit-jupiter:$mockitoVersion")
    testRuntimeOnly("ch.qos.logback:logback-classic:$logbackVersion")
    testRuntimeOnly("org.slf4j:slf4j-api:$slf4jVersion")
}

testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter()
        }
    }
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
        xml.outputLocation.set(layout.buildDirectory.file("reports/jacoco/test/jacocoTestReport.xml"))
    }
}

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.jacocoTestReport)
    violationRules {
        rule {
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.60".toBigDecimal()
            }
        }
    }
}

tasks.named("check") {
    dependsOn(tasks.jacocoTestCoverageVerification)
}

tasks.test {
    useJUnitPlatform()
    jvmArgs(
        "--add-opens", "java.base/java.time=ALL-UNNAMED",
        "--add-opens", "java.base/java.util=ALL-UNNAMED"
    )
    finalizedBy(tasks.jacocoTestReport)
}

tasks.javadoc {
    (options as StandardJavadocDocletOptions).apply {
        addStringOption("Xdoclint:all,-missing", "-quiet")
        encoding = "UTF-8"
        charSet = "UTF-8"
    }
}

checkstyle {
    toolVersion = "10.20.1"
    sourceSets = listOf(project.sourceSets.main.get())
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/ahincho/nova-java-notifications-spring-boot-starter")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
