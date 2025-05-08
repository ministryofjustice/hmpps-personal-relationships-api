package uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.resource

import org.apache.commons.lang3.RandomStringUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import org.openapitools.jackson.nullable.JsonNullable
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.SecureAPIIntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.AddContactRelationshipRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.ContactRelationship
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.CreateContactRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.PatchRelationshipRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.PrisonerContactSummary
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.PersonReference
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.PrisonerContactInfo
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.Source
import uk.gov.justice.digital.hmpps.hmppscontactsapi.util.StubUser
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

class PatchContactRelationshipIntegrationTest : SecureAPIIntegrationTestBase() {

  override val allowedRoles: Set<String> = setOf("ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__RW")

  @BeforeEach
  fun setUp() {
    setCurrentUser(StubUser.READ_WRITE_USER)
  }

  override fun baseRequestBuilder(): WebTestClient.RequestHeadersSpec<*> = webTestClient.patch()
    .uri("/prisoner-contact/99")
    .accept(MediaType.APPLICATION_JSON)
    .contentType(MediaType.APPLICATION_JSON)
    .bodyValue(PatchRelationshipRequest())

  @ParameterizedTest
  @CsvSource(
    value = [
      "Unsupported relationship type null.;{\"relationshipTypeCode\": null,  \"updatedBy\": \"Admin\"}",
      "Unsupported relationship to prisoner null.;{\"relationshipToPrisonerCode\": null,  \"updatedBy\": \"Admin\"}",
      "Unsupported approved visitor value null.;{\"isApprovedVisitor\": null,  \"updatedBy\": \"Admin\"}",
      "Unsupported emergency contact null.;{\"isEmergencyContact\": null,  \"updatedBy\": \"Admin\"}",
      "Unsupported next of kin null.;{\"isNextOfKin\": null,  \"updatedBy\": \"Admin\"}",
      "Unsupported relationship status null.;{\"isRelationshipActive\": null,  \"updatedBy\": \"Admin\"}",
    ],
    delimiter = ';',
  )
  fun `should return bad request when optional fields set with a null value `(
    expectedMessage: String,
    relationShipJson: String,
  ) {
    val prisonerNumber = getRandomPrisonerCode()
    stubPrisonSearchWithResponse(prisonerNumber)
    val prisonerContact = cretePrisonerContact(prisonerNumber)

    val prisonerContactId = prisonerContact.prisonerContactId

    val errors = webTestClient.patch()
      .uri("/prisoner-contact/$prisonerContactId")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationUsingCurrentUser())
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

  @ParameterizedTest
  @MethodSource("allFieldConstraintViolations")
  fun `should check all field constraints`(expectedMessage: String, request: PatchRelationshipRequest) {
    val prisonerNumber = getRandomPrisonerCode()
    stubPrisonSearchWithResponse(prisonerNumber)
    val prisonerContact = cretePrisonerContact(prisonerNumber)

    val prisonerContactId = prisonerContact.prisonerContactId

    val errors = webTestClient.patch()
      .uri("/prisoner-contact/$prisonerContactId")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationUsingCurrentUser())
      .bodyValue(request)
      .exchange()
      .expectStatus()
      .isBadRequest
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody!!

    assertThat(errors.userMessage).isEqualTo("Validation failure(s): $expectedMessage")
    stubEvents.assertHasNoEvents(event = OutboundEvent.PRISONER_CONTACT_UPDATED)
  }

  @Test
  fun `should update the contact relationship with relationship code fields`() {
    val prisonerNumber = getRandomPrisonerCode()
    stubPrisonSearchWithResponse(prisonerNumber)
    val prisonerContact = cretePrisonerContact(prisonerNumber)

    val updateRequest = PatchRelationshipRequest(
      relationshipToPrisonerCode = JsonNullable.of("SIS"),
    )

    val prisonerContactId = prisonerContact.prisonerContactId

    testAPIClient.updateRelationship(prisonerContactId, updateRequest)

    val updatedPrisonerContacts = testAPIClient.getPrisonerContacts(prisonerNumber).content
    assertThat(updatedPrisonerContacts).hasSize(1)
    assertThat(updatedPrisonerContacts[0].relationshipToPrisonerCode).isEqualTo("SIS")
    stubEvents.assertHasEvent(
      event = OutboundEvent.PRISONER_CONTACT_UPDATED,
      additionalInfo = PrisonerContactInfo(prisonerContactId, Source.DPS, "read_write_user"),
      personReference = PersonReference(prisonerNumber, prisonerContact.contactId),
    )
  }

  @Test
  fun `should prevent updating the contact relationship if it would create a duplicate`() {
    val prisonerNumber = getRandomPrisonerCode()
    stubPrisonSearchWithResponse(prisonerNumber)
    val firstRelationship = cretePrisonerContact(prisonerNumber)
    testAPIClient.addAContactRelationship(
      AddContactRelationshipRequest(
        firstRelationship.contactId,
        ContactRelationship(
          prisonerNumber = prisonerNumber,
          relationshipTypeCode = "S",
          relationshipToPrisonerCode = "SIS",
          isNextOfKin = false,
          isEmergencyContact = false,
          isApprovedVisitor = false,
          comments = null,
        ),
      ),
    )

    val updateRequest = PatchRelationshipRequest(
      // try to change BRO to a second SIS
      relationshipToPrisonerCode = JsonNullable.of("SIS"),
    )

    webTestClient.patch()
      .uri("/prisoner-contact/${firstRelationship.prisonerContactId}")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(testAPIClient.setAuthorisationUsingCurrentUser())
      .bodyValue(updateRequest)
      .exchange()
      .expectStatus()
      .isEqualTo(HttpStatus.CONFLICT)
  }

  @Test
  fun `should update the contact relationship from social to official`() {
    val prisonerNumber = getRandomPrisonerCode()
    stubPrisonSearchWithResponse(prisonerNumber)
    val prisonerContact = cretePrisonerContact(prisonerNumber)

    val updateRequest = PatchRelationshipRequest(
      relationshipTypeCode = JsonNullable.of("O"),
      relationshipToPrisonerCode = JsonNullable.of("DR"),
    )

    val prisonerContactId = prisonerContact.prisonerContactId

    testAPIClient.updateRelationship(prisonerContactId, updateRequest)

    val updatedPrisonerContacts = testAPIClient.getPrisonerContacts(prisonerNumber).content
    assertThat(updatedPrisonerContacts).hasSize(1)
    assertThat(updatedPrisonerContacts[0].relationshipTypeCode).isEqualTo("O")
    assertThat(updatedPrisonerContacts[0].relationshipToPrisonerCode).isEqualTo("DR")
    stubEvents.assertHasEvent(
      event = OutboundEvent.PRISONER_CONTACT_UPDATED,
      additionalInfo = PrisonerContactInfo(prisonerContactId, Source.DPS, "read_write_user"),
      personReference = PersonReference(prisonerNumber, prisonerContact.contactId),
    )
  }

  @Test
  fun `should update the contact relationship with next of kin fields`() {
    val prisonerNumber = getRandomPrisonerCode()
    stubPrisonSearchWithResponse(prisonerNumber)
    val prisonerContact = cretePrisonerContact(prisonerNumber)

    val updateRequest = PatchRelationshipRequest(
      isNextOfKin = JsonNullable.of(true),
    )

    val prisonerContactId = prisonerContact.prisonerContactId

    testAPIClient.updateRelationship(prisonerContactId, updateRequest)

    val updatedPrisonerContacts = testAPIClient.getPrisonerContacts(prisonerNumber).content
    assertThat(updatedPrisonerContacts).hasSize(1)
    assertThat(updatedPrisonerContacts[0].isNextOfKin).isTrue
    stubEvents.assertHasEvent(
      event = OutboundEvent.PRISONER_CONTACT_UPDATED,
      additionalInfo = PrisonerContactInfo(prisonerContactId, Source.DPS, "read_write_user"),
      personReference = PersonReference(prisonerNumber, prisonerContact.contactId),
    )
  }

  @Test
  fun `should update the contact relationship with approved visitor`() {
    val prisonerNumber = getRandomPrisonerCode()
    stubPrisonSearchWithResponse(prisonerNumber)
    val prisonerContact = cretePrisonerContact(prisonerNumber)

    val updateRequest = PatchRelationshipRequest(
      isApprovedVisitor = JsonNullable.of(true),
    )

    val prisonerContactId = prisonerContact.prisonerContactId

    testAPIClient.updateRelationship(prisonerContactId, updateRequest)

    val updatedPrisonerContacts = testAPIClient.getPrisonerContacts(prisonerNumber).content
    assertThat(updatedPrisonerContacts).hasSize(1)
    assertThat(updatedPrisonerContacts[0].isApprovedVisitor).isTrue
    stubEvents.assertHasEvent(
      event = OutboundEvent.PRISONER_CONTACT_UPDATED,
      additionalInfo = PrisonerContactInfo(prisonerContactId, Source.DPS, "read_write_user"),
      personReference = PersonReference(prisonerNumber, prisonerContact.contactId),
    )
  }

  @Test
  fun `should update the contact relationship with emergency contact fields`() {
    val prisonerNumber = getRandomPrisonerCode()
    stubPrisonSearchWithResponse(prisonerNumber)
    val prisonerContact = cretePrisonerContact(prisonerNumber)

    val updateRequest = PatchRelationshipRequest(
      isEmergencyContact = JsonNullable.of(true),
    )

    val prisonerContactId = prisonerContact.prisonerContactId

    testAPIClient.updateRelationship(prisonerContactId, updateRequest)

    val updatedPrisonerContacts = testAPIClient.getPrisonerContacts(prisonerNumber).content
    assertThat(updatedPrisonerContacts).hasSize(1)
    assertThat(updatedPrisonerContacts[0].isEmergencyContact).isTrue
    stubEvents.assertHasEvent(
      event = OutboundEvent.PRISONER_CONTACT_UPDATED,
      additionalInfo = PrisonerContactInfo(prisonerContactId, Source.DPS, "read_write_user"),
      personReference = PersonReference(prisonerNumber, prisonerContact.contactId),
    )
  }

  @Test
  fun `should update the contact relationship with relationship active fields`() {
    val prisonerNumber = getRandomPrisonerCode()
    stubPrisonSearchWithResponse(prisonerNumber)
    val prisonerContact = cretePrisonerContact(prisonerNumber)

    val updateRequest = PatchRelationshipRequest(
      isRelationshipActive = JsonNullable.of(true),
    )

    val prisonerContactId = prisonerContact.prisonerContactId

    testAPIClient.updateRelationship(prisonerContactId, updateRequest)

    val updatedPrisonerContacts = testAPIClient.getPrisonerContacts(prisonerNumber).content
    assertThat(updatedPrisonerContacts).hasSize(1)
    assertThat(updatedPrisonerContacts[0].isRelationshipActive).isTrue
    stubEvents.assertHasEvent(
      event = OutboundEvent.PRISONER_CONTACT_UPDATED,
      additionalInfo = PrisonerContactInfo(prisonerContactId, Source.DPS, "read_write_user"),
      personReference = PersonReference(prisonerNumber, prisonerContact.contactId),
    )
  }

  @Test
  fun `should update the contact relationship with comment fields`() {
    val prisonerNumber = getRandomPrisonerCode()
    stubPrisonSearchWithResponse(prisonerNumber)
    val prisonerContact = cretePrisonerContact(prisonerNumber)

    val updateRequest = PatchRelationshipRequest(
      comments = JsonNullable.of("New comment"),
    )

    val prisonerContactId = prisonerContact.prisonerContactId

    testAPIClient.updateRelationship(prisonerContactId, updateRequest)

    val updatedPrisonerContacts = testAPIClient.getPrisonerContacts(prisonerNumber).content
    assertThat(updatedPrisonerContacts).hasSize(1)
    assertThat(updatedPrisonerContacts[0].comments).isEqualTo("New comment")
    stubEvents.assertHasEvent(
      event = OutboundEvent.PRISONER_CONTACT_UPDATED,
      additionalInfo = PrisonerContactInfo(prisonerContactId, Source.DPS, "read_write_user"),
      personReference = PersonReference(prisonerNumber, prisonerContact.contactId),
    )
  }

  @Test
  fun `should update the contact relationship with minimal fields`() {
    val prisonerNumber = getRandomPrisonerCode()
    stubPrisonSearchWithResponse(prisonerNumber)
    val prisonerContact = cretePrisonerContact(prisonerNumber)

    val updateRequest = PatchRelationshipRequest()

    val prisonerContactId = prisonerContact.prisonerContactId

    testAPIClient.updateRelationship(prisonerContactId, updateRequest)

    val updatedPrisonerContacts = testAPIClient.getPrisonerContacts(prisonerNumber).content
    assertThat(updatedPrisonerContacts).hasSize(1)
    stubEvents.assertHasEvent(
      event = OutboundEvent.PRISONER_CONTACT_UPDATED,
      additionalInfo = PrisonerContactInfo(prisonerContactId, Source.DPS, "read_write_user"),
      personReference = PersonReference(prisonerNumber, prisonerContact.contactId),
    )
  }

  @ParameterizedTest
  @ValueSource(strings = ["ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__RW"])
  fun `should update the contact relationship with all fields`(role: String) {
    setCurrentUser(StubUser.READ_WRITE_USER.copy(roles = listOf(role)))
    val prisonerNumber = getRandomPrisonerCode()
    stubPrisonSearchWithResponse(prisonerNumber)
    val prisonerContact = cretePrisonerContact(prisonerNumber)

    val updateRequest = PatchRelationshipRequest(
      relationshipToPrisonerCode = JsonNullable.of("FRI"),
      isNextOfKin = JsonNullable.of(true),
      isApprovedVisitor = JsonNullable.of(true),
      isEmergencyContact = JsonNullable.of(true),
      isRelationshipActive = JsonNullable.of(true),
      comments = JsonNullable.of("comments added"),
    )

    val prisonerContactId = prisonerContact.prisonerContactId

    testAPIClient.updateRelationship(prisonerContactId, updateRequest)

    val updatedPrisonerContacts = testAPIClient.getPrisonerContacts(prisonerNumber).content

    assertThat(updatedPrisonerContacts).hasSize(1)
    assertUpdatedPrisonerContactEquals(updatedPrisonerContacts[0], updateRequest)
    stubEvents.assertHasEvent(
      event = OutboundEvent.PRISONER_CONTACT_UPDATED,
      additionalInfo = PrisonerContactInfo(prisonerContactId, Source.DPS, "read_write_user"),
      personReference = PersonReference(prisonerNumber, prisonerContact.contactId),
    )
  }

  private fun assertUpdatedPrisonerContactEquals(
    prisonerContact: PrisonerContactSummary,
    relationship: PatchRelationshipRequest,
  ) {
    with(prisonerContact) {
      assertThat(relationshipToPrisonerCode).isEqualTo(relationship.relationshipToPrisonerCode.get())
      assertThat(isNextOfKin).isEqualTo(relationship.isNextOfKin.get())
      assertThat(isApprovedVisitor).isEqualTo(relationship.isApprovedVisitor.get())
      assertThat(isEmergencyContact).isEqualTo(relationship.isEmergencyContact.get())
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
      relationshipTypeCode = "S",
      relationshipToPrisonerCode = "BRO",
      isNextOfKin = false,
      isEmergencyContact = false,
      isApprovedVisitor = false,
      comments = null,
    )
    val request = CreateContactRequest(
      lastName = RandomStringUtils.secure().next(35),
      firstName = "a new guy",
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
      assertThat(relationshipToPrisonerCode).isEqualTo(relationship.relationshipToPrisonerCode)
      assertThat(isNextOfKin).isEqualTo(relationship.isNextOfKin)
      assertThat(isEmergencyContact).isEqualTo(relationship.isEmergencyContact)
      assertThat(comments).isEqualTo(relationship.comments)
    }
  }

  companion object {
    @JvmStatic
    fun allFieldConstraintViolations(): List<Arguments> {
      val relationship = PatchRelationshipRequest(
        relationshipToPrisonerCode = JsonNullable.of("FRI"),
        isNextOfKin = JsonNullable.of(true),
        isApprovedVisitor = JsonNullable.of(true),
        isEmergencyContact = JsonNullable.of(true),
        isRelationshipActive = JsonNullable.of(true),
        comments = JsonNullable.of("comments added"),
      )
      return listOf(
        Arguments.of(
          "comments must be <= 240 characters",
          relationship.copy(comments = JsonNullable.of("".padStart(241, 'X'))),
        ),
      )
    }
  }
}
