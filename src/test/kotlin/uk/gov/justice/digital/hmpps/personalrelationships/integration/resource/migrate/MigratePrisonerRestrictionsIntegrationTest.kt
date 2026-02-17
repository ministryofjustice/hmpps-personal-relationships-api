package uk.gov.justice.digital.hmpps.personalrelationships.integration.resource.migrate

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.personalrelationships.integration.PostgresIntegrationTestBase
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.migrate.MigratePrisonerRestrictionRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.migrate.MigratePrisonerRestrictionsRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.migrate.PrisonerRestrictionDetailsRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.migrate.PrisonerRestrictionsMigrationResponse
import uk.gov.justice.digital.hmpps.personalrelationships.repository.PrisonerRestrictionsRepository
import uk.gov.justice.digital.hmpps.personalrelationships.util.StubUser
import java.time.LocalDate
import java.time.LocalDateTime

class MigratePrisonerRestrictionsIntegrationTest : PostgresIntegrationTestBase() {

  @Autowired
  lateinit var prisonerRestrictionsRepository: PrisonerRestrictionsRepository

  @BeforeEach
  fun setUp() {
    setCurrentUser(StubUser.SYNC_AND_MIGRATE_USER)
    prisonerRestrictionsRepository.deleteAll()
  }

  @Nested
  inner class PrisonerRestrictionsMerge {

    @Test
    fun `should return unauthorized if no token provided`() {
      webTestClient.post()
        .uri("/migrate/prisoner-restrictions")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(
          basicMigrationRequest(
            restrictionsList = listOf(
              prisonerRestrictionDetailsRequest(),
            ),
          ),
        )
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @ParameterizedTest
    @ValueSource(strings = ["ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__R", "ROLE_CONTACTS__RW"])
    fun `should return forbidden without an authorised role on the token`(authRole: String) {
      setCurrentUser(StubUser.SYNC_AND_MIGRATE_USER.copy(roles = listOf(authRole)))
      webTestClient.post()
        .uri("/migrate/prisoner-restrictions")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(
          basicMigrationRequest(
            restrictionsList = listOf(
              prisonerRestrictionDetailsRequest(),
            ),
          ),
        )
        .headers(setAuthorisationUsingCurrentUser())
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should migrate prisoner restrictions and return IDs`() {
      val request = basicMigrationRequest(
        restrictionsList = listOf(
          prisonerRestrictionDetailsRequest(),
          prisonerRestrictionDetailsRequest(restrictionType = "DIHCON"),
          prisonerRestrictionDetailsRequest(restrictionType = "NONCON"),
        ),
      )

      webTestClient.post()
        .uri("/migrate/prisoner-restrictions")
        .headers(setAuthorisationUsingCurrentUser())
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus()
        .isOk
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody(PrisonerRestrictionsMigrationResponse::class.java)
        .consumeWith { response ->
          assertThat(response.responseBody?.prisonerNumber).isEqualTo("A1234ZZ")
          assertThat(response.responseBody?.prisonerRestrictionsIds).isNotEmpty
        }

      val restrictions = prisonerRestrictionsRepository.findByPrisonerNumber("A1234ZZ")
      assertThat(restrictions).hasSize(3)
      assertThat(restrictions[0].restrictionType).isEqualTo("CCTV")
      assertThat(restrictions[0].commentText).isEqualTo("No visits allowed")

      assertThat(restrictions[1].restrictionType).isEqualTo("DIHCON")
      assertThat(restrictions[1].commentText).isEqualTo("No visits allowed")

      assertThat(restrictions[2].restrictionType).isEqualTo("NONCON")
      assertThat(restrictions[2].commentText).isEqualTo("No visits allowed")
    }

    @Test
    fun `should not migrate prisoner restrictions when comment text max size validation failed`() {
      val invalidRequest = basicMigrationRequest(
        restrictionsList = listOf(
          prisonerRestrictionDetailsRequest(
            commentText = "a".repeat(241),
            authorisedUsername = "JSMITH",
          ),
        ),
      )

      webTestClient.post()
        .uri("/migrate/prisoner-restrictions")
        .headers(setAuthorisationUsingCurrentUser())
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(invalidRequest)
        .exchange()
        .expectStatus()
        .isBadRequest
        .expectBody()
        .jsonPath("$.userMessage")
        .value { userMessage: String ->
          assertThat(userMessage).contains("restrictions[0].commentText must be less than or equal to 240 characters")
        }
    }

    @Test
    fun `should not migrate prisoner restrictions when restriction type max size validation failed`() {
      val invalidRequest = basicMigrationRequest(
        restrictionsList = listOf(
          prisonerRestrictionDetailsRequest(restrictionType = "LONGER_THAN_12_CHARACTERS"),
          prisonerRestrictionDetailsRequest(restrictionType = "invalid"),
        ),
      )

      webTestClient.post()
        .uri("/migrate/prisoner-restrictions")
        .headers(setAuthorisationUsingCurrentUser())
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(invalidRequest)
        .exchange()
        .expectStatus()
        .isBadRequest
        .expectBody()
        .jsonPath("$.userMessage")
        .isEqualTo(
          "Validation failure(s): " +
            "restrictions[0].restrictionType must be less than or equal to 12 characters",
        )
    }

    @Test
    fun `should not migrate prisoner restrictions when invalid restriction type`() {
      val invalidRequest = basicMigrationRequest(
        restrictionsList = listOf(
          prisonerRestrictionDetailsRequest(restrictionType = "CCTV"),
          prisonerRestrictionDetailsRequest(restrictionType = "invalid"),
        ),
      )

      webTestClient.post()
        .uri("/migrate/prisoner-restrictions")
        .headers(setAuthorisationUsingCurrentUser())
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(invalidRequest)
        .exchange()
        .expectStatus()
        .isNotFound
        .expectBody()
        .jsonPath("$.userMessage")
        .isEqualTo(
          "Entity not found : No reference data found for groupCode: ReferenceCodeGroup.RESTRICTION and code: invalid",
        )
    }

    @Test
    fun `should not migrate prisoner restrictions when restrictions list is empty`() {
      val invalidRequest = basicMigrationRequest(
        restrictionsList = emptyList(),
      )

      webTestClient.post()
        .uri("/migrate/prisoner-restrictions")
        .headers(setAuthorisationUsingCurrentUser())
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(invalidRequest)
        .exchange()
        .expectStatus()
        .isBadRequest
        .expectBody()
        .jsonPath("$.userMessage")
        .isEqualTo("Validation failure(s): restrictions must contain at least one record")
    }

    @Test
    fun `should delete existing restrictions before migration`() {
      webTestClient.post()
        .uri("/migrate/prisoner-restrictions")
        .headers(setAuthorisationUsingCurrentUser())
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(
          basicMigrationRequest(
            prisonerNumber = "A1234ZZ",
            restrictionsList = listOf(
              prisonerRestrictionDetailsRequest(restrictionType = "CCTV"),
              prisonerRestrictionDetailsRequest(restrictionType = "BAN"),
              prisonerRestrictionDetailsRequest(restrictionType = "CHILD"),
            ),
          ),
        )
        .exchange()
        .expectStatus()
        .isOk
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody(PrisonerRestrictionsMigrationResponse::class.java)
        .consumeWith { response ->
          assertThat(response.responseBody?.prisonerNumber).isEqualTo("A1234ZZ")
          assertThat(response.responseBody?.prisonerRestrictionsIds).hasSize(3)
        }

      val request = basicMigrationRequest(
        prisonerNumber = "A1234ZZ",
        restrictionsList = listOf(
          prisonerRestrictionDetailsRequest(restrictionType = "DIHCON"),
          prisonerRestrictionDetailsRequest(restrictionType = "NONCON"),
        ),
      )

      webTestClient.post()
        .uri("/migrate/prisoner-restrictions")
        .headers(setAuthorisationUsingCurrentUser())
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus()
        .isOk
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody(PrisonerRestrictionsMigrationResponse::class.java)
        .consumeWith { response ->
          assertThat(response.responseBody?.prisonerNumber).isEqualTo("A1234ZZ")
          assertThat(response.responseBody?.prisonerRestrictionsIds).hasSize(2)
        }

      val restrictions = prisonerRestrictionsRepository.findByPrisonerNumber("A1234ZZ")
      assertThat(restrictions).hasSize(2)
      assertThat(restrictions.map { it.restrictionType }).containsExactlyInAnyOrder("DIHCON", "NONCON")
    }
  }

  @Nested
  inner class PrisonerRestrictionMerge {
    @Test
    fun `should migrate a single prisoner restriction`() {
      val request = migratePrisonerRestrictionRequest()

      webTestClient.post()
        .uri("/migrate/prisoner-restriction/A1234ZZ")
        .headers(setAuthorisationUsingCurrentUser())
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus()
        .isOk
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody()
        .jsonPath("$.prisonerNumber").isEqualTo("A1234ZZ")
        .jsonPath("$.prisonerRestrictionId").isNumber

      val restrictions = prisonerRestrictionsRepository.findByPrisonerNumber("A1234ZZ")
      assertThat(restrictions).hasSize(1)
      assertThat(restrictions[0].restrictionType).isEqualTo("CCTV")
    }

    @Test
    fun `should not migrate a single prisoner restriction when restriction type is invalid`() {
      val request = migratePrisonerRestrictionRequest()

      webTestClient.post()
        .uri("/migrate/prisoner-restriction/A1234ZZ")
        .headers(setAuthorisationUsingCurrentUser())
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request.copy(restrictionType = "INVALID_TYPE"))
        .exchange()
        .expectStatus()
        .isNotFound
        .expectBody()
        .jsonPath("$.userMessage")
        .isEqualTo("Entity not found : No reference data found for groupCode: ReferenceCodeGroup.RESTRICTION and code: INVALID_TYPE")
    }

    @Test
    fun `should not migrate a single prisoner restriction when restrictionType exceeds max length`() {
      val request = migratePrisonerRestrictionRequest()

      webTestClient.post()
        .uri("/migrate/prisoner-restriction/A1234ZZ")
        .headers(setAuthorisationUsingCurrentUser())
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request.copy(restrictionType = "LONG_RESTRICTION_INVALID_TYPE"))
        .exchange()
        .expectStatus()
        .isBadRequest
        .expectBody()
        .jsonPath("$.userMessage")
        .value { userMessage: String ->
          assertThat(userMessage).contains("restrictionType must be less than or equal to 12 characters")
        }
    }

    @Test
    fun `should not migrate a single prisoner restriction when commentText exceeds max length`() {
      val request = migratePrisonerRestrictionRequest()

      webTestClient.post()
        .uri("/migrate/prisoner-restriction/A1234ZZ")
        .headers(setAuthorisationUsingCurrentUser())
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request.copy(commentText = "a".repeat(241)))
        .exchange()
        .expectStatus()
        .isBadRequest
        .expectBody()
        .jsonPath("$.userMessage")
        .value { userMessage: String ->
          assertThat(userMessage).contains("commentText must be less than or equal to 240 characters")
        }
    }

    private fun migratePrisonerRestrictionRequest(
      restrictionType: String = "CCTV",
      now: LocalDateTime = LocalDateTime.now(),
    ) = MigratePrisonerRestrictionRequest(
      restrictionType,
      effectiveDate = now.toLocalDate(),
      expiryDate = now.toLocalDate().plusDays(10),
      commentText = "CCTV",
      authorisedUsername = "JSMITH",
      currentTerm = true,
      createdBy = "user1",
      createdTime = now,
      updatedBy = "user2",
      updatedTime = now.plusDays(1),
    )
  }

  private fun basicMigrationRequest(
    prisonerNumber: String = "A1234ZZ",
    restrictionsList: List<PrisonerRestrictionDetailsRequest>,
  ) = MigratePrisonerRestrictionsRequest(
    prisonerNumber = prisonerNumber,
    restrictions = restrictionsList,
  )

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
