package uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.resource

import org.apache.commons.lang3.RandomStringUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.SecureAPIIntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.AddContactRelationshipRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.ContactRelationship
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.CreateContactRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.ContactDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.PersonReference
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.PrisonerContactInfo
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.Source
import uk.gov.justice.digital.hmpps.hmppscontactsapi.util.StubUser
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

class AddContactRelationshipIntegrationTest : SecureAPIIntegrationTestBase() {

  private lateinit var contact: ContactDetails

  override val allowedRoles: Set<String> = setOf("ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__RW")

  @BeforeEach
  fun setUp() {
    setCurrentUser(StubUser.READ_WRITE_USER)
    contact = testAPIClient.createAContact(
      CreateContactRequest(
        lastName = RandomStringUtils.secure().nextAlphabetic(10),
        firstName = RandomStringUtils.secure().nextAlphabetic(10),
      ),
    )
  }

  override fun baseRequestBuilder(): WebTestClient.RequestHeadersSpec<*> = webTestClient.post()
    .uri("/prisoner-contact")
    .accept(MediaType.APPLICATION_JSON)
    .contentType(MediaType.APPLICATION_JSON)
    .bodyValue(aMinimalRequest())

  @ParameterizedTest
  @CsvSource(
    value = [
      "relationship must not be null;{\"contactId\": 99, \"relationship\": null, \"createdBy\": \"USER\"}",
      "relationship must not be null;{\"contactId\": 99, \"createdBy\": \"USER\"}",
      "contactId must not be null;{\"contactId\": null, \"relationship\": {\"prisonerNumber\": \"A1324BC\", \"relationshipTypeCode\": \"S\", \"relationshipToPrisonerCode\": \"MOT\", \"isNextOfKin\": false, \"isEmergencyContact\": false, \"isApprovedVisitor\": false}, \"createdBy\": \"USER\"}",
      "contactId must not be null;{\"relationship\": {\"prisonerNumber\": \"A1324BC\", \"relationshipTypeCode\": \"S\", \"relationshipToPrisonerCode\": \"MOT\", \"isNextOfKin\": false, \"isEmergencyContact\": false, \"isApprovedVisitor\": false}, \"createdBy\": \"USER\"}",
      "relationship.prisonerNumber must not be null;{\"contactId\": 99, \"relationship\": {\"prisonerNumber\": null, \"relationshipTypeCode\": \"S\", \"relationshipToPrisonerCode\": \"MOT\", \"isNextOfKin\": false, \"isEmergencyContact\": false, \"isApprovedVisitor\": false}, \"createdBy\": \"USER\"}",
      "relationship.prisonerNumber must not be null;{\"contactId\": 99, \"relationship\": {\"relationshipTypeCode\": \"S\", \"relationshipToPrisonerCode\": \"MOT\", \"isNextOfKin\": false, \"isEmergencyContact\": false, \"isApprovedVisitor\": false}, \"createdBy\": \"USER\"}",
      "relationship.relationshipTypeCode must not be null;{\"contactId\": 99, \"relationship\": {\"prisonerNumber\": \"A1324BC\", \"relationshipToPrisonerCode\": \"MOT\", \"isNextOfKin\": false, \"isEmergencyContact\": false, \"isApprovedVisitor\": false}, \"createdBy\": \"USER\"}",
      "relationship.relationshipTypeCode must not be null;{\"contactId\": 99, \"relationship\": {\"prisonerNumber\": \"A1324BC\", \"relationshipTypeCode\":null, \"relationshipToPrisonerCode\": \"MOT\", \"isNextOfKin\": false, \"isEmergencyContact\": false, \"isApprovedVisitor\": false}, \"createdBy\": \"USER\"}",
      "relationship.relationshipToPrisonerCode must not be null;{\"contactId\": 99, \"relationship\": {\"prisonerNumber\": \"A1324BC\", \"relationshipTypeCode\": \"S\", \"relationshipToPrisonerCode\": null, \"isNextOfKin\": false, \"isEmergencyContact\": false, \"isApprovedVisitor\": false}, \"createdBy\": \"USER\"}",
      "relationship.relationshipToPrisonerCode must not be null;{\"contactId\": 99, \"relationship\": {\"prisonerNumber\": \"A1324BC\", \"relationshipTypeCode\": \"S\", \"isNextOfKin\": false, \"isEmergencyContact\": false, \"isApprovedVisitor\": false}, \"createdBy\": \"USER\"}",
      "relationship.isNextOfKin must not be null;{\"contactId\": 99, \"relationship\": {\"prisonerNumber\": \"A1324BC\", \"relationshipTypeCode\": \"S\", \"relationshipToPrisonerCode\": \"MOT\", \"isEmergencyContact\": false, \"isApprovedVisitor\": false}, \"createdBy\": \"USER\"}",
      "relationship.isEmergencyContact must not be null;{\"contactId\": 99, \"relationship\": {\"prisonerNumber\": \"A1324BC\", \"relationshipTypeCode\": \"S\", \"relationshipToPrisonerCode\": \"MOT\", \"isNextOfKin\": false, \"isApprovedVisitor\": false}, \"createdBy\": \"USER\"}",
      "relationship.isApprovedVisitor must not be null;{\"contactId\": 99, \"relationship\": {\"prisonerNumber\": \"A1324BC\", \"relationshipTypeCode\": \"S\", \"relationshipToPrisonerCode\": \"MOT\", \"isNextOfKin\": false, \"isEmergencyContact\": false}, \"createdBy\": \"USER\"}",
    ],
    delimiter = ';',
  )
  fun `should return bad request if required fields are null`(expectedMessage: String, json: String) {
    val errors = webTestClient.post()
      .uri("/prisoner-contact")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationUsingCurrentUser())
      .bodyValue(json)
      .exchange()
      .expectStatus()
      .isBadRequest
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody!!

    assertThat(errors.userMessage).isEqualTo("Validation failure: $expectedMessage")
  }

  @Test
  fun `Should return 404 if the prisoner can't be found`() {
    val errors = webTestClient.post()
      .uri("/prisoner-contact")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationUsingCurrentUser())
      .bodyValue(aMinimalRequest())
      .exchange()
      .expectStatus()
      .isNotFound
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody!!

    assertThat(errors.userMessage).isEqualTo("Entity not found : Prisoner (A1234BC) could not be found")
  }

  @Test
  fun `Should return 404 if the contact can't be found`() {
    stubPrisonSearchWithResponse("A1234BC")

    val errors = webTestClient.post()
      .uri("/prisoner-contact")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationUsingCurrentUser())
      .bodyValue(aMinimalRequest().copy(contactId = 123456789))
      .exchange()
      .expectStatus()
      .isNotFound
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody!!

    assertThat(errors.userMessage).isEqualTo("Entity not found : Contact (123456789) could not be found")
  }

  @Test
  fun `Should prevent new duplicate relationships being added`() {
    stubPrisonSearchWithResponse("A1234BC")

    val request = AddContactRelationshipRequest(
      contactId = contact.id,
      relationship = ContactRelationship(
        prisonerNumber = "A1234BC",
        relationshipToPrisonerCode = "MOT",
        isNextOfKin = true,
        relationshipTypeCode = "S",
        isEmergencyContact = false,
        isApprovedVisitor = false,
      ),
    )

    testAPIClient.addAContactRelationship(request)
    webTestClient.post()
      .uri("/prisoner-contact")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(testAPIClient.setAuthorisationUsingCurrentUser())
      .bodyValue(request)
      .exchange()
      .expectStatus()
      .isEqualTo(HttpStatus.CONFLICT)
  }

  @Test
  fun `should create the contact relationship with minimal fields`() {
    stubPrisonSearchWithResponse("A1234BC")

    val request = AddContactRelationshipRequest(
      contactId = contact.id,
      relationship = ContactRelationship(
        prisonerNumber = "A1234BC",
        relationshipToPrisonerCode = "MOT",
        isNextOfKin = true,
        relationshipTypeCode = "S",
        isEmergencyContact = false,
        isApprovedVisitor = false,
      ),
    )

    val createdRelationship = testAPIClient.addAContactRelationship(request)

    assertThat(createdRelationship.relationshipToPrisonerCode).isEqualTo("MOT")
    assertThat(createdRelationship.relationshipToPrisonerDescription).isEqualTo("Mother")
    assertThat(createdRelationship.isNextOfKin).isTrue()
    assertThat(createdRelationship.isEmergencyContact).isFalse()
    assertThat(createdRelationship.comments).isNull()

    stubEvents.assertHasEvent(
      event = OutboundEvent.PRISONER_CONTACT_CREATED,
      additionalInfo = PrisonerContactInfo(
        createdRelationship.prisonerContactId,
        source = Source.DPS,
        "read_write_user",
        "BXI",
      ),
      personReference = PersonReference(dpsContactId = contact.id, nomsNumber = request.relationship.prisonerNumber),
    )
  }

  @ParameterizedTest
  @MethodSource("allFieldConstraintViolations")
  fun `should enforce field constraints`(expectedMessage: String, relationship: ContactRelationship) {
    stubPrisonSearchWithResponse("A1234BC")

    val errors = webTestClient.post()
      .uri("/prisoner-contact")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationUsingCurrentUser())
      .bodyValue(aMinimalRequest().copy(relationship = relationship))
      .exchange()
      .expectStatus()
      .isBadRequest
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody!!

    assertThat(errors.userMessage).isEqualTo("Validation failure(s): $expectedMessage")
    stubEvents.assertHasNoEvents(event = OutboundEvent.PRISONER_CONTACT_CREATED)
  }

  @Test
  fun `should create the contact with all fields`() {
    stubPrisonSearchWithResponse("A1234BC")

    val request = AddContactRelationshipRequest(
      contactId = contact.id,
      relationship = ContactRelationship(
        prisonerNumber = "A1234BC",
        relationshipToPrisonerCode = "MOT",
        isNextOfKin = false,
        relationshipTypeCode = "S",
        isEmergencyContact = true,
        isApprovedVisitor = true,
        comments = "Some comments",
      ),
    )

    val createdRelationship = testAPIClient.addAContactRelationship(request)
    assertThat(createdRelationship.relationshipToPrisonerCode).isEqualTo("MOT")
    assertThat(createdRelationship.relationshipToPrisonerDescription).isEqualTo("Mother")
    assertThat(createdRelationship.isNextOfKin).isFalse()
    assertThat(createdRelationship.isEmergencyContact).isTrue()
    assertThat(createdRelationship.isApprovedVisitor).isTrue()
    assertThat(createdRelationship.comments).isEqualTo("Some comments")

    stubEvents.assertHasEvent(
      event = OutboundEvent.PRISONER_CONTACT_CREATED,
      additionalInfo = PrisonerContactInfo(
        createdRelationship.prisonerContactId,
        source = Source.DPS,
        "read_write_user",
        "BXI",
      ),
      personReference = PersonReference(dpsContactId = contact.id, nomsNumber = request.relationship.prisonerNumber),
    )
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
          relationship.copy(comments = "".padStart(241, 'X')),
        ),
      )
    }
  }

  private fun aMinimalRequest() = AddContactRelationshipRequest(
    contactId = contact.id,
    relationship = ContactRelationship(
      prisonerNumber = "A1234BC",
      relationshipToPrisonerCode = "MOT",
      isNextOfKin = true,
      relationshipTypeCode = "S",
      isEmergencyContact = false,
      isApprovedVisitor = false,
    ),
  )
}
