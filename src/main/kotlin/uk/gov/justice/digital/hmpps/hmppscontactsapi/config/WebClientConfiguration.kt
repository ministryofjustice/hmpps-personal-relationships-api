package uk.gov.justice.digital.hmpps.hmppscontactsapi.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.hmpps.kotlin.auth.authorisedWebClient
import uk.gov.justice.hmpps.kotlin.auth.healthWebClient
import java.time.Duration

@Configuration
class WebClientConfiguration(
  @Value("\${api.base.url.hmpps-auth}") val hmppsAuthBaseUri: String,
  @Value("\${api.health-timeout:2s}") val healthTimeout: Duration,
  @Value("\${api.base.url.prisoner-search}") val prisonerSearchBaseUri: String,
  @Value("\${api.base.url.manage-users}") val manageUsersBaseUri: String,
  @Value("\${api.base.url.organisations}") val organisationsBaseUri: String,
  @Value("\${api.timeout:30s}") val timeout: Duration,
  private val builder: WebClient.Builder,
) {
  @Bean
  fun hmppsAuthHealthWebClient(): WebClient = builder.healthWebClient(hmppsAuthBaseUri, healthTimeout)

  @Bean
  fun prisonerSearchApiHealthWebClient() = builder.healthWebClient(prisonerSearchBaseUri, healthTimeout)

  @Bean
  fun prisonerSearchApiWebClient(authorizedClientManager: OAuth2AuthorizedClientManager) = builder.authorisedWebClient(authorizedClientManager, "prisoner-search-api", prisonerSearchBaseUri, timeout)

  @Bean
  fun manageUsersApiWebClient(authorizedClientManager: OAuth2AuthorizedClientManager) = builder.authorisedWebClient(authorizedClientManager, "manage-users-api", manageUsersBaseUri, timeout)

  @Bean
  fun organisationsApiWebClient(authorizedClientManager: OAuth2AuthorizedClientManager) = builder.authorisedWebClient(authorizedClientManager, "organisations-api", organisationsBaseUri, timeout)
}
