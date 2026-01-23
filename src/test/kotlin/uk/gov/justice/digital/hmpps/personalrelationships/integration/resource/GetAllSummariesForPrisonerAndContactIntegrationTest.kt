package uk.gov.justice.digital.hmpps.personalrelationships.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.personalrelationships.integration.SecureAPIIntegrationTestBase
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.AddContactRelationshipRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.ContactRelationship
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.CreateContactRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.restrictions.CreateContactRestrictionRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.restrictions.CreatePrisonerContactRestrictionRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.RestrictionTypeDetails
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.RestrictionsSummary
import uk.gov.justice.digital.hmpps.personalrelationships.util.StubUser
import java.time.LocalDate

class GetAllSummariesForPrisonerAndContactIntegrationTest : SecureAPIIntegrationTestBase() {
  companion object {
    private const val GET_PRISONER_CONTACT = "/prisoner/A4385DZ/contact"
  }

  private val prisonerNumber = "A1234BC"
  private var savedContactId: Long = 0

  @BeforeEach
  fun setUp() {
    setCurrentUser(StubUser.CREATING_USER)
    stubPrisonSearchWithResponse(prisonerNumber)
    savedContactId = testAPIClient.createAContact(
      CreateContactRequest(
        lastName = "Last",
        firstName = "First",
      ),
    ).id
    setCurrentUser(StubUser.READ_ONLY_USER)
  }

  override val allowedRoles: Set<String> = setOf("ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__RW", "ROLE_CONTACTS__R")

  override fun baseRequestBuilder(): WebTestClient.RequestHeadersSpec<*> = webTestClient.get()
    .uri("/prisoner/A4385DZ/contact/$savedContactId")

  @Test
  fun `should return not found if no prisoner found`() {
    stubPrisonSearchWithNotFoundResponse("A4385DZ")

    webTestClient.get()
      .uri("/prisoner/A4385DZ/contact/$savedContactId")
      .headers(setAuthorisationUsingCurrentUser())
      .exchange()
      .expectStatus()
      .isNotFound
  }

  @Test
  fun `should return all relationships with restrictions summary`() {
    doWithTemporaryWritePermission {
      val sisterRelationship = testAPIClient.addAContactRelationship(
        AddContactRelationshipRequest(
          contactId = savedContactId,
          relationship = ContactRelationship(
            prisonerNumber = prisonerNumber,
            relationshipTypeCode = "S",
            relationshipToPrisonerCode = "SIS",
            isNextOfKin = true,
            isEmergencyContact = true,
            isApprovedVisitor = true,
          ),
        ),
      )
      val brotherRelationship = testAPIClient.addAContactRelationship(
        AddContactRelationshipRequest(
          contactId = savedContactId,
          relationship = ContactRelationship(
            prisonerNumber = prisonerNumber,
            relationshipTypeCode = "S",
            relationshipToPrisonerCode = "BRO",
            isNextOfKin = false,
            isEmergencyContact = false,
            isApprovedVisitor = false,
          ),
        ),
      )

      testAPIClient.createContactGlobalRestriction(
        savedContactId,
        CreateContactRestrictionRequest(
          "BAN",
          LocalDate.now().minusDays(1),
          null,
          "global",
        ),
      )

      testAPIClient.createPrisonerContactRestriction(
        sisterRelationship.prisonerContactId,
        CreatePrisonerContactRestrictionRequest(
          "CCTV",
          LocalDate.now().minusDays(1),
          null,
          "rel1",
        ),
      )

      testAPIClient.createPrisonerContactRestriction(
        brotherRelationship.prisonerContactId,
        CreatePrisonerContactRestrictionRequest(
          "NONCON",
          LocalDate.now().minusDays(1),
          null,
          "rel2",
        ),
      )
    }

    val summaries = testAPIClient.getAllSummariesForPrisonerAndContact(prisonerNumber, savedContactId)
    assertThat(summaries).hasSize(2)
    assertThat(summaries.find { it.relationshipToPrisonerCode == "SIS" }!!.restrictionSummary).isEqualTo(
      RestrictionsSummary(
        setOf(RestrictionTypeDetails("BAN", "Banned"), RestrictionTypeDetails("CCTV", "CCTV")),
        2,
        0,
      ),
    )
    assertThat(summaries.find { it.relationshipToPrisonerCode == "BRO" }!!.restrictionSummary).isEqualTo(
      RestrictionsSummary(
        setOf(RestrictionTypeDetails("BAN", "Banned"), RestrictionTypeDetails("NONCON", "Non-contact visit")),
        2,
        0,
      ),
    )
  }
}
