package uk.gov.justice.digital.hmpps.personalrelationships.config

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import io.swagger.v3.core.util.PrimitiveType
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import io.swagger.v3.oas.models.tags.Tag
import jakarta.annotation.PostConstruct
import org.openapitools.jackson.nullable.JsonNullableModule
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer
import org.springframework.boot.info.BuildProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder

@Configuration
class OpenApiConfiguration(buildProperties: BuildProperties) {

  private val version: String = buildProperties.version

  @Bean
  fun customOpenAPI(buildProperties: BuildProperties): OpenAPI? = OpenAPI()
    .components(
      Components().addSecuritySchemes(
        "bearer-jwt",
        SecurityScheme()
          .type(SecurityScheme.Type.HTTP)
          .scheme("bearer")
          .bearerFormat("JWT")
          .`in`(SecurityScheme.In.HEADER)
          .name("Authorization"),
      ),
    )
    .info(
      Info()
        .title("Personal Relationships API")
        .version(version)
        .description("API for the management of contacts, their relationships to prisoners and restrictions.")
        .contact(
          Contact()
            .name("HMPPS Digital Studio")
            .email("feedback@digital.justice.gov.uk"),
        ),
    )
    .tags(
      listOf(
        Tag().apply {
          name("Contacts")
          description("Endpoints for creating and managing contacts.")
        },
        Tag().apply {
          name("Prisoner relationships")
          description("Endpoints for creating and managing the relationships between contacts and prisoners.")
        },
        Tag().apply {
          name("Restrictions")
          description(
            """
            Endpoints for creating and managing restrictions. Two kinds of restrictions are supported:
             - Estate wide restrictions (known as global or contact restrictions), which apply to all the contact's relationships.
             - Prisoner-contact restrictions, which apply to a specific relationship between a prisoner and a contact.
            There are also restrictions that can apply to a prisoner (and all of their relationships) but these are not supported here.
            """.trimIndent(),
          )
        },
        Tag().apply {
          name("Reference data")
          description("Endpoints for accessing the reference data used in this service")
        },
        Tag().apply {
          name("Migrate and sync")
          description("Endpoints for migrating and synchronising data to and from NOMIS")
        },
        Tag().apply {
          name("Sync admin")
          description("Endpoints use by synchronisation for administrative purposes")
        },
      ),
    )
    .addSecurityItem(SecurityRequirement().addList("bearer-jwt", listOf("read", "write")))
    .servers(
      listOf(
        Server().apply {
          url("/")
          description("Default - This environment")
        },
        Server().apply {
          url("https://personal-relationships-api-dev.hmpps.service.justice.gov.uk")
          description("Development")
        },
        Server().apply {
          url("https://personal-relationships-api-preprod.hmpps.service.justice.gov.uk")
          description("Pre-production")
        },
        Server().apply {
          url("https://personal-relationships-api.hmpps.service.justice.gov.uk")
          description("Production")
        },
      ),
    )

  @PostConstruct
  fun enableLocalTimePrimitiveType() {
    PrimitiveType.enablePartialTime()
  }

  @Bean
  fun jsonNullableModule() = JsonNullableModule()

  @Bean
  fun jsonCustomizer(): Jackson2ObjectMapperBuilderCustomizer = Jackson2ObjectMapperBuilderCustomizer { builder: Jackson2ObjectMapperBuilder ->
    builder.serializationInclusion(JsonInclude.Include.NON_NULL)
    builder.featuresToEnable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
  }
}
