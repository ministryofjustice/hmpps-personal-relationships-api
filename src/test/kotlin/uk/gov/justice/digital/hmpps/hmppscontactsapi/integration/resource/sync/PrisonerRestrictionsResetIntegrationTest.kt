package uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.resource.sync

import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppscontactsapi.config.User
import uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.PostgresIntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.migrate.MigratePrisonerRestrictionsRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.migrate.PrisonerRestrictionDetailsRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.ResetPrisonerRestrictionsRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.migrate.PrisonerRestrictionsMigrationResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.PrisonerRestrictionsRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.PrisonerRestrictionsChanged
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.Source
import uk.gov.justice.digital.hmpps.hmppscontactsapi.util.StubUser
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.stream.Stream

class PrisonerRestrictionsResetIntegrationTest : PostgresIntegrationTestBase() {
  @Autowired
  private lateinit var prisonerRestrictionRepository: PrisonerRestrictionsRepository
  companion object {
    private const val RESETTING_PRISONER_NUMBER = "A1234BC"
    private const val RESET_URI = "/prisoner-restrictions/reset"

    private const val TEST_RESTRICTION_TYPE = "CCTV"

    @JvmStatic
    fun invalidResetRestrictionsRequestProvider(): Stream<Arguments> = Stream.of(
      Arguments.of(
        "Validation failure(s): prisonerNumber must not be blank",
        getPrisonerRestrictionDetailsRequest(
          listOf(
            prisonerRestrictionDetailsRequest(),
          ),
        ).copy(prisonerNumber = ""),
      ),
      Arguments.of(
        "restrictionType must be less than or equal to 12 characters",
        getPrisonerRestrictionDetailsRequest(
          listOf(
            prisonerRestrictionDetailsRequest().copy(restrictionType = "TOO_LONG_RESTRICTION_TYPE"),
          ),
        ),
      ),
      Arguments.of(
        "Validation failure(s): restrictions[0].commentText must be less than or equal to 240 characters",
        getPrisonerRestrictionDetailsRequest(
          listOf(
            prisonerRestrictionDetailsRequest().copy(commentText = "a".repeat(241)),
          ),
        ),
      ),
      Arguments.of(
        "Validation failure: restrictions[0].createdBy must not be null",
        """
          {
            "prisonerNumber": "A1234BC",
            "restrictions": [
              {
                "restrictionType": "CCTV",
                "effectiveDate": "2024-06-11",
                "expiryDate": "2024-12-31",
                "commentText": "No visits allowed",
                "authorisedUsername": 654321,
                "currentTerm": true,
                "createdTime": "2024-06-11T10:00:00",
                "updatedBy": null,
                "updatedTime": null
              }
            ]
          }
        """.trimIndent(),
      ),
      Arguments.of(
        "Validation failure: restrictions[0].authorisedUsername must not be null",
        """
        {
          "prisonerNumber": "A1234BC",
          "restrictions": [
            {
              "restrictionType": "CCTV",
              "effectiveDate": "2024-06-11",
              "expiryDate": "2024-12-31",
              "commentText": "No visits allowed",
              "createdTime": "2024-06-11T10:00:00",
              "currentTerm": true,
              "updatedBy": null,
              "updatedTime": null
            }
          ]
        }
        """.trimIndent(),
      ),
    )

    private fun getPrisonerRestrictionDetailsRequest(restrictions: List<PrisonerRestrictionDetailsRequest>) = ResetPrisonerRestrictionsRequest(
      prisonerNumber = RESETTING_PRISONER_NUMBER,
      restrictions,
    )

    private fun prisonerRestrictionDetailsRequest(
      restrictionType: String = TEST_RESTRICTION_TYPE,
      commentText: String = "Test comment",
      authorisedUsername: String = "user",
      createdBy: String = "user",
      updatedBy: String = "user",
      effectiveDate: LocalDate = LocalDate.now(),
      expiryDate: LocalDate = LocalDate.now().plusDays(1),
      currentTerm: Boolean = true,
    ) = PrisonerRestrictionDetailsRequest(
      restrictionType = restrictionType,
      effectiveDate = effectiveDate,
      expiryDate = expiryDate,
      commentText = commentText,
      authorisedUsername = authorisedUsername,
      currentTerm = currentTerm,
      createdBy = createdBy,
      createdTime = LocalDateTime.now(),
      updatedBy = updatedBy,
      updatedTime = LocalDateTime.now(),
    )

    fun aMinimalResetPrisonerRestrictionsRequest(prisonerNumber: String = RESETTING_PRISONER_NUMBER) = ResetPrisonerRestrictionsRequest(
      prisonerNumber = prisonerNumber,
      restrictions =
      listOf(
        prisonerRestrictionDetailsRequest(),
        prisonerRestrictionDetailsRequest(),
        prisonerRestrictionDetailsRequest(),
      ),
    )
  }

  @BeforeEach
  fun setUp() {
    setCurrentUser(StubUser.SYNC_AND_MIGRATE_USER)
    prisonerRestrictionRepository.deleteAll()
    stubEvents.reset()
  }

  @Test
  fun `should return unauthorized when no token provided for reset`() {
    webTestClient.post()
      .uri(RESET_URI)
      .accept(MediaType.APPLICATION_JSON)
      .bodyValue(aMinimalResetPrisonerRestrictionsRequest())
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `should return forbidden when user lacks authorised role for reset`() {
    setCurrentUser(StubUser.USER_WITH_NO_ROLES)
    webTestClient.post()
      .uri(RESET_URI)
      .headers(setAuthorisationUsingCurrentUser())
      .accept(MediaType.APPLICATION_JSON)
      .bodyValue(aMinimalResetPrisonerRestrictionsRequest())
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun `should delete all restrictions and add new ones and then fire events`() {
    migratePrisonerRestrictions()

    val request = aMinimalResetPrisonerRestrictionsRequest()
    webTestClient.post()
      .uri(RESET_URI)
      .headers(setAuthorisationUsingCurrentUser())
      .accept(MediaType.APPLICATION_JSON)
      .bodyValue(request)
      .exchange()
      .expectStatus().isOk
      .expectBody()

    stubEvents.assertHasEvent(
      event = OutboundEvent.PRISONER_RESTRICTIONS_CHANGED,
      additionalInfo = PrisonerRestrictionsChanged(
        keepingPrisonerNumber = request.prisonerNumber,
        removingPrisonerNumber = null,
        source = Source.NOMIS,
        username = User.SYS_USER.username,
        activeCaseLoadId = null,
      ),
      personReference = null,
    )
  }

  @ParameterizedTest
  @MethodSource("invalidResetRestrictionsRequestProvider")
  fun `should return 400 for invalid reset request`(expectedMessage: String, invalidRequest: Any) {
    webTestClient.post()
      .uri(RESET_URI)
      .headers(setAuthorisationUsingCurrentUser())
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(invalidRequest)
      .exchange()
      .expectStatus().isBadRequest
      .expectBody()
      .jsonPath("$.userMessage").value<String> {
        assertThat(it).contains(expectedMessage)
      }
  }

  @Test
  fun `should not reset prisoner restrictions when restrictions list is empty`() {
    val invalidRequest = ResetPrisonerRestrictionsRequest(
      prisonerNumber = RESETTING_PRISONER_NUMBER,
      restrictions = emptyList(),
    )

    webTestClient.post()
      .uri(RESET_URI)
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
  fun `should return not found when restriction type is not found`() {
    val invalidRequest = ResetPrisonerRestrictionsRequest(
      prisonerNumber = RESETTING_PRISONER_NUMBER,
      restrictions = listOf(
        prisonerRestrictionDetailsRequest("CCTV"),
        prisonerRestrictionDetailsRequest("NOT_FOUND"),
      ),
    )

    webTestClient.post()
      .uri(RESET_URI)
      .headers(setAuthorisationUsingCurrentUser())
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(invalidRequest)
      .exchange()
      .expectStatus()
      .isBadRequest
      .expectBody()
      .jsonPath("$.userMessage")
      .isEqualTo("Validation failure: Unsupported restriction type (NOT_FOUND)")
  }

  private fun migratePrisonerRestrictions(): PrisonerRestrictionsMigrationResponse = webTestClient.post()
    .uri("/migrate/prisoner-restrictions")
    .headers(setAuthorisationUsingCurrentUser())
    .contentType(MediaType.APPLICATION_JSON)
    .bodyValue(
      MigratePrisonerRestrictionsRequest(
        prisonerNumber = RESETTING_PRISONER_NUMBER,
        restrictions = listOf(prisonerRestrictionDetailsRequest(), prisonerRestrictionDetailsRequest()),
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
