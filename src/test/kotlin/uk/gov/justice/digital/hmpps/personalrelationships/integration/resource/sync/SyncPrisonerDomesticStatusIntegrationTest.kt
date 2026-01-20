package uk.gov.justice.digital.hmpps.personalrelationships.integration.resource.sync

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.personalrelationships.integration.PostgresIntegrationTestBase
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.sync.SyncUpdatePrisonerDomesticStatusRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.sync.SyncPrisonerDomesticStatusResponse
import uk.gov.justice.digital.hmpps.personalrelationships.repository.PrisonerDomesticStatusRepository
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.PersonReference
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.PrisonerDomesticStatus
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.Source
import uk.gov.justice.digital.hmpps.personalrelationships.util.StubUser
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.time.LocalDateTime

class SyncPrisonerDomesticStatusIntegrationTest : PostgresIntegrationTestBase() {

  private val prisonerNumber = "A1234BC"

  @Autowired
  private lateinit var domesticStatusRepository: PrisonerDomesticStatusRepository

  @BeforeEach
  fun setUp() {
    domesticStatusRepository.deleteAll()
    setCurrentUser(StubUser.SYNC_AND_MIGRATE_USER)
  }

  @Test
  fun `Sync endpoints should return unauthorized if no token provided`() {
    webTestClient.get()
      .uri("/sync/$prisonerNumber/domestic-status")
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus()
      .isUnauthorized

    webTestClient.put()
      .uri("/sync/$prisonerNumber/domestic-status")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(aMinimalRequest())
      .exchange()
      .expectStatus()
      .isUnauthorized

    webTestClient.post()
      .uri("/sync/$prisonerNumber/domestic-status")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(aMinimalRequest())
      .exchange()
      .expectStatus()
      .isUnauthorized

    webTestClient.delete()
      .uri("/sync/$prisonerNumber/domestic-status")
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
      .uri("/sync/$prisonerNumber/domestic-status")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationUsingCurrentUser())
      .exchange()
      .expectStatus()
      .isForbidden

    webTestClient.put()
      .uri("/sync/$prisonerNumber/domestic-status")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(aMinimalRequest())
      .headers(setAuthorisationUsingCurrentUser())
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `should get an existing prisoner domestic status`() {
    val domesticStatusToSync = SyncUpdatePrisonerDomesticStatusRequest(
      domesticStatusCode = "D",
      createdBy = "user",
      createdTime = LocalDateTime.now(),
    )

    // When
    webTestClient.put()
      .uri("/sync/$prisonerNumber/domestic-status")
      .headers(setAuthorisationUsingCurrentUser())
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(domesticStatusToSync)
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(SyncPrisonerDomesticStatusResponse::class.java)
      .returnResult().responseBody

    val domesticStatus = webTestClient.get()
      .uri("/sync/$prisonerNumber/domestic-status")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationUsingCurrentUser())
      .exchange()
      .expectStatus()
      .isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(SyncPrisonerDomesticStatusResponse::class.java)
      .returnResult().responseBody!!

    assertThat(domesticStatus.id).isGreaterThan(0)
    assertThat(domesticStatus.domesticStatusCode).isEqualTo("D")
    assertThat(domesticStatus.createdBy).isEqualTo("user")
    assertThat(domesticStatus.createdTime).isNotNull
    assertThat(domesticStatus.active).isTrue
  }

  @ParameterizedTest
  @ValueSource(strings = ["D"])
  @NullSource
  fun `should create a new domestic status record`(domesticStatusCode: String?) {
    // Given
    val domesticStatusToSync = SyncUpdatePrisonerDomesticStatusRequest(
      domesticStatusCode = domesticStatusCode,
      createdBy = "user",
      createdTime = LocalDateTime.now(),
    )

    // When
    val response = webTestClient.put()
      .uri("/sync/$prisonerNumber/domestic-status")
      .headers(setAuthorisationUsingCurrentUser())
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(domesticStatusToSync)
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(SyncPrisonerDomesticStatusResponse::class.java)
      .returnResult().responseBody

    assertThat(response).isNotNull
    assertThat(response).usingRecursiveComparison()
      .ignoringFields("id", "createdTime")
      .isEqualTo(
        SyncPrisonerDomesticStatusResponse(
          id = 1L,
          domesticStatusCode = domesticStatusCode,
          createdBy = "user",
          createdTime = LocalDateTime.now(),
          active = true,
        ),
      )

    // Verify database state
    val savedDomesticStatus = webTestClient.get()
      .uri("/sync/$prisonerNumber/domestic-status")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationUsingCurrentUser())
      .exchange()
      .expectStatus()
      .isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(SyncPrisonerDomesticStatusResponse::class.java)
      .returnResult().responseBody!!

    assertThat(savedDomesticStatus.id).isGreaterThan(0)
    assertThat(savedDomesticStatus.domesticStatusCode).isEqualTo(domesticStatusCode)
    assertThat(savedDomesticStatus.createdBy).isEqualTo("user")
    assertThat(savedDomesticStatus.createdTime).isNotNull
    assertThat(savedDomesticStatus.active).isTrue

    stubEvents.assertHasEvent(
      event = OutboundEvent.PRISONER_DOMESTIC_STATUS_CREATED,
      additionalInfo = PrisonerDomesticStatus(savedDomesticStatus.id, Source.NOMIS, "SYS", null),
      personReference = PersonReference(nomsNumber = prisonerNumber),
    )
  }

  @Test
  fun `should updates existing record as inactive and create new record when existing status is different`() {
    // Given
    val existingDomesticStatus = SyncUpdatePrisonerDomesticStatusRequest(
      domesticStatusCode = "D",
      createdBy = "user",
      createdTime = LocalDateTime.now(),
    )
    val existingResponse = webTestClient.put()
      .uri("/sync/$prisonerNumber/domestic-status")
      .headers(setAuthorisationUsingCurrentUser())
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(existingDomesticStatus)
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(SyncPrisonerDomesticStatusResponse::class.java)
      .returnResult().responseBody

    assertThat(existingResponse).isNotNull

    val updatedDomesticStatus = SyncUpdatePrisonerDomesticStatusRequest(
      domesticStatusCode = "M",
      createdBy = "user",
      createdTime = LocalDateTime.now(),
    )

    // When
    val response = webTestClient.put()
      .uri("/sync/$prisonerNumber/domestic-status")
      .headers(setAuthorisationUsingCurrentUser())
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(updatedDomesticStatus)
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(SyncPrisonerDomesticStatusResponse::class.java)
      .returnResult().responseBody

    assertThat(response).isNotNull
    assertThat(response).usingRecursiveComparison()
      .ignoringFields("id", "createdBy", "createdTime")
      .isEqualTo(
        SyncPrisonerDomesticStatusResponse(
          id = 0,
          domesticStatusCode = "M",
          createdBy = "User",
          createdTime = LocalDateTime.now(),
          active = true,
        ),
      )

    val savedDomesticStatus = webTestClient.get()
      .uri("/sync/$prisonerNumber/domestic-status")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationUsingCurrentUser())
      .exchange()
      .expectStatus()
      .isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(SyncPrisonerDomesticStatusResponse::class.java)
      .returnResult().responseBody!!

    assertThat(savedDomesticStatus.id).isGreaterThan(0)
    assertThat(savedDomesticStatus.domesticStatusCode).isEqualTo("M")
    assertThat(savedDomesticStatus.createdBy).isEqualTo("user")
    assertThat(savedDomesticStatus.createdTime).isNotNull
    assertThat(savedDomesticStatus.active).isTrue

    val historicalRecord = domesticStatusRepository.findByPrisonerNumberAndActiveFalse(prisonerNumber)
    assertThat(historicalRecord[0].domesticStatusCode).isEqualTo("D")
    assertThat(historicalRecord[0].createdBy).isEqualTo("user")
    stubEvents.assertHasEvent(
      event = OutboundEvent.PRISONER_DOMESTIC_STATUS_UPDATED,
      additionalInfo = PrisonerDomesticStatus(historicalRecord[0].prisonerDomesticStatusId, Source.NOMIS, "SYS", null),
      personReference = PersonReference(nomsNumber = prisonerNumber),
    )

    stubEvents.assertHasEvent(
      event = OutboundEvent.PRISONER_DOMESTIC_STATUS_CREATED,
      additionalInfo = PrisonerDomesticStatus(savedDomesticStatus.id, Source.NOMIS, "SYS", null),
      personReference = PersonReference(nomsNumber = prisonerNumber),
    )
  }

  @Test
  fun `should ignore request when existing status value is same`() {
    // Given
    val existingDomesticStatus = SyncUpdatePrisonerDomesticStatusRequest(
      domesticStatusCode = "D",
      createdBy = "user",
      createdTime = LocalDateTime.now(),
    )
    val existingResponse = webTestClient.put()
      .uri("/sync/$prisonerNumber/domestic-status")
      .headers(setAuthorisationUsingCurrentUser())
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(existingDomesticStatus)
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(SyncPrisonerDomesticStatusResponse::class.java)
      .returnResult().responseBody

    assertThat(existingResponse).isNotNull

    val updatedDomesticStatus = SyncUpdatePrisonerDomesticStatusRequest(
      domesticStatusCode = "D",
      createdBy = "user",
      createdTime = LocalDateTime.now(),
    )
    stubEvents.reset()

    // When
    val response = webTestClient.put()
      .uri("/sync/$prisonerNumber/domestic-status")
      .headers(setAuthorisationUsingCurrentUser())
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(updatedDomesticStatus)
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(SyncPrisonerDomesticStatusResponse::class.java)
      .returnResult().responseBody

    assertThat(response).isNotNull
    assertThat(response).usingRecursiveComparison()
      .ignoringFields("id", "createdBy", "createdTime")
      .isEqualTo(
        SyncPrisonerDomesticStatusResponse(
          id = 0,
          domesticStatusCode = "D",
          createdBy = "User",
          createdTime = LocalDateTime.now(),
          active = true,
        ),
      )

    val savedDomesticStatus = webTestClient.get()
      .uri("/sync/$prisonerNumber/domestic-status")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationUsingCurrentUser())
      .exchange()
      .expectStatus()
      .isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(SyncPrisonerDomesticStatusResponse::class.java)
      .returnResult().responseBody!!

    assertThat(savedDomesticStatus.id).isGreaterThan(0)
    assertThat(savedDomesticStatus.domesticStatusCode).isEqualTo("D")
    assertThat(savedDomesticStatus.createdBy).isEqualTo("user")
    assertThat(savedDomesticStatus.createdTime).isNotNull
    assertThat(savedDomesticStatus.active).isTrue

    val historicalRecord = domesticStatusRepository.findByPrisonerNumberAndActiveFalse(prisonerNumber)
    assertThat(historicalRecord).isEmpty()

    stubEvents.assertHasNoEvents(
      event = OutboundEvent.PRISONER_DOMESTIC_STATUS_UPDATED,
    )

    stubEvents.assertHasNoEvents(
      event = OutboundEvent.PRISONER_DOMESTIC_STATUS_CREATED,
    )
  }

  @Test
  fun `sync domestic status - bad request when invalid data`() {
    // Given
    val invalidDomesticStatus = SyncUpdatePrisonerDomesticStatusRequest(
      domesticStatusCode = "LONGER THAN 12 CHARACTERS",
      createdBy = "user",
      createdTime = LocalDateTime.now(),
    )

    // When
    val response = webTestClient.put()
      .uri("/sync/$prisonerNumber/domestic-status")
      .headers(setAuthorisationUsingCurrentUser())
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(invalidDomesticStatus)
      .exchange()
      .expectStatus().isBadRequest
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody!!

    assertThat(response.developerMessage).contains("domesticStatusCode must be less than or equal to 12 characters")
  }

  @Test
  fun `sync domestic statuses - success`() {
    // Given
    val domesticStatusesToSync = SyncUpdatePrisonerDomesticStatusRequest(
      domesticStatusCode = "D",
      createdBy = "user",
      createdTime = LocalDateTime.now(),
    )

    // When
    val response = webTestClient.put()
      .uri("/sync/$prisonerNumber/domestic-status")
      .headers(setAuthorisationUsingCurrentUser())
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(domesticStatusesToSync)
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(SyncPrisonerDomesticStatusResponse::class.java)
      .returnResult().responseBody

    assertThat(response).isNotNull
    assertThat(response).usingRecursiveComparison()
      .ignoringFields("id", "createdBy", "createdTime")
      .isEqualTo(
        SyncPrisonerDomesticStatusResponse(
          id = 0,
          domesticStatusCode = "D",
          createdBy = "User",
          createdTime = LocalDateTime.now(),
          active = true,
        ),
      )

    // Verify database state
    val savedDomesticStatus = webTestClient.get()
      .uri("/sync/$prisonerNumber/domestic-status")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationUsingCurrentUser())
      .exchange()
      .expectStatus()
      .isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(SyncPrisonerDomesticStatusResponse::class.java)
      .returnResult().responseBody!!

    assertThat(savedDomesticStatus.id).isGreaterThan(0)
    assertThat(savedDomesticStatus.domesticStatusCode).isEqualTo("D")
    assertThat(savedDomesticStatus.createdBy).isEqualTo("user")
    assertThat(savedDomesticStatus.createdTime).isNotNull
    assertThat(savedDomesticStatus.active).isTrue
  }

  private fun aMinimalRequest() = SyncUpdatePrisonerDomesticStatusRequest(
    domesticStatusCode = "D",
    createdBy = "user",
    createdTime = LocalDateTime.now(),
  )
}
