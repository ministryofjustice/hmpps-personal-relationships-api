package uk.gov.justice.digital.hmpps.personalrelationships.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.personalrelationships.integration.SecureAPIIntegrationTestBase
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.restrictions.CreatePrisonerRestrictionRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.restrictions.UpdatePrisonerRestrictionRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.PrisonerRestrictionDetails
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.PersonReference
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.PrisonerRestrictionInfo
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.Source
import uk.gov.justice.digital.hmpps.personalrelationships.util.StubUser
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.time.LocalDate

class ManagePrisonerRestrictionsIntegrationTest : SecureAPIIntegrationTestBase() {
  @Autowired
  private lateinit var jdbcTemplate: JdbcTemplate

  private val prisonerNumber = "A1234BC"

  override val allowedRoles: Set<String> = setOf("ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__RW")

  @BeforeEach
  fun setUp() {
    jdbcTemplate.update("delete from prisoner_restrictions where prisoner_number = ?", prisonerNumber)
    setCurrentUser(StubUser.CREATING_USER)
  }

  @AfterEach
  fun tearDown() {
    setCurrentUser(null)
  }

  override fun baseRequestBuilder(): WebTestClient.RequestHeadersSpec<*> = webTestClient.post()
    .uri("/prisoner-restrictions/$prisonerNumber")
    .accept(MediaType.APPLICATION_JSON)
    .contentType(MediaType.APPLICATION_JSON)
    .bodyValue(createRequest())

  @Test
  fun `should create a restriction and publish a DPS sourced event`() {
    val created = create(createRequest(type = "BAN", comment = "Banned"))

    assertThat(created.prisonerNumber).isEqualTo(prisonerNumber)
    assertThat(created.restrictionType).isEqualTo("BAN")
    assertThat(created.commentText).isEqualTo("Banned")
    assertThat(created.currentTerm).isTrue()
    assertThat(created.createdBy).isEqualTo("created")
    assertThat(created.authorisedUsername).isEqualTo("created")

    stubEvents.assertHasEvent(
      event = OutboundEvent.PRISONER_RESTRICTION_CREATED,
      additionalInfo = PrisonerRestrictionInfo(created.prisonerRestrictionId, Source.DPS, "created", null),
      personReference = PersonReference(nomsNumber = prisonerNumber),
    )
    assertThat(testAPIClient.getPrisonerRestrictions(prisonerNumber).content.map { it.prisonerRestrictionId }).contains(created.prisonerRestrictionId)
  }

  @Test
  fun `should update a restriction and publish a DPS sourced event`() {
    val created = create(createRequest())
    stubEvents.reset()
    setCurrentUser(StubUser.UPDATING_USER)

    val updated = webTestClient.put()
      .uri("/prisoner-restrictions/$prisonerNumber/${created.prisonerRestrictionId}")
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationUsingCurrentUser())
      .bodyValue(UpdatePrisonerRestrictionRequest("CCTV", LocalDate.of(2024, 2, 1), LocalDate.of(2024, 3, 1), "changed"))
      .exchange()
      .expectStatus().isOk
      .expectBody(PrisonerRestrictionDetails::class.java)
      .returnResult().responseBody!!

    assertThat(updated.restrictionType).isEqualTo("CCTV")
    assertThat(updated.expiryDate).isEqualTo(LocalDate.of(2024, 3, 1))
    assertThat(updated.updatedBy).isEqualTo("updated")
    stubEvents.assertHasEvent(
      event = OutboundEvent.PRISONER_RESTRICTION_UPDATED,
      additionalInfo = PrisonerRestrictionInfo(created.prisonerRestrictionId, Source.DPS, "updated", null),
      personReference = PersonReference(nomsNumber = prisonerNumber),
    )
  }

  @Test
  fun `should delete a restriction and publish a DPS sourced event`() {
    val created = create(createRequest())
    stubEvents.reset()
    setCurrentUser(StubUser.DELETING_USER)

    webTestClient.delete()
      .uri("/prisoner-restrictions/$prisonerNumber/${created.prisonerRestrictionId}")
      .headers(setAuthorisationUsingCurrentUser())
      .exchange()
      .expectStatus().isNoContent

    assertThat(testAPIClient.getPrisonerRestrictions(prisonerNumber).content.map { it.prisonerRestrictionId }).doesNotContain(created.prisonerRestrictionId)
    val row = jdbcTemplate.queryForMap("select deleted_at, updated_by, updated_time from prisoner_restrictions where prisoner_restriction_id = ?", created.prisonerRestrictionId)
    assertThat(row["deleted_at"]).isNotNull
    assertThat(row["updated_by"]).isEqualTo(StubUser.DELETING_USER.username)
    assertThat(row["updated_time"]).isEqualTo(row["deleted_at"])
    stubEvents.assertHasEvent(
      event = OutboundEvent.PRISONER_RESTRICTION_DELETED,
      additionalInfo = PrisonerRestrictionInfo(created.prisonerRestrictionId, Source.DPS, "deleted", null),
      personReference = PersonReference(nomsNumber = prisonerNumber),
    )
  }

  @Test
  fun `should not find or change a soft deleted restriction`() {
    val created = create(createRequest())
    webTestClient.delete()
      .uri("/prisoner-restrictions/$prisonerNumber/${created.prisonerRestrictionId}")
      .headers(setAuthorisationUsingCurrentUser())
      .exchange()
      .expectStatus().isNoContent

    webTestClient.delete()
      .uri("/prisoner-restrictions/$prisonerNumber/${created.prisonerRestrictionId}")
      .headers(setAuthorisationUsingCurrentUser())
      .exchange()
      .expectStatus().isNotFound
  }

  @Test
  fun `should return not found for another prisoner`() {
    val created = create(createRequest())
    stubEvents.reset()

    webTestClient.delete()
      .uri("/prisoner-restrictions/Z9999ZZ/${created.prisonerRestrictionId}")
      .headers(setAuthorisationUsingCurrentUser())
      .exchange()
      .expectStatus().isNotFound

    stubEvents.assertHasNoEvents(event = OutboundEvent.PRISONER_RESTRICTION_DELETED)
  }

  @Test
  fun `should reject unknown restriction types`() {
    val errors = webTestClient.post()
      .uri("/prisoner-restrictions/$prisonerNumber")
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationUsingCurrentUser())
      .bodyValue(createRequest(type = "NOPE"))
      .exchange()
      .expectStatus().isNotFound
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody!!

    assertThat(errors.userMessage).contains("NOPE")
    stubEvents.assertHasNoEvents(event = OutboundEvent.PRISONER_RESTRICTION_CREATED)
  }

  @Test
  fun `should reject an expiry date before the effective date`() {
    webTestClient.post()
      .uri("/prisoner-restrictions/$prisonerNumber")
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationUsingCurrentUser())
      .bodyValue(CreatePrisonerRestrictionRequest("BAN", LocalDate.of(2024, 2, 1), LocalDate.of(2024, 1, 1), null))
      .exchange()
      .expectStatus().isBadRequest

    stubEvents.assertHasNoEvents(event = OutboundEvent.PRISONER_RESTRICTION_CREATED)
  }

  @Test
  fun `should reject comments longer than 240 characters`() {
    webTestClient.post()
      .uri("/prisoner-restrictions/$prisonerNumber")
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationUsingCurrentUser())
      .bodyValue(createRequest(comment = "a".repeat(241)))
      .exchange()
      .expectStatus().isBadRequest
  }

  private fun createRequest(type: String = "BAN", comment: String? = null) = CreatePrisonerRestrictionRequest(type, LocalDate.of(2024, 1, 1), null, comment)

  private fun create(request: CreatePrisonerRestrictionRequest): PrisonerRestrictionDetails = webTestClient.post()
    .uri("/prisoner-restrictions/$prisonerNumber")
    .contentType(MediaType.APPLICATION_JSON)
    .headers(setAuthorisationUsingCurrentUser())
    .bodyValue(request)
    .exchange()
    .expectStatus().isCreated
    .expectBody(PrisonerRestrictionDetails::class.java)
    .returnResult().responseBody!!
}
