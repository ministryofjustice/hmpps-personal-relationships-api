package uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.openapitools.jackson.nullable.JsonNullable
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.SecureAPIIntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.AddContactRelationshipRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.ContactRelationship
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.CreateContactRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.PatchRelationshipRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.PrisonerContactRelationshipCount
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.PrisonerContactRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.util.StubUser
import kotlin.jvm.optionals.getOrNull

class GetPrisonerContactRelationshipCountIntegrationTest : SecureAPIIntegrationTestBase() {

  @BeforeEach
  fun setUp() {
    setCurrentUser(StubUser.READ_ONLY_USER)
  }

  override val allowedRoles: Set<String> = setOf("ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__RW", "ROLE_CONTACTS__R")

  override fun baseRequestBuilder(): WebTestClient.RequestHeadersSpec<*> = webTestClient.get()
    .uri("/prisoner/P001/contact/count")

  @Autowired
  private lateinit var prisonerContactRepository: PrisonerContactRepository

  @Test
  fun `should return zeroes if no prisoner found as this will be called from prisoner profile so to make it fast we don't verify the prisoner`() {
    stubPrisonSearchWithNotFoundResponse("A0000AA")
    assertThat(testAPIClient.getPrisonerContactRelationshipCount("A0000AA")).isEqualTo(
      PrisonerContactRelationshipCount(
        0,
        0,
      ),
    )
  }

  @Test
  fun `should count contact relationships for the current term`() {
    val prisonerNumber = "X0123XX"
    stubPrisonSearchWithResponse(prisonerNumber)

    val relationship = ContactRelationship(
      prisonerNumber = prisonerNumber,
      relationshipTypeCode = "S",
      relationshipToPrisonerCode = "FRI",
      isNextOfKin = false,
      isEmergencyContact = false,
      isApprovedVisitor = false,
    )
    doWithTemporaryWritePermission {
      // Contact one has one active social and one active official relationship plus an inactive social relationship and
      // another from a previous term
      val contactOne = testAPIClient.createAContact(
        CreateContactRequest(
          lastName = "Contact",
          firstName = "One",
        ),
      )
      // Make from previous term
      val relationshipFromPreviousTerm = testAPIClient.addAContactRelationship(
        AddContactRelationshipRequest(
          contactOne.id,
          relationship.copy(relationshipToPrisonerCode = "FRI"),
        ),
      )
      val entity = prisonerContactRepository.findById(relationshipFromPreviousTerm.prisonerContactId).getOrNull()!!
      prisonerContactRepository.saveAndFlush(entity.copy(currentTerm = false))

      // Inactive
      val relationshipToMakeInactive = testAPIClient.addAContactRelationship(
        AddContactRelationshipRequest(
          contactOne.id,
          relationship.copy(relationshipToPrisonerCode = "GIF"),
        ),
      )
      testAPIClient.updateRelationship(
        relationshipToMakeInactive.prisonerContactId,
        PatchRelationshipRequest(
          isRelationshipActive = JsonNullable.of(false),
        ),
      )

      // Active social
      testAPIClient.addAContactRelationship(
        AddContactRelationshipRequest(
          contactOne.id,
          relationship.copy(relationshipToPrisonerCode = "WIFE"),
        ),
      )

      // Active official
      testAPIClient.addAContactRelationship(
        AddContactRelationshipRequest(
          contactOne.id,
          relationship.copy(relationshipTypeCode = "O", relationshipToPrisonerCode = "DR"),
        ),
      )

      // Another contact active social
      testAPIClient.createAContact(
        CreateContactRequest(
          lastName = "Contact",
          firstName = "Two",
          relationship = ContactRelationship(
            prisonerNumber = prisonerNumber,
            relationshipTypeCode = "S",
            relationshipToPrisonerCode = "MOT",
            isNextOfKin = false,
            isEmergencyContact = false,
            isApprovedVisitor = false,
          ),
        ),
      )

      // Another contact with active official
      testAPIClient.createAContact(
        CreateContactRequest(
          lastName = "Contact",
          firstName = "Three",
          relationship = ContactRelationship(
            prisonerNumber = prisonerNumber,
            relationshipTypeCode = "O",
            relationshipToPrisonerCode = "POM",
            isNextOfKin = false,
            isEmergencyContact = false,
            isApprovedVisitor = false,
          ),
        ),
      )
    }

    assertThat(testAPIClient.getPrisonerContactRelationshipCount(prisonerNumber)).isEqualTo(
      PrisonerContactRelationshipCount(
        2,
        2,
      ),
    )
  }
}
