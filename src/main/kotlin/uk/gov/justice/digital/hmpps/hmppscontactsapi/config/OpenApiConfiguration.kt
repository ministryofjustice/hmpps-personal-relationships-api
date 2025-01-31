package uk.gov.justice.digital.hmpps.hmppscontactsapi.config

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
        .description("API for management of contacts and their relationships to prisoners including restrictions that apply to the contact or relationship specifically.")
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
          description("APIs relating to creating and managing a contact")
        },
        Tag().apply {
          name("Prisoner Contact Relationship")
          description("APIs relating to creating and managing relationships between contacts and prisoners")
        },
        Tag().apply {
          name("Restrictions")
          description(
            """
            APIs relating to creating and managing restrictions. Two kinds of restrictions are supported in this API
            
             - Estate wide restrictions, a.k.a global and contact restrictions. These apply to all of a contacts relationships.
             - Prisoner-contact restrictions. These apply to a relationship between a specific prisoner and contact. 
             
            There are also restrictions that can apply to a prisoner and all of their relationships but those are not supported by this API.
            """.trimIndent(),
          )
        },
        Tag().apply {
          name("Organisation")
          description("APIs relating to creating and managing a organisation")
        },
        Tag().apply {
          name("Reference Data")
          description("APIs relating to reference data used in the service")
        },
        Tag().apply {
          name("Sync & Migrate")
          description("APIs relating to data sync and migration with NOMIS")
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
