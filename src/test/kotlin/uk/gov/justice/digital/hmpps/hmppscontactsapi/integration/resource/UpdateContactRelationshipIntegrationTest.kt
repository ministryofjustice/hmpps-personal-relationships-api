package uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.resource

import org.apache.commons.lang3.RandomStringUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import org.openapitools.jackson.nullable.JsonNullable
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.SecureAPIIntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.ContactRelationship
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.CreateContactRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.UpdateRelationshipRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.PrisonerContactSummary
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.PersonReference
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.PrisonerContactInfo
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.Source
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

class UpdateContactRelationshipIntegrationTest : SecureAPIIntegrationTestBase() {

  override val allowedRoles: Set<String> = setOf("ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__RW")

  override fun baseRequestBuilder(): WebTestClient.RequestHeadersSpec<*> = webTestClient.patch()
    .uri("/prisoner-contact/99")
    .accept(MediaType.APPLICATION_JSON)
    .contentType(MediaType.APPLICATION_JSON)
    .bodyValue(UpdateRelationshipRequest(updatedBy = "Admin"))

  @ParameterizedTest
  @CsvSource(
    value = [
      "Unsupported relationship type null.;{\"relationshipType\": null,  \"updatedBy\": \"Admin\"}",
      "Unsupported relationship to prisoner null.;{\"relationshipToPrisoner\": null,  \"updatedBy\": \"Admin\"}",
      "Unsupported approved visitor value null.;{\"isApprovedVisitor\": null,  \"updatedBy\": \"Admin\"}",
      "Unsupported emergency contact null.;{\"isEmergencyContact\": null,  \"updatedBy\": \"Admin\"}",
      "Unsupported next of kin null.;{\"isNextOfKin\": null,  \"updatedBy\": \"Admin\"}",
      "Unsupported relationship status null.;{\"isRelationshipActive\": null,  \"updatedBy\": \"Admin\"}",
    ],
    delimiter = ';',
  )
  fun `should return bad request when optional fields set with a null value `(expectedMessage: String, relationShipJson: String) {
    val prisonerNumber = getRandomPrisonerCode()
    stubPrisonSearchWithResponse(prisonerNumber)
    val prisonerContact = cretePrisonerContact(prisonerNumber)

    val prisonerContactId = prisonerContact.prisonerContactId

    val errors = webTestClient.patch()
      .uri("/prisoner-contact/$prisonerContactId")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CONTACTS_ADMIN")))
      .bodyValue(relationShipJson)
      .exchange()
      .expectStatus()
      .isBadRequest
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody!!

    assertThat(errors.userMessage).isEqualTo("Validation failure: $expectedMessage")
    stubEvents.assertHasNoEvents(event = OutboundEvent.PRISONER_CONTACT_UPDATED)
  }

  @Test
  fun `should update the contact relationship with relationship code fields`() {
    val prisonerNumber = getRandomPrisonerCode()
    stubPrisonSearchWithResponse(prisonerNumber)
    val prisonerContact = cretePrisonerContact(prisonerNumber)

    val updateRequest = UpdateRelationshipRequest(
      relationshipToPrisoner = JsonNullable.of("SIS"),
      updatedBy = "Admin",
    )

    val prisonerContactId = prisonerContact.prisonerContactId

    testAPIClient.updateRelationship(prisonerContactId, updateRequest)

    val updatedPrisonerContacts = testAPIClient.getPrisonerContacts(prisonerNumber).content
    assertThat(updatedPrisonerContacts).hasSize(1)
    assertThat(updatedPrisonerContacts[0].relationshipToPrisoner).isEqualTo("SIS")
    stubEvents.assertHasEvent(
      event = OutboundEvent.PRISONER_CONTACT_UPDATED,
      additionalInfo = PrisonerContactInfo(prisonerContactId, Source.DPS),
      personReference = PersonReference(prisonerNumber, prisonerContact.contactId),
    )
  }

  @Test
  fun `should update the contact relationship from social to official`() {
    val prisonerNumber = getRandomPrisonerCode()
    stubPrisonSearchWithResponse(prisonerNumber)
    val prisonerContact = cretePrisonerContact(prisonerNumber)

    val updateRequest = UpdateRelationshipRequest(
      relationshipType = JsonNullable.of("O"),
      relationshipToPrisoner = JsonNullable.of("DR"),
      updatedBy = "Admin",
    )

    val prisonerContactId = prisonerContact.prisonerContactId

    testAPIClient.updateRelationship(prisonerContactId, updateRequest)

    val updatedPrisonerContacts = testAPIClient.getPrisonerContacts(prisonerNumber).content
    assertThat(updatedPrisonerContacts).hasSize(1)
    assertThat(updatedPrisonerContacts[0].relationshipType).isEqualTo("O")
    assertThat(updatedPrisonerContacts[0].relationshipToPrisoner).isEqualTo("DR")
    stubEvents.assertHasEvent(
      event = OutboundEvent.PRISONER_CONTACT_UPDATED,
      additionalInfo = PrisonerContactInfo(prisonerContactId, Source.DPS),
      personReference = PersonReference(prisonerNumber, prisonerContact.contactId),
    )
  }

  @Test
  fun `should update the contact relationship with next of kin fields`() {
    val prisonerNumber = getRandomPrisonerCode()
    stubPrisonSearchWithResponse(prisonerNumber)
    val prisonerContact = cretePrisonerContact(prisonerNumber)

    val updateRequest = UpdateRelationshipRequest(
      isNextOfKin = JsonNullable.of(true),
      updatedBy = "Admin",
    )

    val prisonerContactId = prisonerContact.prisonerContactId

    testAPIClient.updateRelationship(prisonerContactId, updateRequest)

    val updatedPrisonerContacts = testAPIClient.getPrisonerContacts(prisonerNumber).content
    assertThat(updatedPrisonerContacts).hasSize(1)
    assertThat(updatedPrisonerContacts[0].nextOfKin).isTrue
    stubEvents.assertHasEvent(
      event = OutboundEvent.PRISONER_CONTACT_UPDATED,
      additionalInfo = PrisonerContactInfo(prisonerContactId, Source.DPS),
      personReference = PersonReference(prisonerNumber, prisonerContact.contactId),
    )
  }

  @Test
  fun `should update the contact relationship with approved visitor`() {
    val prisonerNumber = getRandomPrisonerCode()
    stubPrisonSearchWithResponse(prisonerNumber)
    val prisonerContact = cretePrisonerContact(prisonerNumber)

    val updateRequest = UpdateRelationshipRequest(
      isApprovedVisitor = JsonNullable.of(true),
      updatedBy = "Admin",
    )

    val prisonerContactId = prisonerContact.prisonerContactId

    testAPIClient.updateRelationship(prisonerContactId, updateRequest)

    val updatedPrisonerContacts = testAPIClient.getPrisonerContacts(prisonerNumber).content
    assertThat(updatedPrisonerContacts).hasSize(1)
    assertThat(updatedPrisonerContacts[0].approvedVisitor).isTrue
    stubEvents.assertHasEvent(
      event = OutboundEvent.PRISONER_CONTACT_UPDATED,
      additionalInfo = PrisonerContactInfo(prisonerContactId, Source.DPS),
      personReference = PersonReference(prisonerNumber, prisonerContact.contactId),
    )
  }

  @Test
  fun `should update the contact relationship with emergency contact fields`() {
    val prisonerNumber = getRandomPrisonerCode()
    stubPrisonSearchWithResponse(prisonerNumber)
    val prisonerContact = cretePrisonerContact(prisonerNumber)

    val updateRequest = UpdateRelationshipRequest(
      isEmergencyContact = JsonNullable.of(true),
      updatedBy = "Admin",
    )

    val prisonerContactId = prisonerContact.prisonerContactId

    testAPIClient.updateRelationship(prisonerContactId, updateRequest)

    val updatedPrisonerContacts = testAPIClient.getPrisonerContacts(prisonerNumber).content
    assertThat(updatedPrisonerContacts).hasSize(1)
    assertThat(updatedPrisonerContacts[0].emergencyContact).isTrue
    stubEvents.assertHasEvent(
      event = OutboundEvent.PRISONER_CONTACT_UPDATED,
      additionalInfo = PrisonerContactInfo(prisonerContactId, Source.DPS),
      personReference = PersonReference(prisonerNumber, prisonerContact.contactId),
    )
  }

  @Test
  fun `should update the contact relationship with relationship active fields`() {
    val prisonerNumber = getRandomPrisonerCode()
    stubPrisonSearchWithResponse(prisonerNumber)
    val prisonerContact = cretePrisonerContact(prisonerNumber)

    val updateRequest = UpdateRelationshipRequest(
      isRelationshipActive = JsonNullable.of(true),
      updatedBy = "Admin",
    )

    val prisonerContactId = prisonerContact.prisonerContactId

    testAPIClient.updateRelationship(prisonerContactId, updateRequest)

    val updatedPrisonerContacts = testAPIClient.getPrisonerContacts(prisonerNumber).content
    assertThat(updatedPrisonerContacts).hasSize(1)
    assertThat(updatedPrisonerContacts[0].isRelationshipActive).isTrue
    stubEvents.assertHasEvent(
      event = OutboundEvent.PRISONER_CONTACT_UPDATED,
      additionalInfo = PrisonerContactInfo(prisonerContactId, Source.DPS),
      personReference = PersonReference(prisonerNumber, prisonerContact.contactId),
    )
  }

  @Test
  fun `should update the contact relationship with comment fields`() {
    val prisonerNumber = getRandomPrisonerCode()
    stubPrisonSearchWithResponse(prisonerNumber)
    val prisonerContact = cretePrisonerContact(prisonerNumber)

    val updateRequest = UpdateRelationshipRequest(
      comments = JsonNullable.of("New comment"),
      updatedBy = "Admin",
    )

    val prisonerContactId = prisonerContact.prisonerContactId

    testAPIClient.updateRelationship(prisonerContactId, updateRequest)

    val updatedPrisonerContacts = testAPIClient.getPrisonerContacts(prisonerNumber).content
    assertThat(updatedPrisonerContacts).hasSize(1)
    assertThat(updatedPrisonerContacts[0].comments).isEqualTo("New comment")
    stubEvents.assertHasEvent(
      event = OutboundEvent.PRISONER_CONTACT_UPDATED,
      additionalInfo = PrisonerContactInfo(prisonerContactId, Source.DPS),
      personReference = PersonReference(prisonerNumber, prisonerContact.contactId),
    )
  }

  @Test
  fun `should update the contact relationship with minimal fields`() {
    val prisonerNumber = getRandomPrisonerCode()
    stubPrisonSearchWithResponse(prisonerNumber)
    val prisonerContact = cretePrisonerContact(prisonerNumber)

    val updateRequest = UpdateRelationshipRequest(
      updatedBy = "Admin",
    )

    val prisonerContactId = prisonerContact.prisonerContactId

    testAPIClient.updateRelationship(prisonerContactId, updateRequest)

    val updatedPrisonerContacts = testAPIClient.getPrisonerContacts(prisonerNumber).content
    assertThat(updatedPrisonerContacts).hasSize(1)
    stubEvents.assertHasEvent(
      event = OutboundEvent.PRISONER_CONTACT_UPDATED,
      additionalInfo = PrisonerContactInfo(prisonerContactId, Source.DPS),
      personReference = PersonReference(prisonerNumber, prisonerContact.contactId),
    )
  }

  @ParameterizedTest
  @ValueSource(strings = ["ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__RW"])
  fun `should update the contact relationship with all fields`(role: String) {
    val prisonerNumber = getRandomPrisonerCode()
    stubPrisonSearchWithResponse(prisonerNumber)
    val prisonerContact = cretePrisonerContact(prisonerNumber)

    val updateRequest = UpdateRelationshipRequest(
      relationshipToPrisoner = JsonNullable.of("FRI"),
      isNextOfKin = JsonNullable.of(true),
      isApprovedVisitor = JsonNullable.of(true),
      isEmergencyContact = JsonNullable.of(true),
      isRelationshipActive = JsonNullable.of(true),
      comments = JsonNullable.of("comments added"),
      updatedBy = "Admin",
    )

    val prisonerContactId = prisonerContact.prisonerContactId

    testAPIClient.updateRelationship(prisonerContactId, updateRequest, role)

    val updatedPrisonerContacts = testAPIClient.getPrisonerContacts(prisonerNumber).content

    assertThat(updatedPrisonerContacts).hasSize(1)
    assertUpdatedPrisonerContactEquals(updatedPrisonerContacts[0], updateRequest)
    stubEvents.assertHasEvent(
      event = OutboundEvent.PRISONER_CONTACT_UPDATED,
      additionalInfo = PrisonerContactInfo(prisonerContactId, Source.DPS),
      personReference = PersonReference(prisonerNumber, prisonerContact.contactId),
    )
  }

  private fun assertUpdatedPrisonerContactEquals(prisonerContact: PrisonerContactSummary, relationship: UpdateRelationshipRequest) {
    with(prisonerContact) {
      assertThat(relationshipToPrisoner).isEqualTo(relationship.relationshipToPrisoner.get())
      assertThat(nextOfKin).isEqualTo(relationship.isNextOfKin.get())
      assertThat(approvedVisitor).isEqualTo(relationship.isApprovedVisitor.get())
      assertThat(emergencyContact).isEqualTo(relationship.isEmergencyContact.get())
      assertThat(comments).isEqualTo(relationship.comments.get())
    }
  }

  private fun getRandomPrisonerCode(): String {
    val letters = ('A'..'Z')
    val numbers = ('0'..'9')

    val firstLetter = letters.random()
    val numberPart = (1..4).map { numbers.random() }.joinToString("")
    val lastTwoLetters = (1..2).map { letters.random() }.joinToString("")

    return "$firstLetter$numberPart$lastTwoLetters"
  }

  private fun cretePrisonerContact(prisonerNumber: String = "A1234AB"): PrisonerContactSummary {
    val requestedRelationship = ContactRelationship(
      prisonerNumber = prisonerNumber,
      relationshipType = "S",
      relationshipToPrisoner = "BRO",
      isNextOfKin = false,
      isEmergencyContact = false,
      comments = null,
    )
    val request = CreateContactRequest(
      lastName = RandomStringUtils.secure().next(35),
      firstName = "a new guy",
      createdBy = "created",
      relationship = requestedRelationship,
    )

    testAPIClient.createAContact(request)

    val prisonerContacts = testAPIClient.getPrisonerContacts(prisonerNumber).content
    assertThat(prisonerContacts).hasSize(1)
    val prisonerContact = prisonerContacts[0]
    assertExistingContact(prisonerContact, requestedRelationship)
    return prisonerContact
  }

  private fun assertExistingContact(
    prisonerContact: PrisonerContactSummary,
    relationship: ContactRelationship,
  ) {
    with(prisonerContact) {
      assertThat(relationshipToPrisoner).isEqualTo(relationship.relationshipToPrisoner)
      assertThat(nextOfKin).isEqualTo(relationship.isNextOfKin)
      assertThat(emergencyContact).isEqualTo(relationship.isEmergencyContact)
      assertThat(comments).isEqualTo(relationship.comments)
    }
  }
}
