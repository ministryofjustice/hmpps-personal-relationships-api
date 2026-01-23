package uk.gov.justice.digital.hmpps.personalrelationships.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.personalrelationships.integration.SecureAPIIntegrationTestBase
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.AddContactRelationshipRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.ContactRelationship
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.CreateContactRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.restrictions.CreateContactRestrictionRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.restrictions.CreatePrisonerContactRestrictionRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.RelationshipDeletePlan
import uk.gov.justice.digital.hmpps.personalrelationships.repository.ContactRepository
import uk.gov.justice.digital.hmpps.personalrelationships.repository.DeletedPrisonerContactRepository
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.ContactInfo
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.PersonReference
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.PrisonerContactInfo
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.Source
import uk.gov.justice.digital.hmpps.personalrelationships.util.StubUser
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.time.LocalDate

class DeleteContactRelationshipIntegrationTest : SecureAPIIntegrationTestBase() {
  private val prisonerNumber = "A9876GH"
  private val anotherPrisonerNumber = "B1234GH"
  private var savedContactId = 0L
  private var savedPrisonerContactId = 0L

  override val allowedRoles: Set<String> = setOf("ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__RW")

  @Autowired
  private lateinit var deletedPrisonerContactRepository: DeletedPrisonerContactRepository

  @Autowired
  private lateinit var contactRepository: ContactRepository

  @BeforeEach
  fun initialiseData() {
    setCurrentUser(StubUser.CREATING_USER)
    stubPrisonSearchWithResponse(prisonerNumber)
    stubPrisonSearchWithResponse(anotherPrisonerNumber)
    val result = testAPIClient.createAContactWithARelationship(
      CreateContactRequest(
        lastName = "identity",
        firstName = "has",
        dateOfBirth = LocalDate.of(2000, 1, 1),
        relationship = ContactRelationship(
          prisonerNumber = prisonerNumber,
          relationshipTypeCode = "S",
          relationshipToPrisonerCode = "FRI",
          isEmergencyContact = false,
          isNextOfKin = false,
          isApprovedVisitor = false,
        ),
      ),
    )
    savedContactId = result.createdContact.id
    savedPrisonerContactId = result.createdRelationship!!.prisonerContactId
    setCurrentUser(StubUser.DELETING_USER)
  }

  override fun baseRequestBuilder(): WebTestClient.RequestHeadersSpec<*> = webTestClient.delete()
    .uri("/prisoner-contact/$savedPrisonerContactId")
    .accept(MediaType.APPLICATION_JSON)

  @Test
  fun `should fail if the relationship is not found`() {
    val errors = webTestClient.delete()
      .uri("/prisoner-contact/-999")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationUsingCurrentUser())
      .exchange()
      .expectStatus()
      .isNotFound
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody!!

    assertThat(errors.userMessage).isEqualTo("Entity not found : Prisoner contact with prisoner contact ID -999 not found")

    stubEvents.assertHasNoEvents(event = OutboundEvent.CONTACT_IDENTITY_DELETED)
  }

  @Test
  fun `should delete the relationship between prisoner and contact`() {
    testAPIClient.deletePrisonerContact(savedPrisonerContactId)
    val contacts = testAPIClient.getPrisonerContacts(prisonerNumber).content
    assertThat(contacts.find { it.prisonerContactId == savedPrisonerContactId }).isNull()

    stubEvents.assertHasEvent(
      event = OutboundEvent.PRISONER_CONTACT_DELETED,
      additionalInfo = PrisonerContactInfo(savedPrisonerContactId, Source.DPS, "deleted", "BXI"),
      personReference = PersonReference(dpsContactId = savedContactId, nomsNumber = prisonerNumber),
    )
  }

  @Test
  fun `should delete the contact's date of birth if they only have internal relationship left`() {
    createInternalOfficialRelationship()

    val plan = testAPIClient.planDeletePrisonerContact(savedPrisonerContactId)
    assertThat(plan).isEqualTo(
      RelationshipDeletePlan(
        willAlsoDeleteContactDob = true,
        hasRestrictions = false,
      ),
    )

    testAPIClient.deletePrisonerContact(savedPrisonerContactId)
    val contact = contactRepository.findById(savedContactId).get()
    assertThat(contact.dateOfBirth).isNull()

    stubEvents.assertHasEvent(
      event = OutboundEvent.PRISONER_CONTACT_DELETED,
      additionalInfo = PrisonerContactInfo(savedPrisonerContactId, Source.DPS, "deleted", "BXI"),
      personReference = PersonReference(dpsContactId = savedContactId, nomsNumber = prisonerNumber),
    )
    stubEvents.assertHasEvent(
      event = OutboundEvent.CONTACT_UPDATED,
      additionalInfo = ContactInfo(contactId = savedContactId, Source.DPS, "deleted", "BXI"),
      personReference = PersonReference(dpsContactId = savedContactId),
    )
  }

  @Test
  fun `should retain the contact's date of birth if they have a non-internal relationship left`() {
    testAPIClient.addAContactRelationship(
      AddContactRelationshipRequest(
        savedContactId,
        ContactRelationship(
          prisonerNumber = anotherPrisonerNumber,
          relationshipTypeCode = "S",
          relationshipToPrisonerCode = "FRI",
          isEmergencyContact = false,
          isNextOfKin = false,
          isApprovedVisitor = false,
        ),
      ),
    )

    val plan = testAPIClient.planDeletePrisonerContact(savedPrisonerContactId)
    assertThat(plan).isEqualTo(
      RelationshipDeletePlan(
        willAlsoDeleteContactDob = false,
        hasRestrictions = false,
      ),
    )

    testAPIClient.deletePrisonerContact(savedPrisonerContactId)
    val contact = contactRepository.findById(savedContactId).get()
    assertThat(contact.dateOfBirth).isEqualTo(LocalDate.of(2000, 1, 1))
  }

  @Test
  fun `should delete the relationship between prisoner and contact even if the contact has global restrictions`() {
    testAPIClient.createContactGlobalRestriction(
      savedContactId,
      CreateContactRestrictionRequest(
        restrictionType = "BAN",
        startDate = LocalDate.now(),
        expiryDate = null,
        comments = null,
      ),
    )

    createInternalOfficialRelationship()
    val plan = testAPIClient.planDeletePrisonerContact(savedPrisonerContactId)
    assertThat(plan).isEqualTo(
      RelationshipDeletePlan(
        willAlsoDeleteContactDob = true,
        hasRestrictions = false,
      ),
    )

    testAPIClient.deletePrisonerContact(savedPrisonerContactId)
    val contacts = testAPIClient.getPrisonerContacts(prisonerNumber).content
    assertThat(contacts.find { it.prisonerContactId == savedPrisonerContactId }).isNull()
    stubEvents.assertHasEvent(
      event = OutboundEvent.PRISONER_CONTACT_DELETED,
      additionalInfo = PrisonerContactInfo(savedPrisonerContactId, Source.DPS, "deleted", "BXI"),
      personReference = PersonReference(dpsContactId = savedContactId, nomsNumber = prisonerNumber),
    )
    val deleted = deletedPrisonerContactRepository.findByPrisonerContactId(savedPrisonerContactId)
    assertThat(deleted).isNotNull
  }

  @Test
  fun `should not delete the relationship between prisoner and contact if there are relationship restrictions`() {
    testAPIClient.createPrisonerContactRestriction(
      savedPrisonerContactId,
      CreatePrisonerContactRestrictionRequest(
        restrictionType = "BAN",
        startDate = LocalDate.now(),
        expiryDate = null,
        comments = null,
      ),
    )
    createInternalOfficialRelationship()

    val plan = testAPIClient.planDeletePrisonerContact(savedPrisonerContactId)
    assertThat(plan).isEqualTo(
      RelationshipDeletePlan(
        willAlsoDeleteContactDob = true,
        hasRestrictions = true,
      ),
    )

    webTestClient.delete()
      .uri("/prisoner-contact/$savedPrisonerContactId")
      .headers(testAPIClient.setAuthorisationUsingCurrentUser())
      .exchange()
      .expectStatus()
      .isEqualTo(HttpStatus.CONFLICT)

    val contacts = testAPIClient.getPrisonerContacts(prisonerNumber).content
    assertThat(contacts.find { it.prisonerContactId == savedPrisonerContactId }).isNotNull
    stubEvents.assertHasNoEvents(event = OutboundEvent.PRISONER_CONTACT_DELETED)
  }

  private fun createInternalOfficialRelationship() {
    testAPIClient.addAContactRelationship(
      AddContactRelationshipRequest(
        savedContactId,
        ContactRelationship(
          prisonerNumber = anotherPrisonerNumber,
          relationshipTypeCode = "O",
          relationshipToPrisonerCode = "POM",
          isEmergencyContact = false,
          isNextOfKin = false,
          isApprovedVisitor = false,
        ),
      ),
    )
  }
}
