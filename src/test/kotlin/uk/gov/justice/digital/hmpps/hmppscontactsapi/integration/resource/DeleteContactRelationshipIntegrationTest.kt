package uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.SecureAPIIntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.ContactRelationship
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.CreateContactRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.restrictions.CreateContactRestrictionRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.restrictions.CreatePrisonerContactRestrictionRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.PersonReference
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.PrisonerContactInfo
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.Source
import uk.gov.justice.digital.hmpps.hmppscontactsapi.util.StubUser
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.time.LocalDate

class DeleteContactRelationshipIntegrationTest : SecureAPIIntegrationTestBase() {
  private val prisonerNumber = "A9876GH"
  private var savedContactId = 0L
  private var savedPrisonerContactId = 0L

  override val allowedRoles: Set<String> = setOf("ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__RW")

  @BeforeEach
  fun initialiseData() {
    setCurrentUser(StubUser.CREATING_USER)
    stubPrisonSearchWithResponse(prisonerNumber)
    val result = testAPIClient.createAContactWithARelationship(
      CreateContactRequest(
        lastName = "identity",
        firstName = "has",
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
}
