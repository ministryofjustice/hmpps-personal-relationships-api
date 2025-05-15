package uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.resource.sync

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppscontactsapi.client.manage.users.UserDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.PostgresIntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.CreateContactRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncCreateContactEmailRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncUpdateContactEmailRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync.SyncContactEmail
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.ContactEmailInfo
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.PersonReference
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.Source
import uk.gov.justice.digital.hmpps.hmppscontactsapi.util.StubUser
import java.time.LocalDateTime

class SyncContactEmailIntegrationTest : PostgresIntegrationTestBase() {

  private var savedContactId = 0L

  @BeforeEach
  fun initialiseData() {
    setCurrentUser(StubUser.READ_WRITE_USER)
    savedContactId = testAPIClient.createAContact(aMinimalCreateContactRequest()).id
    setCurrentUser(StubUser.SYNC_AND_MIGRATE_USER)
    stubGetUserByUsername(UserDetails("CREATE", "Create", "KMI"))
    stubGetUserByUsername(UserDetails("UPDATE", "Update", "BXI"))
  }

  @Test
  fun `Sync endpoints should return unauthorized if no token provided`() {
    webTestClient.get()
      .uri("/sync/contact-email/1")
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus()
      .isUnauthorized

    webTestClient.put()
      .uri("/sync/contact-email")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(createContactEmailRequest(savedContactId))
      .exchange()
      .expectStatus()
      .isUnauthorized

    webTestClient.post()
      .uri("/sync/contact-email/1")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(updateContactEmailRequest(savedContactId))
      .exchange()
      .expectStatus()
      .isUnauthorized

    webTestClient.delete()
      .uri("/sync/contact-email/1")
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @ParameterizedTest
  @ValueSource(strings = ["ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__R", "ROLE_CONTACTS__RW"])
  fun `Sync endpoints should return forbidden without an authorised role on the token`(role: String) {
    setCurrentUser(StubUser.SYNC_AND_MIGRATE_USER.copy(roles = listOf(role)))
    webTestClient.get()
      .uri("/sync/contact-email/1")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationUsingCurrentUser())
      .exchange()
      .expectStatus()
      .isForbidden

    webTestClient.post()
      .uri("/sync/contact-email")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(createContactEmailRequest(savedContactId))
      .headers(setAuthorisationUsingCurrentUser())
      .exchange()
      .expectStatus()
      .isForbidden

    webTestClient.put()
      .uri("/sync/contact-email/1")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(updateContactEmailRequest(savedContactId))
      .headers(setAuthorisationUsingCurrentUser())
      .exchange()
      .expectStatus()
      .isForbidden

    webTestClient.delete()
      .uri("/sync/contact-email/1")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationUsingCurrentUser())
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `should get an existing contact email`() {
    // From base data
    val contactEmailId = 2L
    val contactEmail = webTestClient.get()
      .uri("/sync/contact-email/{contactEmailId}", contactEmailId)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationUsingCurrentUser())
      .exchange()
      .expectStatus()
      .isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(SyncContactEmail::class.java)
      .returnResult().responseBody!!

    assertThat(contactEmail.emailAddress).isEqualTo("miss.last@example.com")
  }

  @Test
  fun `should create a new contact email`() {
    val contactEmail = webTestClient.post()
      .uri("/sync/contact-email")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationUsingCurrentUser())
      .bodyValue(createContactEmailRequest(savedContactId))
      .exchange()
      .expectStatus()
      .isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(SyncContactEmail::class.java)
      .returnResult().responseBody!!

    // The created email is returned
    with(contactEmail) {
      assertThat(contactEmailId).isGreaterThan(3)
      assertThat(contactId).isEqualTo(savedContactId)
      assertThat(emailAddress).isEqualTo("test@test.co.uk")
      assertThat(createdBy).isEqualTo("CREATE")
      assertThat(createdTime).isAfter(LocalDateTime.now().minusMinutes(5))
    }
    stubEvents.assertHasEvent(
      event = OutboundEvent.CONTACT_EMAIL_CREATED,
      additionalInfo = ContactEmailInfo(contactEmail.contactEmailId, Source.NOMIS, "CREATE", "KMI"),
      personReference = PersonReference(dpsContactId = contactEmail.contactId),
    )
  }

  @Test
  fun `should create and then update a contact email`() {
    val contactEmail = webTestClient.post()
      .uri("/sync/contact-email")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationUsingCurrentUser())
      .bodyValue(createContactEmailRequest(savedContactId))
      .exchange()
      .expectStatus()
      .isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(SyncContactEmail::class.java)
      .returnResult().responseBody!!

    with(contactEmail) {
      assertThat(emailAddress).isEqualTo("test@test.co.uk")
      assertThat(createdBy).isEqualTo("CREATE")
      assertThat(createdTime).isAfter(LocalDateTime.now().minusMinutes(5))
    }

    val updatedEmail = webTestClient.put()
      .uri("/sync/contact-email/{contactEmailId}", contactEmail.contactEmailId)
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationUsingCurrentUser())
      .bodyValue(updateContactEmailRequest(savedContactId))
      .exchange()
      .expectStatus()
      .isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(SyncContactEmail::class.java)
      .returnResult().responseBody!!

    // Check the updated copy
    with(updatedEmail) {
      assertThat(contactEmailId).isGreaterThan(4)
      assertThat(contactId).isEqualTo(savedContactId)
      assertThat(emailAddress).isEqualTo("test@test.co.uk")
      assertThat(updatedBy).isEqualTo("UPDATE")
      assertThat(updatedTime).isAfter(LocalDateTime.now().minusMinutes(5))
      assertThat(createdBy).isEqualTo("CREATE")
      assertThat(createdTime).isNotNull()
    }
    stubEvents.assertHasEvent(
      event = OutboundEvent.CONTACT_EMAIL_UPDATED,
      additionalInfo = ContactEmailInfo(contactEmail.contactEmailId, Source.NOMIS, "UPDATE", "BXI"),
      personReference = PersonReference(dpsContactId = contactEmail.contactId),
    )
  }

  @Test
  fun `should delete an existing contact email`() {
    val contactEmailId = 3L

    webTestClient.delete()
      .uri("/sync/contact-email/{contactEmailId}", contactEmailId)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationUsingCurrentUser())
      .exchange()
      .expectStatus()
      .isOk

    webTestClient.get()
      .uri("/sync/contact-email/{contactEmailId}", contactEmailId)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationUsingCurrentUser())
      .exchange()
      .expectStatus()
      .isNotFound
    stubEvents.assertHasEvent(
      event = OutboundEvent.CONTACT_EMAIL_DELETED,
      additionalInfo = ContactEmailInfo(contactEmailId, Source.NOMIS, "SYS", null),
      personReference = PersonReference(dpsContactId = 3),
    )
  }

  private fun updateContactEmailRequest(contactId: Long) = SyncUpdateContactEmailRequest(
    contactId = contactId,
    emailAddress = "test@test.co.uk",
    updatedBy = "UPDATE",
    updatedTime = LocalDateTime.now(),
  )

  private fun createContactEmailRequest(contactId: Long) = SyncCreateContactEmailRequest(
    contactId = contactId,
    emailAddress = "test@test.co.uk",
    createdBy = "CREATE",
  )

  private fun aMinimalCreateContactRequest() = CreateContactRequest(
    lastName = "last",
    firstName = "first",
  )
}
