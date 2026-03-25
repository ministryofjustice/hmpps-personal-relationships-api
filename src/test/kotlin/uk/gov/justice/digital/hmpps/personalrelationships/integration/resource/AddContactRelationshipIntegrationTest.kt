package uk.gov.justice.digital.hmpps.personalrelationships.integration.resource

import org.apache.commons.lang3.RandomStringUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.personalrelationships.config.User
import uk.gov.justice.digital.hmpps.personalrelationships.integration.SecureAPIIntegrationTestBase
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.AddContactRelationshipRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.ContactRelationship
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.CreateContactRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.ContactDetails
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.PrisonerContactRelationshipDetails
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.PersonReference
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.PrisonerContactInfo
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.Source
import uk.gov.justice.digital.hmpps.personalrelationships.util.StubUser
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
        isEmergencyContact = true,
        isApprovedVisitor = true,
      ),
    )

    val createdRelationship = testAPIClient.addAContactRelationship(request)

    assertThat(createdRelationship.relationshipToPrisonerCode).isEqualTo("MOT")
    assertThat(createdRelationship.relationshipToPrisonerDescription).isEqualTo("Mother")
    assertThat(createdRelationship.isNextOfKin).isTrue()
    assertThat(createdRelationship.isEmergencyContact).isTrue()
    assertThat(createdRelationship.isApprovedVisitor).isTrue()
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

    assertCustomEvent(createdRelationship, Source.DPS, User("read_write_user", "BXI"))
    // next of kin, emergency contact and approved visitor are true so an event needs to be created for each
    assertNextOfKinCustomCreatedEvent(createdRelationship, Source.DPS, User("read_write_user", "BXI"))
    assertApprovedVisitorCustomCreatedEvent(createdRelationship, Source.DPS, User("read_write_user", "BXI"))
    assertEmergencyContactCustomCreatedEvent(createdRelationship, Source.DPS, User("read_write_user", "BXI"))
  }

  @Test
  fun `when next of kin, approved visitor and emergency contact are false, only 1 event should be created`() {
    stubPrisonSearchWithResponse("A1234BC")

    val request = AddContactRelationshipRequest(
      contactId = contact.id,
      relationship = ContactRelationship(
        prisonerNumber = "A1234BC",
        relationshipToPrisonerCode = "MOT",
        isNextOfKin = false,
        relationshipTypeCode = "S",
        isEmergencyContact = false,
        isApprovedVisitor = false,
      ),
    )

    val createdRelationship = testAPIClient.addAContactRelationship(request)

    assertThat(createdRelationship.relationshipToPrisonerCode).isEqualTo("MOT")
    assertThat(createdRelationship.relationshipToPrisonerDescription).isEqualTo("Mother")
    assertThat(createdRelationship.isNextOfKin).isFalse()
    assertThat(createdRelationship.isEmergencyContact).isFalse()
    assertThat(createdRelationship.isApprovedVisitor).isFalse()
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

    assertCustomEvent(createdRelationship, Source.DPS, User("read_write_user", "BXI"))
    verify(telemetryClient, never()).trackEvent(
      eq("contact-next-of-kin-created"),
      any<Map<String, String>>(),
      anyOrNull(),
    )
    verify(telemetryClient, never()).trackEvent(
      eq("contact-emergency-contact-created"),
      any<Map<String, String>>(),
      anyOrNull(),
    )
    verify(telemetryClient, never()).trackEvent(
      eq("contact-approved-visitor-created"),
      any<Map<String, String>>(),
      anyOrNull(),
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

    assertCustomEvent(createdRelationship, Source.DPS, User("read_write_user", "BXI"))
    verify(telemetryClient, never()).trackEvent(
      eq("contact-next-of-kin-created"),
      any<Map<String, String>>(),
      anyOrNull(),
    )
    assertApprovedVisitorCustomCreatedEvent(createdRelationship, Source.DPS, User("read_write_user", "BXI"))
    assertEmergencyContactCustomCreatedEvent(createdRelationship, Source.DPS, User("read_write_user", "BXI"))
  }

  private fun assertCustomEvent(contactRelationship: PrisonerContactRelationshipDetails, source: Source, user: User) {
    verify(telemetryContactCustomEventService, times(1)).trackCreatePrisonerContactEvent(contactRelationship, source, user)

    verify(telemetryClient, times(1)).trackEvent(
      "prisoner-contact-created",
      mapOf(
        "description" to "A prisoner contact has been created",
        "source" to source.name,
        "username" to user.username,
        "active_caseload_id" to user.activeCaseLoadId,
        "contactId" to contactRelationship.contactId.toString(),
        "prisoner_contact_id" to contactRelationship.prisonerContactId.toString(),
        "prisoner_number" to contactRelationship.prisonerNumber,
      ),
      null,
    )
  }

  private fun assertNextOfKinCustomCreatedEvent(contactRelationship: PrisonerContactRelationshipDetails, source: Source, user: User) {
    verify(telemetryClient, times(1)).trackEvent(
      "contact-next-of-kin-created",
      mapOf(
        "description" to "A contact next of kin has been created",
        "source" to source.name,
        "username" to user.username,
        "contactId" to contactRelationship.contactId.toString(),
        "active_caseload_id" to user.activeCaseLoadId,
        "prisoner_contact_id" to contactRelationship.prisonerContactId.toString(),
      ),
      null,
    )
  }

  private fun assertEmergencyContactCustomCreatedEvent(contactRelationship: PrisonerContactRelationshipDetails, source: Source, user: User) {
    verify(telemetryClient, times(1)).trackEvent(
      "contact-emergency-contact-created",
      mapOf(
        "description" to "A contact emergency contact has been created",
        "source" to source.name,
        "username" to user.username,
        "contactId" to contactRelationship.contactId.toString(),
        "active_caseload_id" to user.activeCaseLoadId,
        "prisoner_contact_id" to contactRelationship.prisonerContactId.toString(),
      ),
      null,
    )
  }

  private fun assertApprovedVisitorCustomCreatedEvent(contactRelationship: PrisonerContactRelationshipDetails, source: Source, user: User) {
    verify(telemetryClient, times(1)).trackEvent(
      "contact-approved-visitor-created",
      mapOf(
        "description" to "A contact approved visitor has been created",
        "source" to source.name,
        "username" to user.username,
        "contactId" to contactRelationship.contactId.toString(),
        "active_caseload_id" to user.activeCaseLoadId,
        "prisoner_contact_id" to contactRelationship.prisonerContactId.toString(),
      ),
      null,
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
