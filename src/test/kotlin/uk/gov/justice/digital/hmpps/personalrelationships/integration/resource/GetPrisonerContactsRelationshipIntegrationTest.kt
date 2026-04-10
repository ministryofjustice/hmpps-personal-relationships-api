package uk.gov.justice.digital.hmpps.personalrelationships.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.groups.Tuple
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.personalrelationships.client.manage.users.UserDetails
import uk.gov.justice.digital.hmpps.personalrelationships.integration.SecureAPIIntegrationTestBase
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.PrisonerAndContactId
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.PrisonerContactRelationshipsRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.PrisonerContactRelationshipDetails
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.PrisonerContactRelationshipsResponse
import uk.gov.justice.digital.hmpps.personalrelationships.util.StubUser

class GetPrisonerContactsRelationshipIntegrationTest : SecureAPIIntegrationTestBase() {
  companion object {
    private const val GET_PRISONER_CONTACT_RELATIONSHIP = "/prisoner-contact/1"
    private const val POST_SUMMARY_RELATIONSHIPS = "/prisoner-contact/relationships/summary"
  }

  override val allowedRoles: Set<String> = setOf("ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__RW", "ROLE_CONTACTS__R")

  override fun baseRequestBuilder(): WebTestClient.RequestHeadersSpec<*> = webTestClient.get()
    .uri("/prisoner-contact/1")

  @BeforeEach
  fun setUp() {
    setCurrentUser(StubUser.READ_ONLY_USER)
  }

  @Test
  fun `getById - should return not found if no prisoner contact relationship found`() {
    stubPrisonSearchWithNotFoundResponse("A4385DZ")

    webTestClient.get()
      .uri("/prisoner-contact/15453")
      .headers(setAuthorisationUsingCurrentUser())
      .exchange()
      .expectStatus()
      .isNotFound
  }

  @ParameterizedTest
  @ValueSource(strings = ["ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__R", "ROLE_CONTACTS__RW"])
  fun `getById - should return OK`(role: String) {
    stubGetUserByUsername(UserDetails("A_USER", "Created User", "BXI"))
    setCurrentUser(StubUser.READ_ONLY_USER.copy(roles = listOf(role)))
    val expectedPrisonerContactRelationship = PrisonerContactRelationshipDetails(
      prisonerContactId = 1,
      contactId = 1,
      prisonerNumber = "A1234BB",
      relationshipTypeCode = "S",
      relationshipTypeDescription = "Social",
      relationshipToPrisonerCode = "FA",
      relationshipToPrisonerDescription = "Father",
      isNextOfKin = false,
      isEmergencyContact = false,
      isRelationshipActive = true,
      isApprovedVisitor = false,
      comments = "Comment",
      approvedBy = "Created User",
    )

    val actualPrisonerContactSummary = webTestClient.get()
      .uri(GET_PRISONER_CONTACT_RELATIONSHIP)
      .headers(setAuthorisationUsingCurrentUser())
      .exchange()
      .expectStatus()
      .isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody<PrisonerContactRelationshipDetails>()
      .returnResult().responseBody!!

    assertThat(actualPrisonerContactSummary).isEqualTo(expectedPrisonerContactRelationship)
  }

  @Test
  fun `getSummaryRelationships - should return summary relationships for prisoner and contact pairs`() {
    val request = PrisonerContactRelationshipsRequest(
      identifiers = listOf(
        PrisonerAndContactId(prisonerNumber = "G4793VF", contactId = 1L),
        PrisonerAndContactId(prisonerNumber = "G4793VF", contactId = 2L),
        PrisonerAndContactId(prisonerNumber = "G4793VF", contactId = 3L),
      ),
    )

    val response = webTestClient.post()
      .uri(POST_SUMMARY_RELATIONSHIPS)
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationUsingCurrentUser())
      .bodyValue(request)
      .exchange()
      .expectStatus()
      .isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody<PrisonerContactRelationshipsResponse>()
      .returnResult().responseBody!!

    assertThat(response.responses).hasSize(3)

    response.responses.forEach { resp ->
      with(resp) {
        assertThat(prisonerNumber).isEqualTo(request.identifiers[0].prisonerNumber)
        assertThat(contactId).isBetween(1L, 3L)
        assertThat(relationships).hasSize(1)
      }
    }

    val allRelationships = response.responses.flatMap { it.relationships }
    assertThat(allRelationships).hasSize(3)
    assertThat(allRelationships).extracting(
      "prisonerContactId",
      "relationshipTypeCode",
      "relationshipToPrisonerCode",
      "isApprovedVisitor",
      "isRelationshipActive",
      "currentTerm",
    ).containsAll(
      listOf(
        Tuple(8L, "S", "FA", false, true, true),
        Tuple(9L, "S", "MOT", false, true, true),
        Tuple(10L, "O", "POL", false, true, true),
      ),
    )
  }
}
