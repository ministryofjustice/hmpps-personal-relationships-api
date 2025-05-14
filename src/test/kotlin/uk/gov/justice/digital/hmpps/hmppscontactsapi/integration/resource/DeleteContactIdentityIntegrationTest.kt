package uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.SecureAPIIntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.CreateContactRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.identity.CreateIdentityRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.ContactIdentityInfo
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.PersonReference
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.Source
import uk.gov.justice.digital.hmpps.hmppscontactsapi.util.StubUser
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

class DeleteContactIdentityIntegrationTest : SecureAPIIntegrationTestBase() {
  private var savedContactId = 0L
  private var savedContactIdentityId = 0L

  override val allowedRoles: Set<String> = setOf("ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__RW")

  @BeforeEach
  fun initialiseData() {
    setCurrentUser(StubUser.CREATING_USER)
    savedContactId = testAPIClient.createAContact(
      CreateContactRequest(
        lastName = "identity",
        firstName = "has",
      ),

    ).id
    savedContactIdentityId = testAPIClient.createAContactIdentity(
      savedContactId,
      CreateIdentityRequest(
        identityType = "DL",
        identityValue = "DL123456789",
      ),

    ).contactIdentityId
    setCurrentUser(StubUser.DELETING_USER)
  }

  override fun baseRequestBuilder(): WebTestClient.RequestHeadersSpec<*> = webTestClient.delete()
    .uri("/contact/$savedContactId/identity/$savedContactIdentityId")
    .accept(MediaType.APPLICATION_JSON)

  @Test
  fun `should not delete the identity if the contact is not found`() {
    val errors = webTestClient.delete()
      .uri("/contact/-321/identity/$savedContactIdentityId")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationUsingCurrentUser())
      .exchange()
      .expectStatus()
      .isNotFound
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody!!

    assertThat(errors.userMessage).isEqualTo("Entity not found : Contact (-321) not found")

    stubEvents.assertHasNoEvents(
      event = OutboundEvent.CONTACT_IDENTITY_DELETED,
      additionalInfo = ContactIdentityInfo(savedContactIdentityId, Source.DPS, "deleted", "BXI"),
    )
  }

  @Test
  fun `should not update the identity if the identity is not found`() {
    val errors = webTestClient.delete()
      .uri("/contact/$savedContactId/identity/-99")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationUsingCurrentUser())
      .exchange()
      .expectStatus()
      .isNotFound
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody!!

    assertThat(errors.userMessage).isEqualTo("Entity not found : Contact identity (-99) not found")

    stubEvents.assertHasNoEvents(
      event = OutboundEvent.CONTACT_IDENTITY_DELETED,
      additionalInfo = ContactIdentityInfo(-99, Source.DPS, "deleted", "BXI"),
    )
  }

  @Test
  fun `should delete the contacts identity number`() {
    webTestClient.delete()
      .uri("/contact/$savedContactId/identity/$savedContactIdentityId")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationUsingCurrentUser())
      .exchange()
      .expectStatus()
      .isNoContent

    webTestClient.get()
      .uri("/contact/$savedContactId/identity/$savedContactIdentityId")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationUsingCurrentUser())
      .exchange()
      .expectStatus()
      .isNotFound

    stubEvents.assertHasEvent(
      event = OutboundEvent.CONTACT_IDENTITY_DELETED,
      additionalInfo = ContactIdentityInfo(savedContactIdentityId, Source.DPS, "deleted", "BXI"),
      personReference = PersonReference(dpsContactId = savedContactId),
    )
  }
}
