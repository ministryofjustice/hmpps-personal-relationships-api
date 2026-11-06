package uk.gov.justice.digital.hmpps.personalrelationships.integration.resource

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.personalrelationships.entity.PrisonerRestriction
import uk.gov.justice.digital.hmpps.personalrelationships.integration.SecureAPIIntegrationTestBase
import uk.gov.justice.digital.hmpps.personalrelationships.repository.PrisonerRestrictionsRepository
import uk.gov.justice.digital.hmpps.personalrelationships.util.StubUser
import java.time.LocalDate
import java.time.LocalDateTime

// Interim SAR coverage; remove with SubjectAccessRequestService once restrictions are migrated into Alerts.
class SubjectAccessRequestIntegrationTest : SecureAPIIntegrationTestBase() {
  private val prisonerNumber = "S1234AR"

  @Autowired
  private lateinit var repository: PrisonerRestrictionsRepository

  @Autowired
  private lateinit var jdbcTemplate: JdbcTemplate

  override val allowedRoles: Set<String> = setOf("ROLE_SAR_DATA_ACCESS")

  override fun baseRequestBuilder(): WebTestClient.RequestHeadersSpec<*> = webTestClient.get()
    .uri("/subject-access-request?prn=$prisonerNumber")

  @BeforeEach
  fun setUp() {
    jdbcTemplate.update("delete from prisoner_restrictions where prisoner_number = ?", prisonerNumber)
    setCurrentUser(StubUser("sar_client", "SAR", listOf("ROLE_SAR_DATA_ACCESS"), isSystemUser = true))
  }

  @AfterEach
  fun tearDown() {
    setCurrentUser(null)
  }

  @Test
  fun `should return the restrictions for the prisoner`() {
    val first = repository.saveAndFlush(restriction("CCTV"))
    val second = repository.saveAndFlush(restriction("BAN"))

    val body = getSar()

    val restrictions = body["content"]["prisonerRestrictions"]
    assertThat(restrictions.map { it["prisonerRestrictionId"].asLong() })
      .containsExactly(first.prisonerRestrictionId, second.prisonerRestrictionId)
    assertThat(restrictions[0]["restrictionType"].asText()).isEqualTo("CCTV")
  }

  @Test
  fun `should return no content when the prisoner has no restrictions`() {
    webTestClient.get()
      .uri("/subject-access-request?prn=$prisonerNumber")
      .headers(setAuthorisationUsingCurrentUser())
      .exchange()
      .expectStatus().isNoContent
  }

  private fun getSar(): JsonNode = ObjectMapper().readTree(
    webTestClient.get()
      .uri("/subject-access-request?prn=$prisonerNumber")
      .headers(setAuthorisationUsingCurrentUser())
      .exchange()
      .expectStatus().isOk
      .expectBody(String::class.java)
      .returnResult().responseBody!!,
  )

  private fun restriction(type: String) = PrisonerRestriction(
    prisonerNumber = prisonerNumber,
    restrictionType = type,
    effectiveDate = LocalDate.now(),
    authorisedUsername = "AUTH",
    currentTerm = true,
    createdBy = "creator",
    createdTime = LocalDateTime.now(),
  )
}
