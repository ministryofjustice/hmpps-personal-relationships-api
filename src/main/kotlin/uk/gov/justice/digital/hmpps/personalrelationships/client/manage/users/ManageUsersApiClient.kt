package uk.gov.justice.digital.hmpps.personalrelationships.client.manage.users

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono

@Component
class ManageUsersApiClient(private val manageUsersApiWebClient: WebClient) {

  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getUserByUsername(username: String): UserDetails? = manageUsersApiWebClient
    .get()
    .uri("/users/{username}", username)
    .retrieve()
    .bodyToMono<UserDetails>()
    .onErrorResume({ exception ->
      exception is WebClientResponseException.ServiceUnavailable ||
        exception is WebClientResponseException.GatewayTimeout ||
        exception is WebClientResponseException.NotFound
    }) {
      log.debug("Couldn't find user with username: {}", username)
      Mono.empty()
    }
    .block()
}
