package uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.resource.sync

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.web.util.UriBuilder
import uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.PostgresIntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.helper.TestAPIClient.PrisonerRestrictionIdResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.migrate.MigratePrisonerRestrictionsRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.migrate.PrisonerRestrictionDetailsRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.MergePrisonerRestrictionsRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.MergedRestrictionsResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.migrate.PrisonerRestrictionsMigrationResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.PrisonerRestrictionsRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.PersonReference
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.PrisonerRestrictionInfo
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.Source
import uk.gov.justice.digital.hmpps.hmppscontactsapi.util.StubUser
import java.net.URI
import java.time.LocalDate
import java.time.LocalDateTime

class PrisonerRestrictionsAdminIntegrationTest : PostgresIntegrationTestBase() {

  companion object {
    private const val KEEP_PRISONER = "A1234AA"
    private const val REMOVE_PRISONER = "B1234BB"
    private const val MERGE_URI = "/prisoner-restrictions/merge"
  }

  @Autowired
  private lateinit var prisonerRestrictionRepository: PrisonerRestrictionsRepository

  @BeforeEach
  fun setUp() {
    stubEvents.reset()
    prisonerRestrictionRepository.deleteAll()
    setCurrentUser(StubUser.SYNC_AND_MIGRATE_USER)
  }

  @Nested
  inner class Merge {
    @Test
    fun `should return unauthorized when no token provided for merge`() {
      webTestClient.post()
        .uri(MERGE_URI)
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `should return forbidden when user lacks authorised role for merge`() {
      setCurrentUser(StubUser.USER_WITH_NO_ROLES)
      webTestClient.post()
        .uri(MERGE_URI)
        .headers(setAuthorisationUsingCurrentUser())
        .bodyValue(
          MergePrisonerRestrictionsRequest(
            keepingPrisonerNumber = KEEP_PRISONER,
            removingPrisonerNumber = REMOVE_PRISONER,
            restrictions = listOf(prisonerRestrictionDetailsRequest(), prisonerRestrictionDetailsRequest()),
          ),
        )
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `should fire individual prisoner restriction created and deleted events when restrictions are merged`() {
      migratePrisonerRestrictions(KEEP_PRISONER)
      migratePrisonerRestrictions(REMOVE_PRISONER)

      val responseBody = performMerge()

      assertPrisonerRestrictionsPresent()

      // Verify that a PRISONER_RESTRICTION_CREATED event was fired for each created restriction
      responseBody.createdRestrictions.forEach { restrictionId ->
        stubEvents.assertHasEvent(
          event = OutboundEvent.PRISONER_RESTRICTION_CREATED,
          additionalInfo = PrisonerRestrictionInfo(
            prisonerRestrictionId = restrictionId,
            source = Source.NOMIS,
            username = "SYS",
            activeCaseLoadId = null,
          ),
          personReference = PersonReference(KEEP_PRISONER),
        )
      }
      // Verify that a PRISONER_RESTRICTION_DELETED event was fired for the removed prisoner
      responseBody.deletedRestrictions.forEach { restrictionId ->
        stubEvents.assertHasEvent(
          event = OutboundEvent.PRISONER_RESTRICTION_DELETED,
          additionalInfo = PrisonerRestrictionInfo(
            prisonerRestrictionId = restrictionId,
            source = Source.NOMIS,
            username = "SYS",
            activeCaseLoadId = null,
          ),
          personReference = PersonReference(REMOVE_PRISONER),
        )
      }
    }
  }

  @Nested
  inner class Reconcile {
    @Test
    fun `should return unauthorized when no token provided for reconcile`() {
      webTestClient.get()
        .uri {
          getReconcileUrl(it)
        }
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `should return forbidden when user lacks authorised role for reconcile`() {
      setCurrentUser(StubUser.USER_WITH_NO_ROLES)
      webTestClient.get()
        .uri {
          getReconcileUrl(it)
        }
        .headers(setAuthorisationUsingCurrentUser())
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `should return paged restriction IDs for all prisoners using paged reconcile endpoint`() {
      val prisonerNumbers = listOf("A1234AA", "A1234AB", "A1234AC")
      prisonerNumbers.forEach { migratePrisonerRestrictions(it) }

      val response = webTestClient.get()
        .uri {
          getReconcileUrl(it)
        }
        .headers(setAuthorisationUsingCurrentUser())
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus().isOk
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody(PrisonerRestrictionIdResponse::class.java)
        .returnResult().responseBody

      assertThat(response).isNotNull
      with(response!!) {
        assertThat(page.totalElements).isGreaterThanOrEqualTo(3)
        assertThat(content).hasSize(3)
        assertThat(content).extracting("prisonerRestrictionId").hasSize(3)
      }
    }

    private fun getReconcileUrl(builder: UriBuilder): URI = builder.path("/prisoner-restrictions/reconcile")
      .queryParam("size", 10)
      .queryParam("sort", "prisonerRestrictionId,asc")
      .build()
  }

  // --- Helper methods below ---

  private fun performMerge() = webTestClient.post()
    .uri(MERGE_URI)
    .headers(setAuthorisationUsingCurrentUser())
    .contentType(MediaType.APPLICATION_JSON)
    .bodyValue(
      MergePrisonerRestrictionsRequest(
        keepingPrisonerNumber = KEEP_PRISONER,
        removingPrisonerNumber = REMOVE_PRISONER,
        restrictions = listOf(prisonerRestrictionDetailsRequest(), prisonerRestrictionDetailsRequest()),
      ),
    )
    .exchange()
    .expectStatus().isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody(MergedRestrictionsResponse::class.java)
    .returnResult().responseBody!!

  private fun assertPrisonerRestrictionsPresent() {
    // to be replaced with get method once the endpoint is available
    val prisonerRestrictions = prisonerRestrictionRepository.findAll()
    assertThat(prisonerRestrictions).hasSize(2)
    assertThat(prisonerRestrictions[0].restrictionType).isEqualTo("CCTV")
    assertThat(prisonerRestrictions[0].commentText).isEqualTo("No visits allowed")
    assertThat(prisonerRestrictions[0].authorisedUsername).isEqualTo("JSMITH")
    assertThat(prisonerRestrictions[0].createdBy).isEqualTo("user1")
    assertThat(prisonerRestrictions[0].createdTime).isNotNull
    assertThat(prisonerRestrictions[0].updatedBy).isEqualTo("user2")
    assertThat(prisonerRestrictions[0].updatedTime).isNotNull
    assertThat(prisonerRestrictions[0].currentTerm).isTrue
    assertThat(prisonerRestrictions[0].effectiveDate).isEqualTo(LocalDate.of(2024, 1, 1))
    assertThat(prisonerRestrictions[0].expiryDate).isEqualTo(LocalDate.of(2024, 12, 31))

    assertThat(prisonerRestrictions[1].restrictionType).isEqualTo("CCTV")
    assertThat(prisonerRestrictions[0].commentText).isEqualTo("No visits allowed")
    assertThat(prisonerRestrictions[1].createdBy).isEqualTo("user1")
    assertThat(prisonerRestrictions[1].authorisedUsername).isEqualTo("JSMITH")
    assertThat(prisonerRestrictions[1].createdTime).isNotNull
    assertThat(prisonerRestrictions[1].updatedBy).isEqualTo("user2")
    assertThat(prisonerRestrictions[1].updatedTime).isNotNull
    assertThat(prisonerRestrictions[1].currentTerm).isTrue
    assertThat(prisonerRestrictions[1].effectiveDate).isEqualTo(LocalDate.of(2024, 1, 1))
    assertThat(prisonerRestrictions[1].expiryDate).isEqualTo(LocalDate.of(2024, 12, 31))
  }

  private fun migratePrisonerRestrictions(prisonerNumber: String): PrisonerRestrictionsMigrationResponse = webTestClient.post()
    .uri("/migrate/prisoner-restrictions")
    .headers(setAuthorisationUsingCurrentUser())
    .contentType(MediaType.APPLICATION_JSON)
    .bodyValue(
      MigratePrisonerRestrictionsRequest(
        prisonerNumber = prisonerNumber,
        restrictions = listOf(prisonerRestrictionDetailsRequest()),
      ),
    )
    .exchange()
    .expectStatus()
    .isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody(PrisonerRestrictionsMigrationResponse::class.java)
    .returnResult().responseBody!!

  private fun prisonerRestrictionDetailsRequest(
    restrictionType: String = "CCTV",
    commentText: String = "No visits allowed",
    authorisedUsername: String = "JSMITH",
  ) = PrisonerRestrictionDetailsRequest(
    restrictionType,
    effectiveDate = LocalDate.of(2024, 1, 1),
    expiryDate = LocalDate.of(2024, 12, 31),
    commentText,
    authorisedUsername,
    currentTerm = true,
    createdBy = "user1",
    createdTime = LocalDateTime.now(),
    updatedBy = "user2",
    updatedTime = LocalDateTime.now().plusDays(1),
  )
}
