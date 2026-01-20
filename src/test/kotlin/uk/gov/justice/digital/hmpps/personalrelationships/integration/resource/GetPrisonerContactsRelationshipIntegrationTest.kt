package uk.gov.justice.digital.hmpps.personalrelationships.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.personalrelationships.client.manage.users.UserDetails
import uk.gov.justice.digital.hmpps.personalrelationships.integration.SecureAPIIntegrationTestBase
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.PrisonerContactRelationshipDetails
import uk.gov.justice.digital.hmpps.personalrelationships.util.StubUser

class GetPrisonerContactsRelationshipIntegrationTest : SecureAPIIntegrationTestBase() {
  companion object {
    private const val GET_PRISONER_CONTACT_RELATIONSHIP = "/prisoner-contact/1"
  }

  override val allowedRoles: Set<String> = setOf("ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__RW", "ROLE_CONTACTS__R")

  override fun baseRequestBuilder(): WebTestClient.RequestHeadersSpec<*> = webTestClient.get()
    .uri("/prisoner-contact/1")

  @BeforeEach
  fun setUp() {
    setCurrentUser(StubUser.READ_ONLY_USER)
  }

  @Test
  fun `should return not found if no prisoner contact relationship found`() {
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
  fun `should return OK`(role: String) {
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
      .expectBody(PrisonerContactRelationshipDetails::class.java)
      .returnResult().responseBody!!

    assertThat(actualPrisonerContactSummary).isEqualTo(expectedPrisonerContactRelationship)
  }
}
