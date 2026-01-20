package uk.gov.justice.digital.hmpps.personalrelationships.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.personalrelationships.client.prisonersearch.Prisoner
import uk.gov.justice.digital.hmpps.personalrelationships.helpers.prisoner
import uk.gov.justice.digital.hmpps.personalrelationships.integration.SecureAPIIntegrationTestBase
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.AddContactRelationshipRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.ContactRelationship
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.CreateContactRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.LinkedPrisonerDetails
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.PrisonerContactRelationshipDetails
import uk.gov.justice.digital.hmpps.personalrelationships.util.StubUser

class GetContactLinkedPrisonerIntegrationTest : SecureAPIIntegrationTestBase() {
  private var savedContactId = 0L

  override val allowedRoles: Set<String> = setOf("ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__RW", "ROLE_CONTACTS__R")

  val prisoner1 = prisoner(
    prisonerNumber = "A1234BC",
    prisonId = "MDI",
    prisonName = "Moorland (HMP & YOI)",
    firstName = "Joe",
    middleNames = "Middle",
    lastName = "Bloggs",
  )

  val prisoner2 = prisoner(
    prisonerNumber = "X9876YZ",
    prisonId = "BXI",
    prisonName = "Brixton (HMP)",
    firstName = "Barney",
    middleNames = null,
    lastName = "Rubble",
  )

  @BeforeEach
  fun initialiseData() {
    setCurrentUser(StubUser.READ_ONLY_USER)
    doWithTemporaryWritePermission {
      savedContactId = testAPIClient.createAContact(
        CreateContactRequest(
          lastName = "Contact",
          firstName = "Linked",
        ),
      ).id
    }
  }

  override fun baseRequestBuilder(): WebTestClient.RequestHeadersSpec<*> = webTestClient.get()
    .uri("/contact/1/linked-prisoners")

  @Test
  fun `should get linked prisoners`() {
    stubPrisonerSearch(prisoner1)
    stubPrisonerSearch(prisoner2)

    val prisoner1FriendRelationship = doWithTemporaryWritePermission { addRelationship(prisoner1, "FRI") }
    val prisoner1OtherRelationship = doWithTemporaryWritePermission { addRelationship(prisoner1, "OTHER") }
    val prisoner2FatherRelationship = doWithTemporaryWritePermission { addRelationship(prisoner2, "FA") }

    stubSearchPrisonersByPrisonerNumbers(
      setOf(prisoner1.prisonerNumber, prisoner2.prisonerNumber),
      listOf(prisoner1, prisoner2),
    )
    val linkedPrisoners = testAPIClient.getLinkedPrisoners(savedContactId, 0, 10)
    assertThat(linkedPrisoners.page.size).isEqualTo(10)
    assertThat(linkedPrisoners.page.totalElements).isEqualTo(3)
    assertThat(linkedPrisoners.page.totalPages).isEqualTo(1)
    assertThat(linkedPrisoners.page.number).isEqualTo(0)
    assertThat(linkedPrisoners.content).isEqualTo(
      listOf(
        LinkedPrisonerDetails(
          prisonerNumber = prisoner1.prisonerNumber,
          firstName = prisoner1.firstName,
          middleNames = prisoner1.middleNames,
          lastName = prisoner1.lastName,
          prisonId = prisoner1.prisonId,
          prisonName = prisoner1.prisonName,
          prisonerContactId = prisoner1OtherRelationship.prisonerContactId,
          relationshipTypeCode = "S",
          relationshipTypeDescription = "Social",
          relationshipToPrisonerCode = "OTHER",
          relationshipToPrisonerDescription = "Other social relationship",
          isRelationshipActive = true,
        ),
        LinkedPrisonerDetails(
          prisonerNumber = prisoner1.prisonerNumber,
          firstName = prisoner1.firstName,
          middleNames = prisoner1.middleNames,
          lastName = prisoner1.lastName,
          prisonId = prisoner1.prisonId,
          prisonName = prisoner1.prisonName,
          prisonerContactId = prisoner1FriendRelationship.prisonerContactId,
          relationshipTypeCode = "S",
          relationshipTypeDescription = "Social",
          relationshipToPrisonerCode = "FRI",
          relationshipToPrisonerDescription = "Friend",
          isRelationshipActive = true,
        ),
        LinkedPrisonerDetails(
          prisonerNumber = prisoner2.prisonerNumber,
          firstName = prisoner2.firstName,
          middleNames = prisoner2.middleNames,
          lastName = prisoner2.lastName,
          prisonId = prisoner2.prisonId,
          prisonName = prisoner2.prisonName,
          prisonerContactId = prisoner2FatherRelationship.prisonerContactId,
          relationshipTypeCode = "S",
          relationshipTypeDescription = "Social",
          relationshipToPrisonerCode = "FA",
          relationshipToPrisonerDescription = "Father",
          isRelationshipActive = true,
        ),
      ),
    )
  }

  @Test
  fun `should return linked prisoners even if one is missing in prisoner search`() {
    stubPrisonerSearch(prisoner1)
    stubPrisonerSearch(prisoner2)

    val prisoner1Relationship = doWithTemporaryWritePermission { addRelationship(prisoner1, "OTHER") }
    val prisoner2Relationship = doWithTemporaryWritePermission { addRelationship(prisoner2, "FA") }

    stubSearchPrisonersByPrisonerNumbers(setOf(prisoner1.prisonerNumber, prisoner2.prisonerNumber), listOf(prisoner1))

    val linkedPrisoners = testAPIClient.getLinkedPrisoners(savedContactId)
    assertThat(linkedPrisoners.page.totalElements).isEqualTo(2)
    assertThat(linkedPrisoners.content).isEqualTo(
      listOf(
        LinkedPrisonerDetails(
          prisonerNumber = prisoner2.prisonerNumber,
          firstName = null,
          middleNames = null,
          lastName = null,
          prisonId = null,
          prisonName = null,
          prisonerContactId = prisoner2Relationship.prisonerContactId,
          relationshipTypeCode = "S",
          relationshipTypeDescription = "Social",
          relationshipToPrisonerCode = "FA",
          relationshipToPrisonerDescription = "Father",
          isRelationshipActive = true,
        ),
        LinkedPrisonerDetails(
          prisonerNumber = prisoner1.prisonerNumber,
          firstName = prisoner1.firstName,
          middleNames = prisoner1.middleNames,
          lastName = prisoner1.lastName,
          prisonId = prisoner1.prisonId,
          prisonName = prisoner1.prisonName,
          prisonerContactId = prisoner1Relationship.prisonerContactId,
          relationshipTypeCode = "S",
          relationshipTypeDescription = "Social",
          relationshipToPrisonerCode = "OTHER",
          relationshipToPrisonerDescription = "Other social relationship",
          isRelationshipActive = true,
        ),
      ),
    )
  }

  private fun addRelationship(prisoner: Prisoner, relationshipCode: String): PrisonerContactRelationshipDetails = testAPIClient.addAContactRelationship(
    AddContactRelationshipRequest(
      contactId = savedContactId,
      ContactRelationship(
        prisonerNumber = prisoner.prisonerNumber,
        relationshipToPrisonerCode = relationshipCode,
        isNextOfKin = true,
        relationshipTypeCode = "S",
        isEmergencyContact = true,
        isApprovedVisitor = false,
      ),
    ),
  )
}
