package uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.resource.sync

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.PostgresIntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncUpdatePrisonerDomesticStatusRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync.SyncPrisonerDomesticStatusResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.PrisonerDomesticStatusRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.PersonReference
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.PrisonerDomesticStatus
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.Source
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.time.LocalDateTime

class SyncPrisonerDomesticStatusIntegrationTest : PostgresIntegrationTestBase() {

  private val prisonerNumber = "A1234BC"

  @Autowired
  private lateinit var domesticStatusRepository: PrisonerDomesticStatusRepository

  @BeforeEach
  fun setUp() {
    domesticStatusRepository.deleteAll()
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
    webTestClient.get()
      .uri("/sync/$prisonerNumber/domestic-status")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf(role)))
      .exchange()
      .expectStatus()
      .isForbidden

    webTestClient.put()
      .uri("/sync/$prisonerNumber/domestic-status")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(aMinimalRequest())
      .headers(setAuthorisation(roles = listOf(role)))
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @ParameterizedTest
  @CsvSource(
    value = [
      "D;{\n" +
        "  \"id\": 1,\n" +
        "  \"domesticStatusCode\": \"D\",\n" +
        "  \"active\": true,\n" +
        "  \"createdTime\": \"2025-02-26T12:25:21.723Z\",\n" +
        "  \"createdBy\": \"string\"\n" +
        "}",
      "null;{\n" +
        "  \"id\": 1,\n" +
        "  \"domesticStatusCode\": null,\n" +
        "  \"active\": true,\n" +
        "  \"createdTime\": \"2025-02-26T12:25:21.723Z\",\n" +
        "  \"createdBy\": \"string\"\n" +
        "}",
    ],
    delimiter = ';',
  )
  fun `should get an existing prisoner domestic status`(domesticStatusCode: String?, json: String) {
    val domesticStatusToSync = SyncUpdatePrisonerDomesticStatusRequest(
      domesticStatusCode = domesticStatusCode,
      createdBy = "user",
      createdTime = LocalDateTime.now(),
    )

    // When
    webTestClient.put()
      .uri("/sync/$prisonerNumber/domestic-status")
      .headers(setAuthorisation(roles = listOf("PERSONAL_RELATIONSHIPS_MIGRATION")))
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
      .headers(setAuthorisation(roles = listOf("PERSONAL_RELATIONSHIPS_MIGRATION")))
      .exchange()
      .expectStatus()
      .isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(SyncPrisonerDomesticStatusResponse::class.java)
      .returnResult().responseBody!!

    assertThat(domesticStatus.id).isGreaterThan(0)
    assertThat(domesticStatus.domesticStatusCode).isEqualTo(domesticStatusCode)
    assertThat(domesticStatus.createdBy).isEqualTo("user")
    assertThat(domesticStatus.createdTime).isNotNull
    assertThat(domesticStatus.active).isTrue
  }

  @Test
  fun `should create a new domestic status record`() {
    // Given
    val domesticStatusToSync = SyncUpdatePrisonerDomesticStatusRequest(
      domesticStatusCode = "D",
      createdBy = "user",
      createdTime = LocalDateTime.now(),
    )

    // When
    val response = webTestClient.put()
      .uri("/sync/$prisonerNumber/domestic-status")
      .headers(setAuthorisation(roles = listOf("PERSONAL_RELATIONSHIPS_MIGRATION")))
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
          domesticStatusCode = "D",
          createdBy = "user",
          createdTime = LocalDateTime.now(),
          active = true,
        ),
      )

    // Verify database state
    val savedDomesticStatus = webTestClient.get()
      .uri("/sync/$prisonerNumber/domestic-status")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("PERSONAL_RELATIONSHIPS_MIGRATION")))
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
    assertThat(savedDomesticStatus.domesticStatusCode).isEqualTo("D")
    assertThat(savedDomesticStatus.createdBy).isEqualTo("user")
    stubEvents.assertHasEvent(
      event = OutboundEvent.PRISONER_DOMESTIC_STATUS_CREATED,
      additionalInfo = PrisonerDomesticStatus(savedDomesticStatus.id, Source.NOMIS),
      personReference = PersonReference(nomsNumber = prisonerNumber),
    )
  }

  @Test
  fun `should updates existing record as inactive and create new record`() {
    // Given
    val existingDomesticStatus = SyncUpdatePrisonerDomesticStatusRequest(
      domesticStatusCode = "D",
      createdBy = "user",
      createdTime = LocalDateTime.now(),
    )
    val existingResponse = webTestClient.put()
      .uri("/sync/$prisonerNumber/domestic-status")
      .headers(setAuthorisation(roles = listOf("PERSONAL_RELATIONSHIPS_MIGRATION")))
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
      .headers(setAuthorisation(roles = listOf("PERSONAL_RELATIONSHIPS_MIGRATION")))
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
      .headers(setAuthorisation(roles = listOf("PERSONAL_RELATIONSHIPS_MIGRATION")))
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

    val historicalRecord = domesticStatusRepository.findByPrisonerNumberAndActive(prisonerNumber, false)
    assertThat(historicalRecord?.domesticStatusCode).isEqualTo("D")
    assertThat(historicalRecord?.createdBy).isEqualTo("user")
    if (historicalRecord != null) {
      stubEvents.assertHasEvent(
        event = OutboundEvent.PRISONER_DOMESTIC_STATUS_UPDATED,
        additionalInfo = PrisonerDomesticStatus(historicalRecord.prisonerDomesticStatusId, Source.NOMIS),
        personReference = PersonReference(nomsNumber = prisonerNumber),
      )
    }

    stubEvents.assertHasEvent(
      event = OutboundEvent.PRISONER_DOMESTIC_STATUS_CREATED,
      additionalInfo = PrisonerDomesticStatus(savedDomesticStatus.id, Source.NOMIS),
      personReference = PersonReference(nomsNumber = prisonerNumber),
    )
  }

  @Test
  fun `sync domestic status - bad request when invalid data`() {
    // Given
    val invalidDomesticStatus = SyncUpdatePrisonerDomesticStatusRequest(
      domesticStatusCode = "DOM",
      createdBy = "user",
      createdTime = LocalDateTime.now(),
    )

    // When
    val response = webTestClient.put()
      .uri("/sync/$prisonerNumber/domestic-status")
      .headers(setAuthorisation(roles = listOf("PERSONAL_RELATIONSHIPS_MIGRATION")))
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(invalidDomesticStatus)
      .exchange()
      .expectStatus().isBadRequest
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody!!

    assertThat(response.developerMessage).contains("domesticStatusCode must be exactly 1 character")
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
      .headers(setAuthorisation(roles = listOf("PERSONAL_RELATIONSHIPS_MIGRATION")))
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
      .headers(setAuthorisation(roles = listOf("PERSONAL_RELATIONSHIPS_MIGRATION")))
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
