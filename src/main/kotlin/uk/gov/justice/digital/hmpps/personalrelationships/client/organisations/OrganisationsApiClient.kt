package uk.gov.justice.digital.hmpps.personalrelationships.client.organisations

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.personalrelationships.client.organisationsapi.model.OrganisationSummary

@Component
class OrganisationsApiClient(private val organisationsApiWebClient: WebClient) {

  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getOrganisationSummary(id: Long): OrganisationSummary? = organisationsApiWebClient
    .get()
    .uri("/organisation/{id}/summary", id)
    .retrieve()
    .bodyToMono(OrganisationSummary::class.java)
    .onErrorResume(WebClientResponseException.NotFound::class.java) {
      log.debug("Couldn't find organisation with id: {}", id)
      Mono.empty()
    }
    .block()
}
