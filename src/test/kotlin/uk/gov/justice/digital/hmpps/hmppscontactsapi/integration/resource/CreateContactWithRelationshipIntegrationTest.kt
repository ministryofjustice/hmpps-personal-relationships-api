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
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.PostgresIntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.ContactRelationship
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.CreateContactRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.PrisonerContactRelationshipDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.ContactRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.ContactInfo
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.PersonReference
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.PrisonerContactInfo
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.Source
import uk.gov.justice.digital.hmpps.hmppscontactsapi.util.StubUser
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

class CreateContactWithRelationshipIntegrationTest : PostgresIntegrationTestBase() {

  @Autowired
  protected lateinit var contactRepository: ContactRepository

  @BeforeEach
  fun setUp() {
    setCurrentUser(StubUser.CREATING_USER)
  }

  @ParameterizedTest
  @CsvSource(
    value = [
      "relationship.prisonerNumber must not be null;{\"relationshipTypeCode\": \"S\", \"relationshipToPrisonerCode\": \"FRI\", \"isNextOfKin\": false, \"isEmergencyContact\": false }",
      "relationship.relationshipTypeCode must not be null;{ \"prisonerNumber\": \"A1234BC\", \"relationshipToPrisonerCode\": \"FRI\", \"isNextOfKin\": false, \"isEmergencyContact\": false }",
      "relationship.relationshipToPrisonerCode must not be null;{ \"prisonerNumber\": \"A1234BC\", \"relationshipTypeCode\": \"S\", \"isNextOfKin\": false, \"isEmergencyContact\": false }",
      "relationship.isNextOfKin must not be null;{ \"prisonerNumber\": \"A1234BC\", \"relationshipTypeCode\": \"S\", \"relationshipToPrisonerCode\": \"FRI\", \"isEmergencyContact\": false }",
      "relationship.isEmergencyContact must not be null;{ \"prisonerNumber\": \"A1234BC\", \"relationshipTypeCode\": \"S\", \"relationshipToPrisonerCode\": \"FRI\", \"isNextOfKin\": false }",
    ],
    delimiter = ';',
  )
  fun `should return bad request if required fields are null`(expectedMessage: String, relationShipJson: String) {
    val json =
      "{\"firstName\": \"first\", \"lastName\": \"last\", \"relationship\": $relationShipJson}"
    val errors = webTestClient.post()
      .uri("/contact")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CONTACTS_ADMIN")))
      .bodyValue(json)
      .exchange()
      .expectStatus()
      .isBadRequest
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody!!

    assertThat(errors.userMessage).isEqualTo("Validation failure: $expectedMessage")
    stubEvents.assertHasNoEvents(event = OutboundEvent.CONTACT_CREATED)
    stubEvents.assertHasNoEvents(event = OutboundEvent.PRISONER_CONTACT_CREATED)
  }

  @ParameterizedTest
  @MethodSource("allFieldConstraintViolations")
  fun `should enforce field constraints`(expectedMessage: String, request: CreateContactRequest) {
    stubPrisonSearchWithResponse(request.relationship!!.prisonerNumber)
    val errors = webTestClient.post()
      .uri("/contact")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CONTACTS_ADMIN")))
      .bodyValue(request)
      .exchange()
      .expectStatus()
      .isBadRequest
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody!!

    assertThat(errors.userMessage).isEqualTo("Validation failure(s): $expectedMessage")
    stubEvents.assertHasNoEvents(event = OutboundEvent.CONTACT_CREATED)
    stubEvents.assertHasNoEvents(event = OutboundEvent.PRISONER_CONTACT_CREATED)
  }

  @Test
  fun `should return a 404 if the prisoner can not be found`() {
    val prisonerNumber = "A1234AB"
    stubPrisonSearchWithNotFoundResponse(prisonerNumber)
    val request = CreateContactRequest(
      lastName = RandomStringUtils.secure().nextAlphabetic(35),
      firstName = "a new guy",
      relationship = ContactRelationship(
        prisonerNumber = prisonerNumber,
        relationshipTypeCode = "S",
        relationshipToPrisonerCode = "FRI",
        isNextOfKin = false,
        isEmergencyContact = false,
        isApprovedVisitor = false,
        comments = null,
      ),
    )

    val errors = webTestClient.post()
      .uri("/contact")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CONTACTS_ADMIN")))
      .bodyValue(request)
      .exchange()
      .expectStatus()
      .isNotFound
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody!!

    assertThat(errors.userMessage).isEqualTo("Entity not found : Prisoner (A1234AB) could not be found")
    stubEvents.assertHasNoEvents(event = OutboundEvent.CONTACT_CREATED)
    stubEvents.assertHasNoEvents(event = OutboundEvent.PRISONER_CONTACT_CREATED)
  }

  @Test
  fun `should reject the contact relationship if it's not a supported relationship type`() {
    val prisonerNumber = "A1234AB"
    stubPrisonSearchWithResponse(prisonerNumber)
    val requestedRelationship = ContactRelationship(
      prisonerNumber = prisonerNumber,
      relationshipTypeCode = "S",
      relationshipToPrisonerCode = "FOO",
      isNextOfKin = false,
      isEmergencyContact = false,
      isApprovedVisitor = false,
      comments = null,
    )
    val request = CreateContactRequest(
      lastName = RandomStringUtils.secure().nextAlphabetic(35),
      firstName = "a new guy",
      relationship = requestedRelationship,
    )

    val errors = webTestClient.post()
      .uri("/contact")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CONTACTS_ADMIN")))
      .bodyValue(request)
      .exchange()
      .expectStatus()
      .isBadRequest
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody!!

    assertThat(errors.userMessage).isEqualTo("Validation failure: Unsupported social relationship (FOO)")
    stubEvents.assertHasNoEvents(event = OutboundEvent.CONTACT_CREATED)
    stubEvents.assertHasNoEvents(event = OutboundEvent.PRISONER_CONTACT_CREATED)

    // Check that we do not create a contact without the relationship
    val createdContact = contactRepository.findAll().find { it.lastName == request.lastName }
    assertThat(createdContact).isNull()
  }

  @Test
  fun `should create the contact relationship with minimal fields`() {
    val prisonerNumber = "A1234AB"
    stubPrisonSearchWithResponse(prisonerNumber)
    val requestedRelationship = ContactRelationship(
      prisonerNumber = prisonerNumber,
      relationshipTypeCode = "S",
      relationshipToPrisonerCode = "FRI",
      isNextOfKin = false,
      isEmergencyContact = false,
      isApprovedVisitor = false,
      comments = null,
    )
    val request = CreateContactRequest(
      lastName = RandomStringUtils.secure().nextAlphabetic(35),
      firstName = "a new guy",
      relationship = requestedRelationship,
    )

    val created = testAPIClient.createAContactWithARelationship(request)

    assertThat(created.createdContact.lastName).isEqualTo(request.lastName)
    asserPrisonerContactEquals(created.createdRelationship!!, requestedRelationship)

    stubEvents.assertHasEvent(
      event = OutboundEvent.CONTACT_CREATED,
      additionalInfo = ContactInfo(created.createdContact.id, Source.DPS, "created"),
      personReference = PersonReference(dpsContactId = created.createdContact.id),
    )

    stubEvents.assertHasEvent(
      event = OutboundEvent.PRISONER_CONTACT_CREATED,
      additionalInfo = PrisonerContactInfo(created.createdRelationship!!.prisonerContactId, Source.DPS, "created"),
      personReference = PersonReference(dpsContactId = created.createdContact.id, nomsNumber = request.relationship!!.prisonerNumber),
    )
  }

  @ParameterizedTest
  @ValueSource(strings = ["ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__RW"])
  fun `should create the contact relationship with all fields`(role: String) {
    val prisonerNumber = "A1234AA"
    stubPrisonSearchWithResponse(prisonerNumber)
    val requestedRelationship = ContactRelationship(
      prisonerNumber = prisonerNumber,
      relationshipTypeCode = "S",
      relationshipToPrisonerCode = "FRI",
      isNextOfKin = true,
      isEmergencyContact = true,
      isApprovedVisitor = true,
      comments = "Some comments",
    )
    val request = CreateContactRequest(
      lastName = RandomStringUtils.secure().nextAlphabetic(35),
      firstName = "a new guy",
      relationship = requestedRelationship,
    )

    val created = testAPIClient.createAContactWithARelationship(request, role)

    assertThat(created.createdContact.lastName).isEqualTo(request.lastName)
    asserPrisonerContactEquals(created.createdRelationship!!, requestedRelationship)

    stubEvents.assertHasEvent(
      event = OutboundEvent.CONTACT_CREATED,
      additionalInfo = ContactInfo(created.createdContact.id, Source.DPS, "created"),
      personReference = PersonReference(dpsContactId = created.createdContact.id),
    )

    stubEvents.assertHasEvent(
      event = OutboundEvent.PRISONER_CONTACT_CREATED,
      additionalInfo = PrisonerContactInfo(created.createdRelationship!!.prisonerContactId, Source.DPS, "created"),
      personReference = PersonReference(dpsContactId = created.createdContact.id, nomsNumber = request.relationship!!.prisonerNumber),
    )
  }

  private fun asserPrisonerContactEquals(
    prisonerContact: PrisonerContactRelationshipDetails,
    relationship: ContactRelationship,
  ) {
    with(prisonerContact) {
      assertThat(relationshipTypeCode).isEqualTo(relationship.relationshipTypeCode)
      assertThat(relationshipToPrisonerCode).isEqualTo(relationship.relationshipToPrisonerCode)
      assertThat(isNextOfKin).isEqualTo(relationship.isNextOfKin)
      assertThat(isEmergencyContact).isEqualTo(relationship.isEmergencyContact)
      assertThat(isApprovedVisitor).isEqualTo(relationship.isApprovedVisitor)
      assertThat(comments).isEqualTo(relationship.comments)
    }
  }

  companion object {
    @JvmStatic
    fun allFieldConstraintViolations(): List<Arguments> {
      val relationship = ContactRelationship(
        prisonerNumber = "A1234AB",
        relationshipTypeCode = "S",
        relationshipToPrisonerCode = "FRI",
        isNextOfKin = false,
        isEmergencyContact = false,
        isApprovedVisitor = false,
        comments = null,
      )
      return listOf(
        Arguments.of(
          "relationship.comments must be <= 240 characters",
          CreateContactRequest(
            lastName = RandomStringUtils.secure().nextAlphabetic(35),
            firstName = "a new guy",
            relationship = relationship.copy(comments = "".padStart(241, 'X')),
          ),
        ),
      )
    }
  }
}
