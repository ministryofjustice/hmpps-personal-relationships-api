package uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.resource.sync

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.PostgresIntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncUpdatePrisonerDomesticStatusRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync.SyncPrisonerDomesticStatusResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.PrisonerDomesticStatusRepository
import java.time.LocalDateTime

class PrisonerDomesticStatusSyncIntegrationTest : PostgresIntegrationTestBase() {

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
  fun `Sync endpoints should return forbidden without authorized role`(role: String) {
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

    webTestClient.delete()
      .uri("/sync/$prisonerNumber/domestic-status")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf(role)))
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `should get an existing prisoner domestic status`() {
    val domesticStatusToSync = SyncUpdatePrisonerDomesticStatusRequest(
      prisonerNumber = prisonerNumber,
      domesticStatusCode = "D",
      createdBy = "user",
      createdTime = LocalDateTime.now(),
      active = true,
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

    with(domesticStatus) {
      assertThat(prisonerNumber).isEqualTo(prisonerNumber)
      assertThat(domesticStatusCode).isEqualTo("D")
      assertThat(createdBy).isEqualTo("user")
      assertThat(active).isTrue()
    }
  }

  @Test
  fun `sync domestic status - create new record`() {
    // Given
    val domesticStatusToSync = SyncUpdatePrisonerDomesticStatusRequest(
      prisonerNumber = prisonerNumber,
      domesticStatusCode = "D",
      createdBy = "user",
      createdTime = LocalDateTime.now(),
      active = true,
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
      .ignoringFields("id", "createdBy", "createdTime")
      .isEqualTo(domesticStatusToSync)

    // Verify database state
    val savedDomesticStatus = domesticStatusRepository.findByPrisonerNumber(prisonerNumber)
    assertThat(savedDomesticStatus?.domesticStatusCode).isEqualTo("D")
    assertThat(savedDomesticStatus?.createdBy).isEqualTo("user")
  }

  @Test
  fun `sync domestic status - updates existing record as inactive and create new record`() {
    // Given
    val existingDomesticStatus = SyncUpdatePrisonerDomesticStatusRequest(
      prisonerNumber = prisonerNumber,
      domesticStatusCode = "D",
      createdBy = "user",
      createdTime = LocalDateTime.now(),
      active = true,
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
      prisonerNumber = prisonerNumber,
      domesticStatusCode = "M",
      createdBy = "user",
      createdTime = LocalDateTime.now(),
      active = true,
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
          prisonerNumber = prisonerNumber,
          domesticStatusCode = "M",
          createdBy = "user",
          createdTime = LocalDateTime.now(),
          active = true,
        ),
      )

    val updatedRecord = domesticStatusRepository.findByPrisonerNumberAndActive(prisonerNumber, true)
    assertThat(updatedRecord?.domesticStatusCode).isEqualTo("M")
    assertThat(updatedRecord?.createdBy).isEqualTo("user")

    val historicalRecord = domesticStatusRepository.findByPrisonerNumberAndActive(prisonerNumber, false)
    assertThat(historicalRecord?.domesticStatusCode).isEqualTo("D")
    assertThat(historicalRecord?.createdBy).isEqualTo("user")
  }

  @Test
  fun `sync domestic status - bad request when invalid data`() {
    // Given
    val invalidDomesticStatus = SyncUpdatePrisonerDomesticStatusRequest(
      prisonerNumber = prisonerNumber,
      domesticStatusCode = "DOM",
      createdBy = "user",
      createdTime = LocalDateTime.now(),
      active = true,
    )

    // When
    val response = webTestClient.put()
      .uri("/sync/$prisonerNumber/domestic-status")
      .headers(setAuthorisation(roles = listOf("PERSONAL_RELATIONSHIPS_MIGRATION")))
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(invalidDomesticStatus)
      .exchange()

    // Then
    response.expectStatus().isBadRequest
  }

  @Test
  fun `sync multiple domestic statuses - success`() {
    // Given
    val domesticStatusesToSync = SyncUpdatePrisonerDomesticStatusRequest(
      prisonerNumber = prisonerNumber,
      domesticStatusCode = "D",
      createdBy = "user",
      createdTime = LocalDateTime.now(),
      active = true,
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
          prisonerNumber = prisonerNumber,
          domesticStatusCode = "D",
          createdBy = "user",
          createdTime = LocalDateTime.now(),
          active = true,
        ),
      )

    // Verify database state
    val savedStatuses = domesticStatusRepository.findByPrisonerNumberAndActive(prisonerNumber, true)
    assertThat(savedStatuses).isNotNull
    assertThat(savedStatuses?.domesticStatusCode).isEqualTo("D")
    assertThat(savedStatuses?.createdBy).isEqualTo("user")
    assertThat(savedStatuses?.active).isTrue()
  }

  @Test
  fun `should set to inactive when deleting an existing domestic status`() {
    val domesticStatusesToSync = SyncUpdatePrisonerDomesticStatusRequest(
      prisonerNumber = prisonerNumber,
      domesticStatusCode = "D",
      createdBy = "user",
      createdTime = LocalDateTime.now(),
      active = true,
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
    val beforeCount = domesticStatusRepository.count()
    assertThat(beforeCount).isEqualTo((1))

    webTestClient.delete()
      .uri("/sync/$prisonerNumber/domestic-status")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("PERSONAL_RELATIONSHIPS_MIGRATION")))
      .exchange()
      .expectStatus()
      .isOk

    val afterCount = domesticStatusRepository.count()
    assertThat(afterCount).isEqualTo((1))
    val savedDomesticStatus = domesticStatusRepository.findByPrisonerNumberAndActive(prisonerNumber, false)
    assertThat(savedDomesticStatus?.domesticStatusCode).isEqualTo("D")
  }

  private fun aMinimalRequest() = SyncUpdatePrisonerDomesticStatusRequest(
    prisonerNumber = prisonerNumber,
    domesticStatusCode = "D",
    createdBy = "user",
    createdTime = LocalDateTime.now(),
    active = true,
  )
}
