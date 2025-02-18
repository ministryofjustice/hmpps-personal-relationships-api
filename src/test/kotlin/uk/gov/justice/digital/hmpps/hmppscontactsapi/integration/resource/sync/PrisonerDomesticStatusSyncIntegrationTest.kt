package uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.resource.sync

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.PrisonerDomesticStatus
import uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.SecureAPIIntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncUpdatePrisonerDomesticStatusRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.PrisonerDomesticStatusRepository
import java.time.LocalDateTime

class PrisonerDomesticStatusSyncIntegrationTest : SecureAPIIntegrationTestBase() {

  override val allowedRoles: Set<String> = setOf("ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__RW", "ROLE_CONTACTS__R")

  override fun baseRequestBuilder(): WebTestClient.RequestHeadersSpec<*> = webTestClient.post()
    .uri("/sync/domestic-status")
    .accept(MediaType.APPLICATION_JSON)
    .contentType(MediaType.APPLICATION_JSON)
    .bodyValue(aMinimalRequest())

  private val prisonerNumber = "A1234BC"

  @Autowired
  private lateinit var domesticStatusRepository: PrisonerDomesticStatusRepository

  @BeforeEach
  fun setUp() {
    domesticStatusRepository.deleteAll()
  }

  @Test
  fun `sync domestic status - success`() {
    // Given
    val domesticStatusToSync = SyncUpdatePrisonerDomesticStatusRequest(
      prisonerNumber = prisonerNumber,
      domesticStatusCode = "D",
      createdBy = "user",
      createdTime = LocalDateTime.now(),
      active = true,
    )

    // When
    val response = webTestClient.post()
      .uri("/sync/domestic-status")
      .headers(setAuthorisation(roles = listOf("ROLE_SYNC_CONTACTS")))
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(domesticStatusToSync)
      .exchange()

    // Then
    response.expectStatus().isOk
      .expectBody()
      .jsonPath("$.id").isEqualTo(123456)
      .jsonPath("$.personId").isEqualTo("A1234BC")
      .jsonPath("$.status").isEqualTo("MARRIED")

    // Verify database state
    val savedDomesticStatus = domesticStatusRepository.findById(123456L).get()
    assertThat(savedDomesticStatus.domesticStatusCode).isEqualTo("MARRIED")
    assertThat(savedDomesticStatus.createdBy).isEqualTo("UPDATED_USER")
  }

  @Test
  fun `sync domestic status - updates existing record`() {
    // Given
    val existingDomesticStatus = PrisonerDomesticStatus(
      id = 1,
      prisonerNumber = prisonerNumber,
      domesticStatusCode = "OLD_CODE",
      active = true,
      createdBy = "USER1",
      createdTime = LocalDateTime.now(),
    )
    domesticStatusRepository.save(existingDomesticStatus)

    val updatedDomesticStatus = SyncUpdatePrisonerDomesticStatusRequest(
      prisonerNumber = prisonerNumber,
      domesticStatusCode = "D",
      createdBy = "user",
      createdTime = LocalDateTime.now(),
      active = true,
    )

    // When
    val response = webTestClient.post()
      .uri("/sync/domestic-status")
      .headers(setAuthorisation(roles = listOf("ROLE_SYNC_CONTACTS")))
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(updatedDomesticStatus)
      .exchange()

    // Then
    response.expectStatus().isOk
      .expectBody()
      .jsonPath("$.status").isEqualTo("MARRIED")
      .jsonPath("$.modifiedBy").isEqualTo("UPDATED_USER")

    val updatedRecord = domesticStatusRepository.findById(123456L).get()
    assertThat(updatedRecord.domesticStatusCode).isEqualTo("MARRIED")
    assertThat(updatedRecord.createdBy).isEqualTo("UPDATED_USER")
  }

  @Test
  fun `sync domestic status - unauthorized without role`() {
    // Given
    val domesticStatusToSync = SyncUpdatePrisonerDomesticStatusRequest(
      prisonerNumber = prisonerNumber,
      domesticStatusCode = "D",
      createdBy = "user",
      createdTime = LocalDateTime.now(),
      active = true,
    )

    // When
    val response = webTestClient.post()
      .uri("/sync/domestic-status")
      .headers(setAuthorisation(roles = listOf()))
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(domesticStatusToSync)
      .exchange()

    // Then
    response.expectStatus().isUnauthorized
  }

  @Test
  fun `sync domestic status - bad request when invalid data`() {
    // Given
    val invalidDomesticStatus = SyncUpdatePrisonerDomesticStatusRequest(
      prisonerNumber = prisonerNumber,
      domesticStatusCode = "D",
      createdBy = "user",
      createdTime = LocalDateTime.now(),
      active = true,
    )

    // When
    val response = webTestClient.post()
      .uri("/sync/domestic-status")
      .headers(setAuthorisation(roles = listOf("ROLE_SYNC_CONTACTS")))
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(invalidDomesticStatus)
      .exchange()

    // Then
    response.expectStatus().isBadRequest
  }

  @Test
  fun `sync multiple domestic statuses - success`() {
    // Given
    val domesticStatusesToSync = listOf(
      SyncUpdatePrisonerDomesticStatusRequest(
        prisonerNumber = prisonerNumber,
        domesticStatusCode = "D",
        createdBy = "user",
        createdTime = LocalDateTime.now(),
        active = true,
      ),
      SyncUpdatePrisonerDomesticStatusRequest(
        prisonerNumber = prisonerNumber,
        domesticStatusCode = "D",
        createdBy = "user",
        createdTime = LocalDateTime.now(),
        active = true,
      ),
    )

    // When
    val response = webTestClient.post()
      .uri("/sync/domestic-status/list")
      .headers(setAuthorisation(roles = listOf("ROLE_SYNC_CONTACTS")))
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(domesticStatusesToSync)
      .exchange()

    // Then
    response.expectStatus().isOk
      .expectBody()
      .jsonPath("$[0].id").isEqualTo(123456)
      .jsonPath("$[0].status").isEqualTo("MARRIED")
      .jsonPath("$[1].id").isEqualTo(123457)
      .jsonPath("$[1].status").isEqualTo("SINGLE")

    // Verify database state
    val savedStatuses = domesticStatusRepository.findAllById(listOf(123456L, 123457L))
    assertThat(savedStatuses).hasSize(2)
    assertThat(savedStatuses.map { it.domesticStatusCode }).containsExactlyInAnyOrder("MARRIED", "SINGLE")
  }

  private fun aMinimalRequest() = SyncUpdatePrisonerDomesticStatusRequest(
    prisonerNumber = prisonerNumber,
    domesticStatusCode = "D",
    createdBy = "user",
    createdTime = LocalDateTime.now(),
    active = true,
  )
}
