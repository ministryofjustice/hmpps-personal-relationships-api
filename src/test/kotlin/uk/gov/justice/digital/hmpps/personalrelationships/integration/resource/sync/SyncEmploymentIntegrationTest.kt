package uk.gov.justice.digital.hmpps.personalrelationships.integration.resource.sync

import org.apache.commons.lang3.RandomUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.personalrelationships.client.manage.users.UserDetails
import uk.gov.justice.digital.hmpps.personalrelationships.config.User
import uk.gov.justice.digital.hmpps.personalrelationships.integration.PostgresIntegrationTestBase
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.CreateContactRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.sync.SyncCreateEmploymentRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.sync.SyncUpdateEmploymentRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.sync.SyncEmployment
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.EmploymentInfo
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.PersonReference
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.Source
import uk.gov.justice.digital.hmpps.personalrelationships.util.StubUser
import java.time.LocalDate
import java.time.LocalDateTime

class SyncEmploymentIntegrationTest : PostgresIntegrationTestBase() {

  @BeforeEach
  fun resetEvents() {
    stubEvents.reset()
    setCurrentUser(StubUser.SYNC_AND_MIGRATE_USER)
    stubGetUserByUsername(UserDetails("CREATOR", "Create", "KMI"))
    stubGetUserByUsername(UserDetails("UPDATER", "Update", "BXI"))
  }

  @Test
  fun `Sync endpoints should return unauthorized if no token provided`() {
    webTestClient.get()
      .uri("/sync/employment/1")
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus()
      .isUnauthorized

    webTestClient.post()
      .uri("/sync/employment")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(createSyncEmploymentRequest())
      .exchange()
      .expectStatus()
      .isUnauthorized

    webTestClient.put()
      .uri("/sync/employment/1")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(updateEmploymentRequest(employmentSyncResponse()))
      .exchange()
      .expectStatus()
      .isUnauthorized

    webTestClient.delete()
      .uri("/sync/employment/1")
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Sync endpoints should return forbidden without an authorised role on the token`() {
    setCurrentUser(StubUser.SYNC_AND_MIGRATE_USER.copy(roles = listOf("ROLE_WRONG")))
    webTestClient.get()
      .uri("/sync/employment/1")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationUsingCurrentUser())
      .exchange()
      .expectStatus()
      .isForbidden

    webTestClient.post()
      .uri("/sync/employment")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(createSyncEmploymentRequest())
      .headers(setAuthorisationUsingCurrentUser())
      .exchange()
      .expectStatus()
      .isForbidden

    webTestClient.put()
      .uri("/sync/employment/1")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(updateEmploymentRequest(employmentSyncResponse()))
      .headers(setAuthorisationUsingCurrentUser())
      .exchange()
      .expectStatus()
      .isForbidden

    webTestClient.delete()
      .uri("/sync/employment/1")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationUsingCurrentUser())
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `should get an existing employment`() {
    val savedEmployment = createEmployment()
    val employment = webTestClient.get()
      .uri("/sync/employment/{employmentId}", savedEmployment.employmentId)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationUsingCurrentUser())
      .exchange()
      .expectStatus()
      .isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(SyncEmployment::class.java)
      .returnResult().responseBody!!

    with(employment) {
      assertThat(employmentId).isEqualTo(savedEmployment.employmentId)
      assertThat(organisationId).isEqualTo(savedEmployment.organisationId)
      assertThat(contactId).isEqualTo(savedEmployment.contactId)
      assertThat(active).isTrue()
      assertThat(createdBy).isEqualTo(savedEmployment.createdBy)
      assertThat(createdTime).isAfter(LocalDateTime.now().minusMinutes(5))
    }
  }

  @Test
  fun `should create a new employment`() {
    val organisationId = createOrganisation()
    val contactId = createContact()

    val employment = webTestClient.post()
      .uri("/sync/employment")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationUsingCurrentUser())
      .bodyValue(createSyncEmploymentRequest(organisationId = organisationId, contactId = contactId))
      .exchange()
      .expectStatus()
      .isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(SyncEmployment::class.java)
      .returnResult().responseBody!!

    // The created is returned
    with(employment) {
      assertThat(employmentId).isNotNull()
      assertThat(employment.organisationId).isEqualTo(organisationId)
      assertThat(employment.contactId).isEqualTo(contactId)
      assertThat(active).isTrue()
      assertThat(createdBy).isEqualTo("CREATOR")
      assertThat(updatedBy).isEqualTo(null)
      assertThat(createdTime).isAfter(LocalDateTime.now().minusMinutes(5))
    }

    stubEvents.assertHasEvent(
      event = OutboundEvent.EMPLOYMENT_CREATED,
      additionalInfo = EmploymentInfo(employment.employmentId, Source.NOMIS, "CREATOR", "KMI"),
      personReference = PersonReference(dpsContactId = employment.contactId),
    )

    assertCustomCreatedEvent(employment, Source.NOMIS, User("CREATOR", "KMI"))
  }

  @Test
  fun `should create and then update a employment`() {
    val employment = createEmployment()

    val updateEmploymentRequest = updateEmploymentRequest(employment)
    val updatedEmployment = webTestClient.put()
      .uri("/sync/employment/{employmentId}", employment.employmentId)
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationUsingCurrentUser())
      .bodyValue(updateEmploymentRequest)
      .exchange()
      .expectStatus()
      .isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(SyncEmployment::class.java)
      .returnResult().responseBody!!

    // Check the updated copy
    with(updatedEmployment) {
      assertThat(employmentId).isEqualTo(employment.employmentId)
      assertThat(organisationId).isEqualTo(updateEmploymentRequest.organisationId)
      assertThat(contactId).isEqualTo(updateEmploymentRequest.contactId)
      assertThat(active).isFalse()
      assertThat(updatedBy).isEqualTo(updateEmploymentRequest.updatedBy)
      assertThat(updatedTime).isAfter(LocalDateTime.now().minusMinutes(5))
    }

    stubEvents.assertHasEvent(
      event = OutboundEvent.EMPLOYMENT_UPDATED,
      additionalInfo = EmploymentInfo(employment.employmentId, Source.NOMIS, updateEmploymentRequest.updatedBy, "BXI"),
      personReference = PersonReference(dpsContactId = updatedEmployment.contactId),
    )

    assertCustomUpdatedEvent(updatedEmployment, Source.NOMIS, User(updateEmploymentRequest.updatedBy, "BXI"))
  }

  @Test
  fun `should delete an existing employment`() {
    val employment = createEmployment()

    webTestClient.delete()
      .uri("/sync/employment/{employmentId}", employment.employmentId)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationUsingCurrentUser())
      .exchange()
      .expectStatus()
      .isOk

    webTestClient.get()
      .uri("/sync/employment/{employmentId}", employment.employmentId)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationUsingCurrentUser())
      .exchange()
      .expectStatus()
      .isNotFound

    stubEvents.assertHasEvent(
      event = OutboundEvent.EMPLOYMENT_DELETED,
      additionalInfo = EmploymentInfo(employment.employmentId, Source.NOMIS, "SYS", null),
      personReference = PersonReference(dpsContactId = employment.contactId),
    )

    assertCustomDeletedEvent(employment, Source.NOMIS, User("SYS"))
  }

  private fun updateEmploymentRequest(employment: SyncEmployment) = SyncUpdateEmploymentRequest(
    organisationId = employment.organisationId,
    contactId = employment.contactId,
    active = false,
    updatedBy = "UPDATER",
    updatedTime = LocalDateTime.now(),
  )

  private fun createSyncEmploymentRequest(organisationId: Long = 2L, contactId: Long = 3L) = SyncCreateEmploymentRequest(
    organisationId = organisationId,
    contactId = contactId,
    active = true,
    createdBy = "CREATOR",
    createdTime = LocalDateTime.now(),
  )

  private fun createEmployment(): SyncEmployment {
    val organisationId = createOrganisation()
    val contactId = createContact()

    val employment = webTestClient.post()
      .uri("/sync/employment")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationUsingCurrentUser())
      .bodyValue(createSyncEmploymentRequest(organisationId = organisationId, contactId = contactId))
      .exchange()
      .expectStatus()
      .isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(SyncEmployment::class.java)
      .returnResult().responseBody!!

    // The created is returned
    with(employment) {
      assertThat(employmentId).isNotNull()
    }
    return employment
  }

  private fun assertCustomCreatedEvent(syncEmployment: SyncEmployment, source: Source, user: User) {
    verify(telemetryContactCustomEventService, times(1)).trackCreateEmploymentEvent(syncEmployment, source, user)

    verify(telemetryClient, times(1)).trackEvent(
      "contact-employment-created",
      mapOf(
        "description" to "A contact employment has been created",
        "source" to source.name,
        "username" to user.username,
        "contactId" to syncEmployment.contactId.toString(),
        "active_caseload_id" to user.activeCaseLoadId,
        "contact_employment_id" to syncEmployment.employmentId.toString(),
      ),
      null,
    )
  }

  private fun assertCustomUpdatedEvent(syncEmployment: SyncEmployment, source: Source, user: User) {
    verify(telemetryContactCustomEventService, times(1)).trackUpdateEmploymentEvent(syncEmployment, source, user)

    verify(telemetryClient, times(1)).trackEvent(
      "contact-employment-updated",
      mapOf(
        "description" to "A contact employment has been updated",
        "source" to source.name,
        "username" to user.username,
        "contactId" to syncEmployment.contactId.toString(),
        "active_caseload_id" to user.activeCaseLoadId,
        "contact_employment_id" to syncEmployment.employmentId.toString(),
      ),
      null,
    )
  }

  private fun assertCustomDeletedEvent(syncEmployment: SyncEmployment, source: Source, user: User) {
    verify(telemetryContactCustomEventService, times(1)).trackDeleteEmploymentEvent(any<SyncEmployment>(), any<Source>(), any<User>())

    verify(telemetryClient, times(1)).trackEvent(
      "contact-employment-deleted",
      mapOf(
        "description" to "A contact employment has been deleted",
        "source" to source.name,
        "username" to user.username,
        "contactId" to syncEmployment.contactId.toString(),
        "contact_employment_id" to syncEmployment.employmentId.toString(),
      ),
      null,
    )
  }

  private fun createOrganisation(): Long = stubOrganisationSummary(RandomUtils.secure().randomLong(10000, 99999)).organisationId

  private fun createContact(): Long {
    val request = CreateContactRequest(
      titleCode = "MR",
      lastName = "last",
      firstName = "first",
      middleNames = "middle",
      dateOfBirth = LocalDate.of(1982, 6, 15),
    )

    val response = doWithTemporaryWritePermission { testAPIClient.createAContact(request) }

    assertThat(response).isNotNull
    with(response) { assertThat(id).isNotNull() }
    return response.id
  }

  private fun employmentSyncResponse() = SyncEmployment(
    employmentId = 2L,
    organisationId = 2L,
    contactId = 2L,
    active = true,
    createdBy = "CREATOR",
    createdTime = LocalDateTime.now(),
    updatedBy = null,
    updatedTime = null,
  )
}
