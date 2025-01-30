package uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.resource

import org.apache.commons.lang3.RandomStringUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
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
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

class AddContactRelationshipIntegrationTest : SecureAPIIntegrationTestBase() {

  private lateinit var contact: ContactDetails

  override val allowedRoles: Set<String> = setOf("ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__RW")

  @BeforeEach
  fun setUp() {
    contact = testAPIClient.createAContact(
      CreateContactRequest(
        firstName = RandomStringUtils.secure().nextAlphabetic(10),
        lastName = RandomStringUtils.secure().nextAlphabetic(10),
        createdBy = "USER",
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
      "contactId must not be null;{\"contactId\": null, \"relationship\": {\"prisonerNumber\": \"A1324BC\", \"relationshipType\": \"S\", \"relationshipToPrisoner\": \"MOT\", \"isNextOfKin\": false, \"isEmergencyContact\": false}, \"createdBy\": \"USER\"}",
      "contactId must not be null;{\"relationship\": {\"prisonerNumber\": \"A1324BC\", \"relationshipType\": \"S\", \"relationshipToPrisoner\": \"MOT\", \"isNextOfKin\": false, \"isEmergencyContact\": false}, \"createdBy\": \"USER\"}",
      "createdBy must not be null;{\"contactId\": 99, \"relationship\": {\"prisonerNumber\": \"A1324BC\", \"relationshipType\": \"S\", \"relationshipToPrisoner\": \"MOT\", \"isNextOfKin\": false, \"isEmergencyContact\": false}, \"createdBy\": null}",
      "createdBy must not be null;{\"contactId\": 99, \"relationship\": {\"prisonerNumber\": \"A1324BC\", \"relationshipType\": \"S\", \"relationshipToPrisoner\": \"MOT\", \"isNextOfKin\": false, \"isEmergencyContact\": false}}",
      "relationship.prisonerNumber must not be null;{\"contactId\": 99, \"relationship\": {\"prisonerNumber\": null, \"relationshipType\": \"S\", \"relationshipToPrisoner\": \"MOT\", \"isNextOfKin\": false, \"isEmergencyContact\": false}, \"createdBy\": \"USER\"}",
      "relationship.prisonerNumber must not be null;{\"contactId\": 99, \"relationship\": {\"relationshipType\": \"S\", \"relationshipToPrisoner\": \"MOT\", \"isNextOfKin\": false, \"isEmergencyContact\": false}, \"createdBy\": \"USER\"}",
      "relationship.relationshipType must not be null;{\"contactId\": 99, \"relationship\": {\"prisonerNumber\": \"A1324BC\", \"relationshipToPrisoner\": \"MOT\", \"isNextOfKin\": false, \"isEmergencyContact\": false}, \"createdBy\": \"USER\"}",
      "relationship.relationshipType must not be null;{\"contactId\": 99, \"relationship\": {\"prisonerNumber\": \"A1324BC\", \"relationshipType\":null, \"relationshipToPrisoner\": \"MOT\", \"isNextOfKin\": false, \"isEmergencyContact\": false}, \"createdBy\": \"USER\"}",
      "relationship.relationshipToPrisoner must not be null;{\"contactId\": 99, \"relationship\": {\"prisonerNumber\": \"A1324BC\", \"relationshipType\": \"S\", \"relationshipToPrisoner\": null, \"isNextOfKin\": false, \"isEmergencyContact\": false}, \"createdBy\": \"USER\"}",
      "relationship.relationshipToPrisoner must not be null;{\"contactId\": 99, \"relationship\": {\"prisonerNumber\": \"A1324BC\", \"relationshipType\": \"S\", \"isNextOfKin\": false, \"isEmergencyContact\": false}, \"createdBy\": \"USER\"}",
      "relationship.isNextOfKin must not be null;{\"contactId\": 99, \"relationship\": {\"prisonerNumber\": \"A1324BC\", \"relationshipType\": \"S\", \"relationshipToPrisoner\": \"MOT\", \"isEmergencyContact\": false}, \"createdBy\": \"USER\"}",
      "relationship.isEmergencyContact must not be null;{\"contactId\": 99, \"relationship\": {\"prisonerNumber\": \"A1324BC\", \"relationshipType\": \"S\", \"relationshipToPrisoner\": \"MOT\", \"isNextOfKin\": false}, \"createdBy\": \"USER\"}",
    ],
    delimiter = ';',
  )
  fun `should return bad request if required fields are null`(expectedMessage: String, json: String) {
    val errors = webTestClient.post()
      .uri("/prisoner-contact")
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
  }

  @Test
  fun `Should return 404 if the prisoner can't be found`() {
    val errors = webTestClient.post()
      .uri("/prisoner-contact")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CONTACTS_ADMIN")))
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
      .headers(setAuthorisation(roles = listOf("ROLE_CONTACTS_ADMIN")))
      .bodyValue(aMinimalRequest().copy(contactId = 123456789))
      .exchange()
      .expectStatus()
      .isNotFound
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody!!

    assertThat(errors.userMessage).isEqualTo("Entity not found : Contact (123456789) could not be found")
  }

  @ParameterizedTest
  @ValueSource(strings = ["ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__RW"])
  fun `should create the contact relationship with minimal fields`(role: String) {
    stubPrisonSearchWithResponse("A1234BC")

    val request = AddContactRelationshipRequest(
      contactId = contact.id,
      relationship = ContactRelationship(
        prisonerNumber = "A1234BC",
        relationshipToPrisoner = "MOT",
        isNextOfKin = true,
        relationshipType = "S",
        isEmergencyContact = false,
      ),
      createdBy = "USER",
    )

    val createdRelationship = testAPIClient.addAContactRelationship(request, role)

    assertThat(createdRelationship.relationshipToPrisonerCode).isEqualTo("MOT")
    assertThat(createdRelationship.relationshipToPrisonerDescription).isEqualTo("Mother")
    assertThat(createdRelationship.nextOfKin).isTrue()
    assertThat(createdRelationship.emergencyContact).isFalse()
    assertThat(createdRelationship.comments).isNull()

    stubEvents.assertHasEvent(
      event = OutboundEvent.PRISONER_CONTACT_CREATED,
      additionalInfo = PrisonerContactInfo(createdRelationship.prisonerContactId, source = Source.DPS),
      personReference = PersonReference(dpsContactId = contact.id, nomsNumber = request.relationship.prisonerNumber),
    )
  }

  @Test
  fun `should create the contact with all fields`() {
    stubPrisonSearchWithResponse("A1234BC")

    val request = AddContactRelationshipRequest(
      contactId = contact.id,
      relationship = ContactRelationship(
        prisonerNumber = "A1234BC",
        relationshipToPrisoner = "MOT",
        isNextOfKin = false,
        relationshipType = "S",
        isEmergencyContact = true,
        comments = "Some comments",
      ),
      createdBy = "USER",
    )

    val createdRelationship = testAPIClient.addAContactRelationship(request)
    assertThat(createdRelationship.relationshipToPrisonerCode).isEqualTo("MOT")
    assertThat(createdRelationship.relationshipToPrisonerDescription).isEqualTo("Mother")
    assertThat(createdRelationship.nextOfKin).isFalse()
    assertThat(createdRelationship.emergencyContact).isTrue()
    assertThat(createdRelationship.comments).isEqualTo("Some comments")

    stubEvents.assertHasEvent(
      event = OutboundEvent.PRISONER_CONTACT_CREATED,
      additionalInfo = PrisonerContactInfo(createdRelationship.prisonerContactId, source = Source.DPS),
      personReference = PersonReference(dpsContactId = contact.id, nomsNumber = request.relationship.prisonerNumber),
    )
  }

  private fun aMinimalRequest() = AddContactRelationshipRequest(
    contactId = contact.id,
    relationship = ContactRelationship(
      prisonerNumber = "A1234BC",
      relationshipToPrisoner = "MOT",
      isNextOfKin = true,
      relationshipType = "S",
      isEmergencyContact = false,
    ),
    createdBy = "USER",
  )
}
