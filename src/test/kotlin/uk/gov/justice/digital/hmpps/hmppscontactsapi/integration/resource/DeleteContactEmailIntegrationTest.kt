package uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.SecureAPIIntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.CreateContactRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.email.CreateEmailRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.ContactEmailInfo
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.PersonReference
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.Source
import uk.gov.justice.digital.hmpps.hmppscontactsapi.util.StubUser
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

class DeleteContactEmailIntegrationTest : SecureAPIIntegrationTestBase() {
  private var savedContactId = 0L
  private var savedContactEmailId = 0L

  override val allowedRoles: Set<String> = setOf("ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__RW")

  @BeforeEach
  fun initialiseData() {
    setCurrentUser(StubUser.READ_WRITE_USER)
    savedContactId = testAPIClient.createAContact(
      CreateContactRequest(
        lastName = "email",
        firstName = "has",
      ),
    ).id
    savedContactEmailId = testAPIClient.createAContactEmail(
      savedContactId,
      CreateEmailRequest(
        emailAddress = "test@example.com",
      ),
    ).contactEmailId
    setCurrentUser(StubUser.DELETING_USER)
  }

  override fun baseRequestBuilder(): WebTestClient.RequestHeadersSpec<*> = webTestClient.delete()
    .uri("/contact/$savedContactId/email/$savedContactEmailId")
    .accept(MediaType.APPLICATION_JSON)

  @Test
  fun `should not delete the email if the contact is not found`() {
    val errors = webTestClient.delete()
      .uri("/contact/-321/email/$savedContactEmailId")
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
      event = OutboundEvent.CONTACT_EMAIL_DELETED,
      additionalInfo = ContactEmailInfo(savedContactEmailId, Source.DPS, "deleted"),
    )
  }

  @Test
  fun `should not update the email if the email is not found`() {
    val errors = webTestClient.delete()
      .uri("/contact/$savedContactId/email/-99")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationUsingCurrentUser())
      .exchange()
      .expectStatus()
      .isNotFound
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody!!

    assertThat(errors.userMessage).isEqualTo("Entity not found : Contact email (-99) not found")

    stubEvents.assertHasNoEvents(
      event = OutboundEvent.CONTACT_EMAIL_DELETED,
      additionalInfo = ContactEmailInfo(-99, Source.DPS, "deleted"),
    )
  }

  @ParameterizedTest
  @ValueSource(strings = ["ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__RW"])
  fun `should delete the contacts email`(role: String) {
    setCurrentUser(StubUser.DELETING_USER.copy(roles = listOf(role)))
    webTestClient.delete()
      .uri("/contact/$savedContactId/email/$savedContactEmailId")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationUsingCurrentUser())
      .exchange()
      .expectStatus()
      .isNoContent

    webTestClient.get()
      .uri("/contact/$savedContactId/email/$savedContactEmailId")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationUsingCurrentUser())
      .exchange()
      .expectStatus()
      .isNotFound

    stubEvents.assertHasEvent(
      event = OutboundEvent.CONTACT_EMAIL_DELETED,
      additionalInfo = ContactEmailInfo(savedContactEmailId, Source.DPS, "deleted"),
      personReference = PersonReference(dpsContactId = savedContactId),
    )
  }
}
