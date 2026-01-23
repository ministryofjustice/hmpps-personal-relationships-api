package uk.gov.justice.digital.hmpps.personalrelationships.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.personalrelationships.helpers.prisoner
import uk.gov.justice.digital.hmpps.personalrelationships.integration.SecureAPIIntegrationTestBase
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.migrate.MigratePrisonerRestrictionsRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.migrate.PrisonerRestrictionDetailsRequest
import uk.gov.justice.digital.hmpps.personalrelationships.util.StubUser
import java.time.LocalDate
import java.time.LocalDateTime

class GetPrisonerRestrictionsIntegrationTest : SecureAPIIntegrationTestBase() {

  private val prisonerNumber = "A1234BC"

  override val allowedRoles: Set<String> = setOf("ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__RW", "ROLE_CONTACTS__R")

  @BeforeEach
  fun setUp() {
    setCurrentUser(StubUser.READ_ONLY_USER)
    initialiseData()
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
    val response = testAPIClient.getPrisonerRestrictions(
      prisonerNumber,
      currentTermOnly = true,
      paged = true,
      page = 0,
      size = 10,
    )

    // 12 current-term restrictions created; page size 10 should return 10
    assertThat(response.page.totalElements).isEqualTo(12)
    assertThat(response.content.size).isEqualTo(10)
    assertThat(response.content.all { it.currentTerm }).isTrue()
  }

  @Test
  fun `should return remaining prisoner restrictions on second page when currentTermOnly`() {
    val response = testAPIClient.getPrisonerRestrictions(
      prisonerNumber,
      currentTermOnly = true,
      paged = true,
      page = 1,
      size = 10,
    )

    // Remaining 2 current-term records on page 1
    assertThat(response.page.totalElements).isEqualTo(12)
    assertThat(response.content.size).isEqualTo(2)
    assertThat(response.content.all { it.currentTerm }).isTrue()
  }

  @Test
  fun `should return all prisoner restrictions when paged is false`() {
    val response = testAPIClient.getPrisonerRestrictions(
      prisonerNumber,
      currentTermOnly = false,
      paged = false,
    )

    // 20 total records created
    assertThat(response.page.totalElements).isEqualTo(20)
    assertThat(response.content.size).isEqualTo(20)
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
    // Create 20 total restrictions: 12 current-term, 8 non-current-term
    val currentTermRestrictions = (1..12).map { i ->
      PrisonerRestrictionDetailsRequest(
        restrictionType = if (i % 2 == 0) "CCTV" else "BAN",
        effectiveDate = LocalDate.of(2023, 1, 1).plusDays(i.toLong()),
        expiryDate = LocalDate.of(2023, 12, 31).plusDays(i.toLong()),
        commentText = "Current term restriction #$i",
        authorisedUsername = if (i % 2 == 0) "JSMITH" else "ASMITH",
        currentTerm = true,
        createdBy = if (i % 2 == 0) "user1" else "user3",
        createdTime = LocalDateTime.of(2023, 1, 1, 11, 15, 15).plusDays(i.toLong()),
        updatedBy = if (i % 2 == 0) "user2" else "user4",
        updatedTime = LocalDateTime.of(2023, 2, 1, 11, 15, 15).plusDays(i.toLong()),
      )
    }
    val nonCurrentTermRestrictions = (1..8).map { i ->
      PrisonerRestrictionDetailsRequest(
        restrictionType = if (i % 2 == 0) "BAN" else "CCTV",
        effectiveDate = LocalDate.of(2022, 6, 1).plusDays(i.toLong()),
        expiryDate = LocalDate.of(2022, 12, 1).plusDays(i.toLong()),
        commentText = "Non-current term restriction #$i",
        authorisedUsername = if (i % 2 == 0) "JSMITH" else "ASMITH",
        currentTerm = false,
        createdBy = if (i % 2 == 0) "user3" else "user1",
        createdTime = LocalDateTime.of(2022, 6, 1, 11, 15, 15).plusDays(i.toLong()),
        updatedBy = if (i % 2 == 0) "user4" else "user2",
        updatedTime = LocalDateTime.of(2022, 7, 1, 11, 15, 15).plusDays(i.toLong()),
      )
    }
    val request = MigratePrisonerRestrictionsRequest(
      prisonerNumber = prisonerNumber,
      restrictions = currentTermRestrictions + nonCurrentTermRestrictions,
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
