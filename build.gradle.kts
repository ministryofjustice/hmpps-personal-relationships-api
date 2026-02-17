import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.openapitools.generator.gradle.plugin.tasks.GenerateTask

plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "10.0.3"
  id("org.openapi.generator") version "7.20.0"
  id("io.gatling.gradle") version "3.14.9"
  kotlin("plugin.spring") version "2.3.0"
  kotlin("plugin.jpa") version "2.3.0"
}

allOpen {
  annotations(
    "javax.persistence.Entity",
    "javax.persistence.MappedSuperclass",
    "javax.persistence.Embeddable",
  )
}

configurations
  .matching { !it.name.startsWith("gatling") }
  .all {
    resolutionStrategy.eachDependency {
      if (requested.group == "io.netty" &&
        !requested.name.startsWith("netty-tcnative")
      ) {
        useVersion("4.2.10.Final")
        because("Fix CVE-2025-67735")
      }
    }
    exclude(group = "tools.jackson.core")
  }

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

// Local suppressions
dependencyCheck {
  suppressionFiles.add("dependency-check-suppress-json.xml")
}

dependencies {
  // Spring boot dependencies

  implementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter:2.0.0")
  implementation("org.springframework.boot:spring-boot-starter-webclient")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-flyway")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:6.0.0")
  implementation("io.sentry:sentry-spring-boot-starter-jakarta:8.26.0")
  implementation("org.springframework.boot:spring-boot-starter-validation")
  implementation("org.openapitools:jackson-databind-nullable:0.2.9")
  implementation("org.apache.logging.log4j:log4j-api:2.25.3")
  implementation("io.github.cdimascio:dotenv-kotlin:6.4.1")

  // CSV dependencies
  implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-csv:2.20.1")

  // Database dependencies
  runtimeOnly("org.flywaydb:flyway-database-postgresql")
  runtimeOnly("org.postgresql:postgresql:42.7.8")
  implementation("org.hibernate.orm:hibernate-envers")
  implementation("org.springframework.data:spring-data-envers")

  // OpenAPI
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.1")

  implementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations:2.21.0")
  implementation("org.springframework.boot:spring-boot-jackson2")
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.21.0")

  // Test dependencies
  testImplementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter-test:2.0.0")
  testImplementation("org.springframework.boot:spring-boot-starter-webclient-test")

  testImplementation("org.springframework.boot:spring-boot-starter-webflux-test")
  testImplementation("org.wiremock:wiremock-standalone:3.13.2")
  testImplementation("io.swagger.parser.v3:swagger-parser:2.1.37") {
    testImplementation("org.mockito:mockito-inline:5.2.0")
    exclude(group = "io.swagger.core.v3")
  }
  testImplementation("org.testcontainers:testcontainers-postgresql:2.0.3")
  testImplementation("org.testcontainers:testcontainers-localstack:2.0.3")
  testImplementation("org.awaitility:awaitility-kotlin:4.3.0")
  testImplementation("io.opentelemetry:opentelemetry-sdk-testing:1.56.0")
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
  modelPackage.set("uk.gov.justice.digital.hmpps.personalrelationships.client.organisationsapi.model")
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
