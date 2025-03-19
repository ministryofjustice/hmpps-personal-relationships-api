package uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppscontactsapi.helpers.prisoner
import uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.SecureAPIIntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.CreateOrUpdatePrisonerNumberOfChildrenRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.PrisonerNumberOfChildrenResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.PersonReference
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.PrisonerNumberOfChildren
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.Source

class CreateOrUpdatePrisonerNumberOfChildrenIntegrationTest : SecureAPIIntegrationTestBase() {

  private val prisonerNumber = "A1234BC"
  private val prisoner1 = prisoner(
    prisonerNumber = "A1234BC",
    prisonId = "MDI",
    firstName = "Joe",
    middleNames = "Middle",
    lastName = "Bloggs",
  )
  override val allowedRoles: Set<String> = setOf("ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__RW")

  override fun baseRequestBuilder(): WebTestClient.RequestHeadersSpec<*> = webTestClient.put()
    .uri("/prisoner/$prisonerNumber/number-of-children")
    .accept(MediaType.APPLICATION_JSON)
    .contentType(MediaType.APPLICATION_JSON)
    .bodyValue(createRequest())

  @ParameterizedTest
  @ValueSource(strings = ["ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__RW"])
  fun `should create new number of children`(role: String) {
    stubPrisonerSearch(prisoner1)
    val request = createRequest()

    val response = webTestClient.put()
      .uri("/prisoner/$prisonerNumber/number-of-children")
      .headers(setAuthorisation(roles = listOf(role)))
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(request)
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(PrisonerNumberOfChildrenResponse::class.java)
      .returnResult().responseBody

    assertThat(response).isNotNull
    assertThat(response).usingRecursiveComparison()
      .ignoringFields("id", "createdTime")
      .isEqualTo(
        PrisonerNumberOfChildrenResponse(
          id = 2L,
          numberOfChildren = "1",
          active = true,
          createdBy = "test-user",
        ),
      )
    stubEvents.assertHasEvent(
      event = OutboundEvent.PRISONER_NUMBER_OF_CHILDREN_CREATED,
      additionalInfo = PrisonerNumberOfChildren(response!!.id, Source.DPS),
      personReference = PersonReference(nomsNumber = prisonerNumber),
    )
  }

  @Test
  fun `should allow null value for number of children`() {
    stubPrisonerSearch(prisoner1)
    val request = CreateOrUpdatePrisonerNumberOfChildrenRequest(
      numberOfChildren = null,
      requestedBy = "test-user",
    )

    val response = webTestClient.put()
      .uri("/prisoner/$prisonerNumber/number-of-children")
      .headers(setAuthorisation(roles = listOf("ROLE_CONTACTS_ADMIN")))
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(request)
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(PrisonerNumberOfChildrenResponse::class.java)
      .returnResult().responseBody

    assertThat(response).isNotNull
    assertThat(response).usingRecursiveComparison()
      .ignoringFields("id", "createdTime")
      .isEqualTo(
        PrisonerNumberOfChildrenResponse(
          id = 2L,
          numberOfChildren = null,
          active = true,
          createdBy = "test-user",
        ),
      )
    stubEvents.assertHasEvent(
      event = OutboundEvent.PRISONER_NUMBER_OF_CHILDREN_CREATED,
      additionalInfo = PrisonerNumberOfChildren(response!!.id, Source.DPS),
      personReference = PersonReference(nomsNumber = prisonerNumber),
    )
  }

  @Test
  fun `should update existing number of children`() {
    stubPrisonerSearch(prisoner1)
    val request = createRequest()
    val response = webTestClient.put()
      .uri("/prisoner/$prisonerNumber/number-of-children")
      .headers(setAuthorisation(roles = listOf("ROLE_CONTACTS_ADMIN")))
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(request)
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(PrisonerNumberOfChildrenResponse::class.java)
      .returnResult().responseBody

    assertThat(response).isNotNull

    val updateResponse = webTestClient.put()
      .uri("/prisoner/$prisonerNumber/number-of-children")
      .headers(setAuthorisation(roles = listOf("ROLE_CONTACTS__RW")))
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(request)
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(PrisonerNumberOfChildrenResponse::class.java)
      .returnResult().responseBody

    assertThat(updateResponse).isNotNull
    assertThat(updateResponse!!.id).isGreaterThan(response!!.id)
    assertThat(updateResponse).usingRecursiveComparison()
      .ignoringFields("id", "createdTime")
      .isEqualTo(
        PrisonerNumberOfChildrenResponse(
          id = 1L,
          numberOfChildren = "1",
          active = true,
          createdBy = "test-user",
        ),
      )
    stubEvents.assertHasEvent(
      event = OutboundEvent.PRISONER_NUMBER_OF_CHILDREN_CREATED,
      additionalInfo = PrisonerNumberOfChildren(response.id, Source.DPS),
      personReference = PersonReference(nomsNumber = prisonerNumber),
    )
  }

  @Test
  fun `should return 400 when number of children is more than 99`() {
    webTestClient.put()
      .uri("/prisoner/$prisonerNumber/number-of-children")
      .headers(setAuthorisation(roles = listOf("ROLE_CONTACTS__RW")))
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(
        CreateOrUpdatePrisonerNumberOfChildrenRequest(
          numberOfChildren = 100,
          requestedBy = "test-user",
        ),
      )
      .exchange()
      .expectStatus().isBadRequest
      .expectBody()
      .jsonPath("$.userMessage")
      .isEqualTo("Validation failure(s): numberOfChildren must be less than or equal to 99")
    stubEvents.assertHasNoEvents(
      event = OutboundEvent.PRISONER_NUMBER_OF_CHILDREN_CREATED,
    )
  }

  @Test
  fun `should return 400 when number of children is invalid`() {
    stubPrisonerSearch(prisoner1)
    webTestClient.put()
      .uri("/prisoner/$prisonerNumber/number-of-children")
      .headers(setAuthorisation(roles = listOf("ROLE_CONTACTS__RW")))
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(
        CreateOrUpdatePrisonerNumberOfChildrenRequest(
          numberOfChildren = -1,
          requestedBy = "test-user",
        ),
      )
      .exchange()
      .expectStatus().isBadRequest
      .expectBody()
      .jsonPath("$.userMessage")
      .isEqualTo("Validation failure(s): numberOfChildren must be greater than or equal to 0")
    stubEvents.assertHasNoEvents(
      event = OutboundEvent.PRISONER_NUMBER_OF_CHILDREN_CREATED,
    )
  }

  private fun createRequest() = CreateOrUpdatePrisonerNumberOfChildrenRequest(
    numberOfChildren = 1,
    requestedBy = "test-user",
  )
}
