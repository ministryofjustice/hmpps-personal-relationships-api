package uk.gov.justice.digital.hmpps.personalrelationships.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.personalrelationships.config.User
import uk.gov.justice.digital.hmpps.personalrelationships.integration.SecureAPIIntegrationTestBase
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.AddContactRelationshipRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.ContactRelationship
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.CreateContactRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.restrictions.CreateContactRestrictionRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.restrictions.CreatePrisonerContactRestrictionRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.PrisonerContactRelationshipDetails
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

    assertCustomEvent(contactId = savedContactId, prisonerContactId = savedPrisonerContactId, prisonerNumber = prisonerNumber, groupCode = "S", relationshipCode = "FRI", relationshipStatus = true, source = Source.DPS, user = User("deleted", "BXI"))
    verify(telemetryClient, never()).trackEvent(
      eq("contact-next-of-kin-deleted"),
      any<Map<String, String>>(),
      anyOrNull(),
    )
  }

  @Test
  fun `should delete the relationship between prisoner and contact with next of kin as true and generate a next of kin deleted event`() {
    val result = testAPIClient.addAContactRelationship(
      AddContactRelationshipRequest(
        savedContactId,
        ContactRelationship(
          prisonerNumber = anotherPrisonerNumber,
          relationshipTypeCode = "S",
          relationshipToPrisonerCode = "FRI",
          isEmergencyContact = false,
          isNextOfKin = true,
          isApprovedVisitor = false,
        ),
      ),
    )

    testAPIClient.deletePrisonerContact(result.prisonerContactId)
    assertNextOfKinCustomDeletedEvent(result, Source.DPS, User("deleted", "BXI"))
  }

  @Test
  fun `should delete the relationship between prisoner and contact with approved visitor as true and generate an approved visitor deleted event`() {
    val result = testAPIClient.addAContactRelationship(
      AddContactRelationshipRequest(
        savedContactId,
        ContactRelationship(
          prisonerNumber = anotherPrisonerNumber,
          relationshipTypeCode = "S",
          relationshipToPrisonerCode = "FRI",
          isEmergencyContact = false,
          isNextOfKin = false,
          isApprovedVisitor = true,
        ),
      ),
    )

    testAPIClient.deletePrisonerContact(result.prisonerContactId)
    assertApprovedVisitorDeletedCustomEvent(result, Source.DPS, User("deleted", "BXI"))
  }

  @Test
  fun `should delete the relationship between prisoner and contact with emergency contact as true and generate an emergency contact deleted event`() {
    val result = testAPIClient.addAContactRelationship(
      AddContactRelationshipRequest(
        savedContactId,
        ContactRelationship(
          prisonerNumber = anotherPrisonerNumber,
          relationshipTypeCode = "S",
          relationshipToPrisonerCode = "FRI",
          isEmergencyContact = true,
          isNextOfKin = false,
          isApprovedVisitor = false,
        ),
      ),
    )

    testAPIClient.deletePrisonerContact(result.prisonerContactId)
    assertEmergencyContactDeletedCustomEvent(result, Source.DPS, User("deleted", "BXI"))
  }

  @Test
  fun `when next of kin, emergency contact and approved visitor are false, no next of kin, approved visitor or emergency contact events should be generated`() {
    val result = testAPIClient.addAContactRelationship(
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

    testAPIClient.deletePrisonerContact(result.prisonerContactId)
    verify(telemetryClient, never()).trackEvent(
      eq("contact-next-of-kin-deleted"),
      any<Map<String, String>>(),
      anyOrNull(),
    )
    verify(telemetryClient, never()).trackEvent(
      eq("contact-emergency-contact-deleted"),
      any<Map<String, String>>(),
      anyOrNull(),
    )
    verify(telemetryClient, never()).trackEvent(
      eq("contact-approved-visitor-deleted"),
      any<Map<String, String>>(),
      anyOrNull(),
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

  private fun assertCustomEvent(contactId: Long, prisonerContactId: Long, prisonerNumber: String, groupCode: String, relationshipCode: String, relationshipStatus: Boolean, source: Source, user: User) {
    verify(telemetryContactCustomEventService, times(1)).trackDeletePrisonerContactEvent(
      contactId = contactId,
      prisonerContactId = prisonerContactId,
      prisonerNumber = prisonerNumber,
      relationshipTypeCode = groupCode,
      relationshipToPrisonerCode = relationshipCode,
      activeRelationship = relationshipStatus,
      source = source,
      user = user,
    )

    val relationshipStatusString = if (relationshipStatus) "active" else "inactive"
    verify(telemetryClient, times(1)).trackEvent(
      "prisoner-contact-deleted",
      mapOf(
        "description" to "A prisoner contact has been deleted",
        "source" to source.name,
        "username" to user.username,
        "active_caseload_id" to user.activeCaseLoadId,
        "contact_id" to contactId.toString(),
        "prisoner_number" to prisonerNumber,
        "prisoner_contact_id" to prisonerContactId.toString(),
        "group_code" to groupCode,
        "relationship_code" to relationshipCode,
        "relationship_status" to relationshipStatusString,
      ),
      null,
    )
  }

  private fun assertNextOfKinCustomDeletedEvent(contactRelationship: PrisonerContactRelationshipDetails, source: Source, user: User) {
    verify(telemetryClient, times(1)).trackEvent(
      "contact-next-of-kin-deleted",
      mapOf(
        "description" to "A contact next of kin has been deleted",
        "source" to source.name,
        "username" to user.username,
        "contact_id" to contactRelationship.contactId.toString(),
        "active_caseload_id" to user.activeCaseLoadId,
        "prisoner_contact_id" to contactRelationship.prisonerContactId.toString(),
      ),
      null,
    )
  }

  private fun assertApprovedVisitorDeletedCustomEvent(contactRelationship: PrisonerContactRelationshipDetails, source: Source, user: User) {
    verify(telemetryClient, times(1)).trackEvent(
      "contact-approved-visitor-deleted",
      mapOf(
        "description" to "A contact approved visitor has been deleted",
        "source" to source.name,
        "username" to user.username,
        "contact_id" to contactRelationship.contactId.toString(),
        "active_caseload_id" to user.activeCaseLoadId,
        "prisoner_contact_id" to contactRelationship.prisonerContactId.toString(),
      ),
      null,
    )
  }

  private fun assertEmergencyContactDeletedCustomEvent(contactRelationship: PrisonerContactRelationshipDetails, source: Source, user: User) {
    verify(telemetryClient, times(1)).trackEvent(
      "contact-emergency-contact-deleted",
      mapOf(
        "description" to "A contact emergency contact has been deleted",
        "source" to source.name,
        "username" to user.username,
        "contact_id" to contactRelationship.contactId.toString(),
        "active_caseload_id" to user.activeCaseLoadId,
        "prisoner_contact_id" to contactRelationship.prisonerContactId.toString(),
      ),
      null,
    )
  }
}
