package uk.gov.justice.digital.hmpps.personalrelationships.integration.resource

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import uk.gov.justice.digital.hmpps.personalrelationships.entity.PrisonerRestriction
import uk.gov.justice.digital.hmpps.personalrelationships.integration.PostgresIntegrationTestBase
import uk.gov.justice.digital.hmpps.personalrelationships.repository.PrisonerRestrictionsRepository
import uk.gov.justice.digital.hmpps.personalrelationships.util.StubUser
import java.time.LocalDate
import java.time.LocalDateTime

class SubjectAccessRequestIntegrationTest : PostgresIntegrationTestBase() {
  private val prisonerNumber = "S1234AR"

  @Autowired
  private lateinit var repository: PrisonerRestrictionsRepository

  @Autowired
  private lateinit var jdbcTemplate: JdbcTemplate

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
  fun `should return restrictions but exclude soft deleted ones`() {
    val kept = repository.saveAndFlush(restriction("CCTV"))
    val deleted = repository.saveAndFlush(restriction("BAN"))
    repository.saveAndFlush(deleted.softDeleted("deleter"))

    val body = getSar()

    val restrictions = body["content"]["prisonerRestrictions"]
    assertThat(restrictions.map { it["prisonerRestrictionId"].asLong() }).containsExactly(kept.prisonerRestrictionId)
    assertThat(restrictions[0]["restrictionType"].asText()).isEqualTo("CCTV")
  }

  @Test
  fun `should return no content when the only restriction is soft deleted`() {
    val deleted = repository.saveAndFlush(restriction("BAN"))
    repository.saveAndFlush(deleted.softDeleted("deleter"))

    webTestClient.get()
      .uri("/subject-access-request?prn=$prisonerNumber")
      .headers(setAuthorisationUsingCurrentUser())
      .exchange()
      .expectStatus().isNoContent
  }

  @Test
  fun `should be forbidden without the SAR role`() {
    setCurrentUser(StubUser.READ_ONLY_USER)
    webTestClient.get()
      .uri("/subject-access-request?prn=$prisonerNumber")
      .headers(setAuthorisationUsingCurrentUser())
      .exchange()
      .expectStatus().isForbidden
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
