package uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppscontactsapi.helpers.prisoner
import uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.SecureAPIIntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.migrate.MigratePrisonerRestrictionsRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.migrate.PrisonerRestrictionDetailsRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.PrisonerRestrictionDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.util.StubUser
import java.time.LocalDate
import java.time.LocalDateTime

class GetPrisonerRestrictionsIntegrationTest : SecureAPIIntegrationTestBase() {

  private val prisonerNumber = "A1234BC"

  override val allowedRoles: Set<String> = setOf("ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__RW", "ROLE_CONTACTS__R")

  @BeforeEach
  fun setUp() {
    setCurrentUser(StubUser.READ_ONLY_USER)
  }

  override fun baseRequestBuilder(): WebTestClient.RequestHeadersSpec<*> = webTestClient.get()
    .uri("/prisoner-restrictions/$prisonerNumber")

  @Test
  fun `should return empty results when there are no restrictions`() {
    val response = testAPIClient.getPrisonerRestrictions("UNKNOWN")

    assertThat(response.page.totalElements).isEqualTo(0)
    assertThat(response.content.size).isEqualTo(0)
  }

  @Test
  fun `should return prisoner restrictions with paging and currentTermOnly`() {
    initialiseData()
    val response = testAPIClient.getPrisonerRestrictions(
      prisonerNumber,
      currentTermOnly = true,
      page = 0,
      size = 10,
    )

    assertThat(response.page.totalElements).isEqualTo(2)
    assertThat(response.content).isEqualTo(
      listOf(
        PrisonerRestrictionDetails(
          prisonerRestrictionId = 1L,
          prisonerNumber = prisonerNumber,
          restrictionType = "CCTV",
          effectiveDate = LocalDate.of(2023, 2, 3),
          expiryDate = LocalDate.of(2023, 12, 3),
          commentText = "Current term restriction",
          authorisedUsername = "JSMITH",
          currentTerm = true,
          createdBy = "user1",
          createdTime = LocalDateTime.of(2023, 2, 3, 11, 15, 15),
          updatedBy = "user2",
          updatedTime = LocalDateTime.of(2023, 3, 3, 11, 15, 15),
        ),
        PrisonerRestrictionDetails(
          prisonerRestrictionId = 2L,
          prisonerNumber = prisonerNumber,
          restrictionType = "BAN",
          effectiveDate = LocalDate.of(2023, 2, 3),
          expiryDate = LocalDate.of(2023, 12, 3),
          commentText = "NON Current term restriction",
          authorisedUsername = "ASMITH",
          currentTerm = true,
          createdBy = "user3",
          createdTime = LocalDateTime.of(2023, 2, 3, 11, 15, 15),
          updatedBy = "user4",
          updatedTime = LocalDateTime.of(2023, 3, 3, 11, 15, 15),
        ),
      ),
    )
  }

  private fun initialiseData() {
    stubPrisonerSearch(
      prisoner(
        prisonerNumber = prisonerNumber,
        prisonId = "MDI",
        firstName = "Joe",
        middleNames = "Middle",
        lastName = "Bloggs",
      ),
    )
    val request = MigratePrisonerRestrictionsRequest(
      prisonerNumber = prisonerNumber,
      restrictions = listOf(
        PrisonerRestrictionDetailsRequest(
          restrictionType = "CCTV",
          effectiveDate = LocalDate.of(2023, 2, 3),
          expiryDate = LocalDate.of(2023, 12, 3),
          commentText = "Current term restriction",
          authorisedUsername = "JSMITH",
          currentTerm = true,
          createdBy = "user1",
          createdTime = LocalDateTime.of(2023, 2, 3, 11, 15, 15),
          updatedBy = "user2",
          updatedTime = LocalDateTime.of(2023, 3, 3, 11, 15, 15),
        ),
        PrisonerRestrictionDetailsRequest(
          restrictionType = "BAN",
          effectiveDate = LocalDate.of(2023, 2, 3),
          expiryDate = LocalDate.of(2023, 12, 3),
          commentText = "NON Current term restriction",
          authorisedUsername = "ASMITH",
          currentTerm = true,
          createdBy = "user3",
          createdTime = LocalDateTime.of(2023, 2, 3, 11, 15, 15),
          updatedBy = "user4",
          updatedTime = LocalDateTime.of(2023, 3, 3, 11, 15, 15),
        ),
      ),
    )
    setCurrentUser(StubUser.SYNC_AND_MIGRATE_USER)
    webTestClient.post()
      .uri("/migrate/prisoner-restrictions")
      .headers(setAuthorisationUsingCurrentUser())
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(request)
      .exchange()
      .expectStatus().isOk
    setCurrentUser(StubUser.READ_ONLY_USER)
  }
}
