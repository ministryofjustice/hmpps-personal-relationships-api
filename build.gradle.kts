import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "7.0.0"
  id("org.openapi.generator") version "7.11.0"
  kotlin("plugin.spring") version "2.1.10"
  kotlin("plugin.jpa") version "2.1.10"
}

allOpen {
  annotations(
    "javax.persistence.Entity",
    "javax.persistence.MappedSuperclass",
    "javax.persistence.Embeddable",
  )
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

dependencies {
  // Spring boot dependencies
  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter:1.1.1")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:5.2.2")
  implementation("org.springframework.boot:spring-boot-starter-validation")
  implementation("org.openapitools:jackson-databind-nullable:0.2.6")

  // CSV dependencies
  implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-csv:2.18.2")

  // Database dependencies
  runtimeOnly("org.flywaydb:flyway-database-postgresql")
  runtimeOnly("org.postgresql:postgresql:42.7.5")

  // OpenAPI
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.4")

  implementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations:2.12.0")

  // Test dependencies
  testImplementation("io.jsonwebtoken:jjwt-impl:0.12.6")
  testImplementation("io.jsonwebtoken:jjwt-jackson:0.12.6")
  testImplementation("net.javacrumbs.json-unit:json-unit:4.1.0")
  testImplementation("net.javacrumbs.json-unit:json-unit-assertj:4.1.0")
  testImplementation("net.javacrumbs.json-unit:json-unit-json-path:4.1.0")
  testImplementation("org.awaitility:awaitility-kotlin:4.2.2")
  testImplementation("org.mockito:mockito-inline:5.2.0")
  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testImplementation("org.springframework.security:spring-security-test")
  testImplementation("org.testcontainers:postgresql:1.20.4")
  testImplementation("org.testcontainers:localstack:1.20.4")
  testImplementation("org.wiremock:wiremock-standalone:3.11.0")
  testImplementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter-test:1.1.1")
}

kotlin {
  jvmToolchain(21)
}

tasks {
  withType<KotlinCompile> {
    compilerOptions.jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21
  }
}
