package uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.openapitools.jackson.nullable.JsonNullable
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.util.UriComponentsBuilder
import uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.SecureAPIIntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.CreateContactRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.PatchContactRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.PatchContactResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.ContactRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.ContactInfo
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.PersonReference
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.Source
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.time.LocalDate

class PatchContactIntegrationTest : SecureAPIIntegrationTestBase() {

  private val contactId = 21L
  private val updatedByUser = "JD000001"

  override val allowedRoles: Set<String> = setOf("ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__RW")

  @Autowired
  lateinit var contactRepository: ContactRepository

  override fun baseRequestBuilder(): WebTestClient.RequestHeadersSpec<*> = webTestClient.patch()
    .uri("/contact/123456")
    .accept(MediaType.APPLICATION_JSON)
    .contentType(MediaType.APPLICATION_JSON)
    .bodyValue(PatchContactRequest(updatedBy = updatedByUser))

  @Nested
  inner class ErrorScenarios {

    @Test
    fun ` should return bad request when request is empty`() {
      webTestClient.patch()
        .uri("/contact/$contactId")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_CONTACTS_ADMIN")))
        .bodyValue(
          """{
                  }""",
        )
        .exchange()
        .expectStatus()
        .isBadRequest
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody(ErrorResponse::class.java)
        .returnResult().responseBody!!
    }

    private fun aPatchContactRequest() = PatchContactRequest(
      languageCode = JsonNullable.of("BEN"),
      updatedBy = updatedByUser,
    )
  }

  @Nested
  inner class LanguageCode {

    @ParameterizedTest
    @ValueSource(strings = ["ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__RW"])
    fun `should not patch the language code when not provided`(role: String) {
      resetLanguageCode()

      val req = PatchContactRequest(
        updatedBy = updatedByUser,
      )

      val res = testAPIClient.patchAContact(req, "/contact/$contactId", role)

      assertThat(res.languageCode).isEqualTo("ENG")
      assertThat(res.updatedBy).isEqualTo(updatedByUser)

      stubEvents.assertHasEvent(
        event = OutboundEvent.CONTACT_UPDATED,
        additionalInfo = ContactInfo(contactId, Source.DPS),
        personReference = PersonReference(dpsContactId = contactId),
      )
    }

    @Test
    fun `should successfully patch the language code with null value`() {
      resetLanguageCode()

      val req = PatchContactRequest(
        languageCode = JsonNullable.of(null),
        updatedBy = updatedByUser,
      )

      val res = testAPIClient.patchAContact(req, "/contact/$contactId")

      assertThat(res.languageCode).isEqualTo(null)
      assertThat(res.updatedBy).isEqualTo(updatedByUser)

      stubEvents.assertHasEvent(
        event = OutboundEvent.CONTACT_UPDATED,
        additionalInfo = ContactInfo(contactId, Source.DPS),
        personReference = PersonReference(dpsContactId = contactId),
      )
    }

    @Test
    fun `should successfully patch the language code with a value`() {
      resetLanguageCode()

      val req = PatchContactRequest(
        languageCode = JsonNullable.of("FRE-FRA"),
        updatedBy = updatedByUser,
      )

      val res = testAPIClient.patchAContact(req, "/contact/$contactId")

      assertThat(res.languageCode).isEqualTo("FRE-FRA")
      assertThat(res.updatedBy).isEqualTo(updatedByUser)

      stubEvents.assertHasEvent(
        event = OutboundEvent.CONTACT_UPDATED,
        additionalInfo = ContactInfo(contactId, Source.DPS),
        personReference = PersonReference(dpsContactId = contactId),
      )
    }

    private fun resetLanguageCode() {
      val req = PatchContactRequest(
        languageCode = JsonNullable.of("ENG"),
        updatedBy = updatedByUser,
      )

      val res = testAPIClient.patchAContact(req, "/contact/$contactId")

      assertThat(res.languageCode).isEqualTo("ENG")
      assertThat(res.updatedBy).isEqualTo(updatedByUser)

      stubEvents.reset()
    }
  }

  @Nested
  inner class InterpreterRequired {

    @Test
    fun `should successfully patch the interpreter required with true`() {
      resetInterpreterRequired(false)

      val req = PatchContactRequest(
        interpreterRequired = JsonNullable.of(true),
        updatedBy = updatedByUser,
      )

      val res = testAPIClient.patchAContact(req, "/contact/$contactId")

      assertThat(res.interpreterRequired).isEqualTo(true)
      assertThat(res.updatedBy).isEqualTo(updatedByUser)

      stubEvents.assertHasEvent(
        event = OutboundEvent.CONTACT_UPDATED,
        additionalInfo = ContactInfo(contactId, Source.DPS),
        personReference = PersonReference(dpsContactId = contactId),
      )
    }

    @Test
    fun `should not patch the interpreter required when not provided`() {
      resetInterpreterRequired(true)

      val req = PatchContactRequest(
        updatedBy = updatedByUser,
      )

      val res = testAPIClient.patchAContact(req, "/contact/$contactId")

      assertThat(res.interpreterRequired).isEqualTo(true)
      assertThat(res.updatedBy).isEqualTo(updatedByUser)

      stubEvents.assertHasEvent(
        event = OutboundEvent.CONTACT_UPDATED,
        additionalInfo = ContactInfo(contactId, Source.DPS),
        personReference = PersonReference(dpsContactId = contactId),
      )
    }

    @Test
    fun `should not patch the interpreter required with null value`() {
      resetInterpreterRequired(true)

      val req = PatchContactRequest(
        interpreterRequired = JsonNullable.of(null),
        updatedBy = updatedByUser,
      )
      val uri = UriComponentsBuilder.fromPath("/contact/$contactId")
        .build()
        .toUri()

      val errors = testAPIClient.getBadResponseErrorsWithPatch(req, uri)

      assertThat(errors.userMessage).isEqualTo("Validation failure: Unsupported interpreter required type null.")

      stubEvents.assertHasNoEvents(OutboundEvent.CONTACT_UPDATED, ContactInfo(contactId, Source.DPS))
    }

    private fun resetInterpreterRequired(resetValue: Boolean) {
      val req = PatchContactRequest(
        interpreterRequired = JsonNullable.of(resetValue),
        updatedBy = updatedByUser,
      )
      val res =
        testAPIClient.patchAContact(req, "/contact/$contactId")

      assertThat(res.interpreterRequired).isEqualTo(resetValue)
      assertThat(res.updatedBy).isEqualTo(updatedByUser)

      stubEvents.reset()
    }
  }

  @Nested
  inner class DomesticStatus {

    @Test
    fun `should not patch the domestic status code when not provided`() {
      resetDomesticStatus()

      val req = PatchContactRequest(
        updatedBy = updatedByUser,
      )

      val res = testAPIClient.patchAContact(req, "/contact/$contactId")

      assertThat(res.domesticStatus).isEqualTo("P")
      assertThat(res.updatedBy).isEqualTo(updatedByUser)

      stubEvents.assertHasEvent(
        event = OutboundEvent.CONTACT_UPDATED,
        additionalInfo = ContactInfo(contactId, Source.DPS),
        personReference = PersonReference(dpsContactId = contactId),
      )
    }

    @Test
    fun `should successfully patch the domestic status code with null value`() {
      resetDomesticStatus()

      val req = PatchContactRequest(
        domesticStatus = JsonNullable.of(null),
        updatedBy = updatedByUser,
      )
      val res = testAPIClient.patchAContact(req, "/contact/$contactId")

      assertThat(res.domesticStatus).isEqualTo(null)
      assertThat(res.updatedBy).isEqualTo(updatedByUser)

      stubEvents.assertHasEvent(
        event = OutboundEvent.CONTACT_UPDATED,
        additionalInfo = ContactInfo(contactId, Source.DPS),
        personReference = PersonReference(dpsContactId = contactId),
      )
    }

    @Test
    fun `should successfully patch the domestic status code with a value`() {
      resetDomesticStatus()

      val req = PatchContactRequest(
        domesticStatus = JsonNullable.of("M"),
        updatedBy = updatedByUser,
      )

      val res = testAPIClient.patchAContact(req, "/contact/$contactId")

      assertThat(res.domesticStatus).isEqualTo("M")
      assertThat(res.updatedBy).isEqualTo(updatedByUser)

      stubEvents.assertHasEvent(
        event = OutboundEvent.CONTACT_UPDATED,
        additionalInfo = ContactInfo(contactId, Source.DPS),
        personReference = PersonReference(dpsContactId = contactId),
      )
    }

    private fun resetDomesticStatus() {
      val req = PatchContactRequest(
        domesticStatus = JsonNullable.of("P"),
        updatedBy = updatedByUser,
      )

      val res = testAPIClient.patchAContact(req, "/contact/$contactId")

      assertThat(res.domesticStatus).isEqualTo("P")
      assertThat(res.updatedBy).isEqualTo(updatedByUser)

      stubEvents.reset()
    }
  }

  @Nested
  inner class StaffFlag {

    @Test
    fun `should successfully patch the staff flag with true`() {
      resetStaffFlag(false)

      val req = PatchContactRequest(
        isStaff = JsonNullable.of(true),
        updatedBy = updatedByUser,
      )

      val res = testAPIClient.patchAContact(req, "/contact/$contactId")

      assertThat(res.isStaff).isEqualTo(true)
      assertThat(res.updatedBy).isEqualTo(updatedByUser)

      stubEvents.assertHasEvent(
        event = OutboundEvent.CONTACT_UPDATED,
        additionalInfo = ContactInfo(contactId, Source.DPS),
        personReference = PersonReference(dpsContactId = contactId),
      )
    }

    @Test
    fun `should not patch the staff flag when not provided`() {
      resetStaffFlag(true)

      val req = PatchContactRequest(
        updatedBy = updatedByUser,
      )

      val res = testAPIClient.patchAContact(req, "/contact/$contactId")

      assertThat(res.isStaff).isEqualTo(true)
      assertThat(res.updatedBy).isEqualTo(updatedByUser)

      stubEvents.assertHasEvent(
        event = OutboundEvent.CONTACT_UPDATED,
        additionalInfo = ContactInfo(contactId, Source.DPS),
        personReference = PersonReference(dpsContactId = contactId),
      )
    }

    @Test
    fun `should not patch the staff flag with null value`() {
      resetStaffFlag(true)

      val req = PatchContactRequest(
        isStaff = JsonNullable.of(null),
        updatedBy = updatedByUser,
      )
      val uri = UriComponentsBuilder.fromPath("/contact/$contactId")
        .build()
        .toUri()

      val errors = testAPIClient.getBadResponseErrorsWithPatch(req, uri)

      assertThat(errors.userMessage).isEqualTo("Validation failure: Unsupported staff flag value null.")

      stubEvents.assertHasNoEvents(OutboundEvent.CONTACT_UPDATED, ContactInfo(contactId, Source.DPS))
    }

    private fun resetStaffFlag(resetValue: Boolean) {
      val req = PatchContactRequest(
        isStaff = JsonNullable.of(resetValue),
        updatedBy = updatedByUser,
      )

      val res = testAPIClient.patchAContact(req, "/contact/$contactId")

      assertThat(res.isStaff).isEqualTo(resetValue)
      assertThat(res.updatedBy).isEqualTo(updatedByUser)
      stubEvents.reset()
    }
  }

  @Nested
  inner class DateOfBirth {
    private var contactIdThatHasDOB = 0L

    @BeforeEach
    fun createContactWithDob() {
      contactIdThatHasDOB = testAPIClient.createAContact(
        CreateContactRequest(
          lastName = "Date of birth",
          firstName = "Has",
          dateOfBirth = LocalDate.of(1982, 6, 15),
          createdBy = "created",
        ),

      ).id
    }

    @Test
    fun `should not patch the date of birth when not provided`() {
      val req = PatchContactRequest(
        updatedBy = updatedByUser,
      )

      val res = testAPIClient.patchAContact(req, "/contact/$contactIdThatHasDOB")

      assertThat(res.dateOfBirth).isEqualTo(LocalDate.of(1982, 6, 15))
      assertThat(res.updatedBy).isEqualTo(updatedByUser)

      stubEvents.assertHasEvent(
        event = OutboundEvent.CONTACT_UPDATED,
        additionalInfo = ContactInfo(contactIdThatHasDOB, Source.DPS),
        personReference = PersonReference(dpsContactId = contactIdThatHasDOB),
      )
    }

    @Test
    fun `should successfully patch the date of birth with null value`() {
      val req = PatchContactRequest(
        dateOfBirth = JsonNullable.of(null),
        updatedBy = updatedByUser,
      )

      val res = testAPIClient.patchAContact(req, "/contact/$contactIdThatHasDOB")

      assertThat(res.dateOfBirth).isNull()
      assertThat(res.updatedBy).isEqualTo(updatedByUser)

      stubEvents.assertHasEvent(
        event = OutboundEvent.CONTACT_UPDATED,
        additionalInfo = ContactInfo(contactIdThatHasDOB, Source.DPS),
        personReference = PersonReference(dpsContactId = contactIdThatHasDOB),
      )
    }

    @Test
    fun `should successfully patch the date of birth with a value`() {
      val req = PatchContactRequest(
        dateOfBirth = JsonNullable.of(LocalDate.of(2000, 12, 25)),
        updatedBy = updatedByUser,
      )

      val res = testAPIClient.patchAContact(req, "/contact/$contactIdThatHasDOB")

      assertThat(res.dateOfBirth).isEqualTo(LocalDate.of(2000, 12, 25))
      assertThat(res.updatedBy).isEqualTo(updatedByUser)

      stubEvents.assertHasEvent(
        event = OutboundEvent.CONTACT_UPDATED,
        additionalInfo = ContactInfo(contactIdThatHasDOB, Source.DPS),
        personReference = PersonReference(dpsContactId = contactIdThatHasDOB),
      )
    }
  }

  @Nested
  inner class PatchNames {
    private var contactThatHasAllNameFields = 0L

    @BeforeEach
    fun createContactWithDob() {
      contactThatHasAllNameFields = testAPIClient.createAContact(
        CreateContactRequest(
          lastName = "Last",
          firstName = "First",
          middleNames = "Middle Names",
          title = "MR",
          createdBy = "created",
        ),

      ).id
    }

    @Test
    fun `should not patch names when not provided`() {
      val req = PatchContactRequest(
        updatedBy = updatedByUser,
      )
      val res = testAPIClient.patchAContact(req, "/contact/$contactThatHasAllNameFields")

      assertThat(res.firstName).isEqualTo("First")
      assertThat(res.lastName).isEqualTo("Last")
      assertThat(res.middleNames).isEqualTo("Middle Names")
      assertThat(res.title).isEqualTo("MR")
      assertThat(res.updatedBy).isEqualTo(updatedByUser)

      stubEvents.assertHasEvent(
        event = OutboundEvent.CONTACT_UPDATED,
        additionalInfo = ContactInfo(contactThatHasAllNameFields, Source.DPS),
        personReference = PersonReference(dpsContactId = contactThatHasAllNameFields),
      )
    }

    @Test
    fun `should not patch first or last names`() {
      val res = webTestClient.patch()
        .uri("/contact/$contactThatHasAllNameFields")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_CONTACTS_ADMIN")))
        .bodyValue(
          """
          { "firstName": "update first", "lastName": "update last", "middleNames": "update middle", "title": "MRS", "updatedBy": "$updatedByUser" }
          """.trimIndent(),
        )
        .exchange()
        .expectStatus()
        .isOk
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody(PatchContactResponse::class.java)
        .returnResult().responseBody!!

      assertThat(res.firstName).isEqualTo("First")
      assertThat(res.lastName).isEqualTo("Last")
      assertThat(res.middleNames).isEqualTo("update middle")
      assertThat(res.title).isEqualTo("MRS")
      assertThat(res.updatedBy).isEqualTo(updatedByUser)

      stubEvents.assertHasEvent(
        event = OutboundEvent.CONTACT_UPDATED,
        additionalInfo = ContactInfo(contactThatHasAllNameFields, Source.DPS),
        personReference = PersonReference(dpsContactId = contactThatHasAllNameFields),
      )
    }

    @Test
    fun `should successfully patch middle name and title with null values`() {
      val req = PatchContactRequest(
        title = JsonNullable.of(null),
        middleNames = JsonNullable.of(null),
        updatedBy = updatedByUser,
      )
      val res = testAPIClient.patchAContact(req, "/contact/$contactThatHasAllNameFields")

      assertThat(res.firstName).isEqualTo("First")
      assertThat(res.lastName).isEqualTo("Last")
      assertThat(res.middleNames).isNull()
      assertThat(res.title).isNull()
      assertThat(res.updatedBy).isEqualTo(updatedByUser)

      stubEvents.assertHasEvent(
        event = OutboundEvent.CONTACT_UPDATED,
        additionalInfo = ContactInfo(contactThatHasAllNameFields, Source.DPS),
        personReference = PersonReference(dpsContactId = contactThatHasAllNameFields),
      )
    }

    @Test
    fun `should successfully patch middle name and title with a value`() {
      val req = PatchContactRequest(
        title = JsonNullable.of("MRS"),
        middleNames = JsonNullable.of("Updated Middle"),
        updatedBy = updatedByUser,
      )
      val res = testAPIClient.patchAContact(req, "/contact/$contactThatHasAllNameFields")

      assertThat(res.firstName).isEqualTo("First")
      assertThat(res.lastName).isEqualTo("Last")
      assertThat(res.middleNames).isEqualTo("Updated Middle")
      assertThat(res.title).isEqualTo("MRS")
      assertThat(res.updatedBy).isEqualTo(updatedByUser)

      stubEvents.assertHasEvent(
        event = OutboundEvent.CONTACT_UPDATED,
        additionalInfo = ContactInfo(contactThatHasAllNameFields, Source.DPS),
        personReference = PersonReference(dpsContactId = contactThatHasAllNameFields),
      )
    }

    @Test
    fun `should not be able to patch to an invalid title value`() {
      val req = PatchContactRequest(
        title = JsonNullable.of("FOO"),
        updatedBy = updatedByUser,
      )

      val uri = UriComponentsBuilder.fromPath("/contact/$contactThatHasAllNameFields")
        .build()
        .toUri()
      val errors = testAPIClient.getBadResponseErrorsWithPatch(req, uri)
      assertThat(errors.userMessage).isEqualTo("Validation failure: Unsupported title (FOO)")

      stubEvents.assertHasNoEvents(
        event = OutboundEvent.CONTACT_UPDATED,
        additionalInfo = ContactInfo(contactThatHasAllNameFields, Source.DPS),
      )
    }

    @Test
    fun `should not be able to patch middle name when it's too long`() {
      val req = PatchContactRequest(
        middleNames = JsonNullable.of("".padEnd(36, 'X')),
        updatedBy = updatedByUser,
      )

      val uri = UriComponentsBuilder.fromPath("/contact/$contactThatHasAllNameFields")
        .build()
        .toUri()
      val errors = testAPIClient.getBadResponseErrorsWithPatch(req, uri)
      assertThat(errors.userMessage).isEqualTo("Validation failure(s): middleNames must be <= 35 characters")

      stubEvents.assertHasNoEvents(
        event = OutboundEvent.CONTACT_UPDATED,
        additionalInfo = ContactInfo(contactThatHasAllNameFields, Source.DPS),
      )
    }
  }

  @Nested
  inner class PatchGender {
    private var contactWithAGender = 0L

    @BeforeEach
    fun createContactWithDob() {
      contactWithAGender = testAPIClient.createAContact(
        CreateContactRequest(
          lastName = "Last",
          firstName = "First",
          createdBy = "created",
        ),

      ).id
      val entity = contactRepository.findById(contactWithAGender).get()
      contactRepository.saveAndFlush(entity.copy(gender = "NS"))
    }

    @Test
    fun `should not patch gender if undefined`() {
      val req = PatchContactRequest(
        updatedBy = updatedByUser,
      )
      val res = testAPIClient.patchAContact(req, "/contact/$contactWithAGender")

      assertThat(res.gender).isEqualTo("NS")
      assertThat(res.updatedBy).isEqualTo(updatedByUser)

      stubEvents.assertHasEvent(
        event = OutboundEvent.CONTACT_UPDATED,
        additionalInfo = ContactInfo(contactWithAGender, Source.DPS),
        personReference = PersonReference(dpsContactId = contactWithAGender),
      )
    }

    @Test
    fun `should successfully patch gender with null values`() {
      val req = PatchContactRequest(
        gender = JsonNullable.of(null),
        updatedBy = updatedByUser,
      )
      val res = testAPIClient.patchAContact(req, "/contact/$contactWithAGender")

      assertThat(res.gender).isNull()
      assertThat(res.updatedBy).isEqualTo(updatedByUser)

      stubEvents.assertHasEvent(
        event = OutboundEvent.CONTACT_UPDATED,
        additionalInfo = ContactInfo(contactWithAGender, Source.DPS),
        personReference = PersonReference(dpsContactId = contactWithAGender),
      )
    }

    @Test
    fun `should successfully patch gender with a value`() {
      val req = PatchContactRequest(
        gender = JsonNullable.of("M"),
        updatedBy = updatedByUser,
      )
      val res = testAPIClient.patchAContact(req, "/contact/$contactWithAGender")

      assertThat(res.gender).isEqualTo("M")
      assertThat(res.updatedBy).isEqualTo(updatedByUser)

      stubEvents.assertHasEvent(
        event = OutboundEvent.CONTACT_UPDATED,
        additionalInfo = ContactInfo(contactWithAGender, Source.DPS),
        personReference = PersonReference(dpsContactId = contactWithAGender),
      )
    }

    @Test
    fun `should not be able to patch to an invalid gender value`() {
      val req = PatchContactRequest(
        gender = JsonNullable.of("FOO"),
        updatedBy = updatedByUser,
      )

      val uri = UriComponentsBuilder.fromPath("/contact/$contactWithAGender")
        .build()
        .toUri()
      val errors = testAPIClient.getBadResponseErrorsWithPatch(req, uri)
      assertThat(errors.userMessage).isEqualTo("Validation failure: Unsupported gender (FOO)")

      stubEvents.assertHasNoEvents(OutboundEvent.CONTACT_UPDATED, ContactInfo(contactWithAGender, Source.DPS))
    }
  }
}
