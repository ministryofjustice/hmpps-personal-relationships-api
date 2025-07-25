import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.openapitools.generator.gradle.plugin.tasks.GenerateTask

plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "8.3.3"
  id("org.openapi.generator") version "7.14.0"
  id("io.gatling.gradle") version "3.14.3.3"
  kotlin("plugin.spring") version "2.2.0"
  kotlin("plugin.jpa") version "2.2.0"
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
  implementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter:1.4.10")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:5.4.9")
  implementation("io.sentry:sentry-spring-boot-starter-jakarta:8.17.0")
  implementation("org.springframework.boot:spring-boot-starter-validation")
  implementation("org.openapitools:jackson-databind-nullable:0.2.6")

  // CSV dependencies
  implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-csv:2.19.2")

  // Database dependencies
  runtimeOnly("org.flywaydb:flyway-database-postgresql")
  runtimeOnly("org.postgresql:postgresql:42.7.7")

  // OpenAPI
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.9")

  implementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations:2.18.0")

  // Test dependencies
  testImplementation("io.jsonwebtoken:jjwt-impl:0.12.6")
  testImplementation("io.jsonwebtoken:jjwt-jackson:0.12.6")
  testImplementation("net.javacrumbs.json-unit:json-unit:4.1.1")
  testImplementation("net.javacrumbs.json-unit:json-unit-assertj:4.1.1")
  testImplementation("net.javacrumbs.json-unit:json-unit-json-path:4.1.1")
  testImplementation("org.awaitility:awaitility-kotlin:4.3.0")
  testImplementation("org.mockito:mockito-inline:5.2.0")
  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testImplementation("org.springframework.security:spring-security-test")
  testImplementation("org.testcontainers:postgresql:1.21.3")
  testImplementation("org.testcontainers:localstack:1.21.3")
  testImplementation("org.wiremock:wiremock-standalone:3.13.1")
  testImplementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter-test:1.4.10")
  testImplementation("io.opentelemetry:opentelemetry-sdk-testing:1.52.0")
}

tasks {
  withType<KotlinCompile> {
    dependsOn("buildOrganisationApiModel")
    compilerOptions.jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21
    compilerOptions.freeCompilerArgs.add("-Xannotation-default-target=param-property")
  }
}

dependencyCheck {
  suppressionFiles.add("dependencyCheck/suppression.xml")
}

val configValues = mapOf(
  "dateLibrary" to "java8-localdatetime",
  "serializationLibrary" to "jackson",
  "useBeanValidation" to "false",
  "enumPropertyNaming" to "UPPERCASE",
)

val buildDirectory: Directory = layout.buildDirectory.get()

tasks.register("buildOrganisationApiModel", GenerateTask::class) {
  generatorName.set("kotlin")
  inputSpec.set("openapi-specs/hmpps-organisations-api.json")
  outputDir.set("$buildDirectory/generated/organisationsapi")
  modelPackage.set("uk.gov.justice.digital.hmpps.hmppscontactsapi.client.organisationsapi.model")
  configOptions.set(configValues)
  globalProperties.set(mapOf("models" to ""))
}

tasks.named("runKtlintCheckOverMainSourceSet") {
  dependsOn("buildOrganisationApiModel")
}

kotlin {
  jvmToolchain(21)
  sourceSets["main"].apply {
    kotlin.srcDir("$buildDirectory/generated/organisationsapi/src/main/kotlin")
  }
}

ktlint {
  filter {
    exclude {
      it.file.path.contains("organisationsapi/model/")
    }
  }
}
