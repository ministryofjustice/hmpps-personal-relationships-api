package uk.gov.justice.digital.hmpps.personalrelationships.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.openapitools.jackson.nullable.JsonNullable
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.util.UriComponentsBuilder
import uk.gov.justice.digital.hmpps.personalrelationships.integration.SecureAPIIntegrationTestBase
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.CreateContactRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.PatchContactRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.PatchContactResponse
import uk.gov.justice.digital.hmpps.personalrelationships.repository.ContactRepository
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.ContactInfo
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.PersonReference
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.Source
import uk.gov.justice.digital.hmpps.personalrelationships.util.StubUser
import java.time.LocalDate

class PatchContactIntegrationTest : SecureAPIIntegrationTestBase() {

  private val contactId = 21L
  private val updatedByUser = "read_write_user"

  override val allowedRoles: Set<String> = setOf("ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__RW")

  @Autowired
  lateinit var contactRepository: ContactRepository

  @BeforeEach
  fun setUp() {
    setCurrentUser(StubUser.READ_WRITE_USER)
  }

  override fun baseRequestBuilder(): WebTestClient.RequestHeadersSpec<*> = webTestClient.patch()
    .uri("/contact/123456")
    .accept(MediaType.APPLICATION_JSON)
    .contentType(MediaType.APPLICATION_JSON)
    .bodyValue(PatchContactRequest())

  @Nested
  inner class LanguageCode {

    @Test
    fun `should not patch the language code when not provided`() {
      resetLanguageCode()

      val req = PatchContactRequest()

      val res = testAPIClient.patchAContact(req, "/contact/$contactId")

      assertThat(res.languageCode).isEqualTo("ENG")
      assertThat(res.updatedBy).isEqualTo(updatedByUser)

      stubEvents.assertHasEvent(
        event = OutboundEvent.CONTACT_UPDATED,
        additionalInfo = ContactInfo(contactId, Source.DPS, "read_write_user", "BXI"),
        personReference = PersonReference(dpsContactId = contactId),
      )
    }

    @Test
    fun `should successfully patch the language code with null value`() {
      resetLanguageCode()

      val req = PatchContactRequest(
        languageCode = JsonNullable.of(null),
      )

      val res = testAPIClient.patchAContact(req, "/contact/$contactId")

      assertThat(res.languageCode).isEqualTo(null)
      assertThat(res.updatedBy).isEqualTo(updatedByUser)

      stubEvents.assertHasEvent(
        event = OutboundEvent.CONTACT_UPDATED,
        additionalInfo = ContactInfo(contactId, Source.DPS, "read_write_user", "BXI"),
        personReference = PersonReference(dpsContactId = contactId),
      )
    }

    @Test
    fun `should not patch the language code with an invalid value`() {
      val req = PatchContactRequest(
        languageCode = JsonNullable.of("FOO"),
      )

      val uri = UriComponentsBuilder.fromPath("/contact/$contactId")
        .build()
        .toUri()
      val errors = testAPIClient.getBadResponseErrorsWithPatch(req, uri)
      assertThat(errors.userMessage).isEqualTo("Validation failure: Unsupported language (FOO)")

      stubEvents.assertHasNoEvents(OutboundEvent.CONTACT_UPDATED, ContactInfo(contactId, Source.DPS, "read_write_user", "BXI"))
    }

    @Test
    fun `should successfully patch the language code with a value`() {
      resetLanguageCode()

      val req = PatchContactRequest(
        languageCode = JsonNullable.of("FRE-FRA"),
      )

      val res = testAPIClient.patchAContact(req, "/contact/$contactId")

      assertThat(res.languageCode).isEqualTo("FRE-FRA")
      assertThat(res.updatedBy).isEqualTo(updatedByUser)

      stubEvents.assertHasEvent(
        event = OutboundEvent.CONTACT_UPDATED,
        additionalInfo = ContactInfo(contactId, Source.DPS, "read_write_user", "BXI"),
        personReference = PersonReference(dpsContactId = contactId),
      )
    }

    private fun resetLanguageCode() {
      val req = PatchContactRequest(
        languageCode = JsonNullable.of("ENG"),
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
      )

      val res = testAPIClient.patchAContact(req, "/contact/$contactId")

      assertThat(res.interpreterRequired).isEqualTo(true)
      assertThat(res.updatedBy).isEqualTo(updatedByUser)

      stubEvents.assertHasEvent(
        event = OutboundEvent.CONTACT_UPDATED,
        additionalInfo = ContactInfo(contactId, Source.DPS, "read_write_user", "BXI"),
        personReference = PersonReference(dpsContactId = contactId),
      )
    }

    @Test
    fun `should not patch the interpreter required when not provided`() {
      resetInterpreterRequired(true)

      val req = PatchContactRequest()

      val res = testAPIClient.patchAContact(req, "/contact/$contactId")

      assertThat(res.interpreterRequired).isEqualTo(true)
      assertThat(res.updatedBy).isEqualTo(updatedByUser)

      stubEvents.assertHasEvent(
        event = OutboundEvent.CONTACT_UPDATED,
        additionalInfo = ContactInfo(contactId, Source.DPS, "read_write_user", "BXI"),
        personReference = PersonReference(dpsContactId = contactId),
      )
    }

    @Test
    fun `should not patch the interpreter required with null value`() {
      resetInterpreterRequired(true)

      val req = PatchContactRequest(
        interpreterRequired = JsonNullable.of(null),
      )
      val uri = UriComponentsBuilder.fromPath("/contact/$contactId")
        .build()
        .toUri()

      val errors = testAPIClient.getBadResponseErrorsWithPatch(req, uri)

      assertThat(errors.userMessage).isEqualTo("Validation failure: Unsupported interpreter required type null.")

      stubEvents.assertHasNoEvents(OutboundEvent.CONTACT_UPDATED, ContactInfo(contactId, Source.DPS, "read_write_user", "BXI"))
    }

    private fun resetInterpreterRequired(resetValue: Boolean) {
      val req = PatchContactRequest(
        interpreterRequired = JsonNullable.of(resetValue),
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

      val req = PatchContactRequest()

      val res = testAPIClient.patchAContact(req, "/contact/$contactId")

      assertThat(res.domesticStatusCode).isEqualTo("P")
      assertThat(res.updatedBy).isEqualTo(updatedByUser)

      stubEvents.assertHasEvent(
        event = OutboundEvent.CONTACT_UPDATED,
        additionalInfo = ContactInfo(contactId, Source.DPS, "read_write_user", "BXI"),
        personReference = PersonReference(dpsContactId = contactId),
      )
    }

    @Test
    fun `should successfully patch the domestic status code with null value`() {
      resetDomesticStatus()

      val req = PatchContactRequest(
        domesticStatusCode = JsonNullable.of(null),
      )
      val res = testAPIClient.patchAContact(req, "/contact/$contactId")

      assertThat(res.domesticStatusCode).isEqualTo(null)
      assertThat(res.updatedBy).isEqualTo(updatedByUser)

      stubEvents.assertHasEvent(
        event = OutboundEvent.CONTACT_UPDATED,
        additionalInfo = ContactInfo(contactId, Source.DPS, "read_write_user", "BXI"),
        personReference = PersonReference(dpsContactId = contactId),
      )
    }

    @Test
    fun `should successfully patch the domestic status code with a value`() {
      resetDomesticStatus()

      val req = PatchContactRequest(
        domesticStatusCode = JsonNullable.of("M"),
      )

      val res = testAPIClient.patchAContact(req, "/contact/$contactId")

      assertThat(res.domesticStatusCode).isEqualTo("M")
      assertThat(res.updatedBy).isEqualTo(updatedByUser)

      stubEvents.assertHasEvent(
        event = OutboundEvent.CONTACT_UPDATED,
        additionalInfo = ContactInfo(contactId, Source.DPS, "read_write_user", "BXI"),
        personReference = PersonReference(dpsContactId = contactId),
      )
    }

    @Test
    fun `should not patch the domestic status code with an invalid value`() {
      val req = PatchContactRequest(
        domesticStatusCode = JsonNullable.of("FOO"),
      )

      val uri = UriComponentsBuilder.fromPath("/contact/$contactId")
        .build()
        .toUri()
      val errors = testAPIClient.getBadResponseErrorsWithPatch(req, uri)
      assertThat(errors.userMessage).isEqualTo("Validation failure: Unsupported domestic status (FOO)")

      stubEvents.assertHasNoEvents(OutboundEvent.CONTACT_UPDATED, ContactInfo(contactId, Source.DPS, "read_write_user", "BXI"))
    }

    private fun resetDomesticStatus() {
      val req = PatchContactRequest(
        domesticStatusCode = JsonNullable.of("P"),
      )

      val res = testAPIClient.patchAContact(req, "/contact/$contactId")

      assertThat(res.domesticStatusCode).isEqualTo("P")
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
      )

      val res = testAPIClient.patchAContact(req, "/contact/$contactId")

      assertThat(res.isStaff).isEqualTo(true)
      assertThat(res.updatedBy).isEqualTo(updatedByUser)

      stubEvents.assertHasEvent(
        event = OutboundEvent.CONTACT_UPDATED,
        additionalInfo = ContactInfo(contactId, Source.DPS, "read_write_user", "BXI"),
        personReference = PersonReference(dpsContactId = contactId),
      )
    }

    @Test
    fun `should not patch the staff flag when not provided`() {
      resetStaffFlag(true)

      val req = PatchContactRequest()

      val res = testAPIClient.patchAContact(req, "/contact/$contactId")

      assertThat(res.isStaff).isEqualTo(true)
      assertThat(res.updatedBy).isEqualTo(updatedByUser)

      stubEvents.assertHasEvent(
        event = OutboundEvent.CONTACT_UPDATED,
        additionalInfo = ContactInfo(contactId, Source.DPS, "read_write_user", "BXI"),
        personReference = PersonReference(dpsContactId = contactId),
      )
    }

    @Test
    fun `should not patch the staff flag with null value`() {
      resetStaffFlag(true)

      val req = PatchContactRequest(
        isStaff = JsonNullable.of(null),
      )
      val uri = UriComponentsBuilder.fromPath("/contact/$contactId")
        .build()
        .toUri()

      val errors = testAPIClient.getBadResponseErrorsWithPatch(req, uri)

      assertThat(errors.userMessage).isEqualTo("Validation failure: Unsupported staff flag value null.")

      stubEvents.assertHasNoEvents(OutboundEvent.CONTACT_UPDATED, ContactInfo(contactId, Source.DPS, "read_write_user", "BXI"))
    }

    private fun resetStaffFlag(resetValue: Boolean) {
      val req = PatchContactRequest(
        isStaff = JsonNullable.of(resetValue),
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
        ),

      ).id
    }

    @Test
    fun `should not patch the date of birth when not provided`() {
      val req = PatchContactRequest()

      val res = testAPIClient.patchAContact(req, "/contact/$contactIdThatHasDOB")

      assertThat(res.dateOfBirth).isEqualTo(LocalDate.of(1982, 6, 15))
      assertThat(res.updatedBy).isEqualTo(updatedByUser)

      stubEvents.assertHasEvent(
        event = OutboundEvent.CONTACT_UPDATED,
        additionalInfo = ContactInfo(contactIdThatHasDOB, Source.DPS, "read_write_user", "BXI"),
        personReference = PersonReference(dpsContactId = contactIdThatHasDOB),
      )
    }

    @Test
    fun `should successfully patch the date of birth with null value`() {
      val req = PatchContactRequest(
        dateOfBirth = JsonNullable.of(null),
      )

      val res = testAPIClient.patchAContact(req, "/contact/$contactIdThatHasDOB")

      assertThat(res.dateOfBirth).isNull()
      assertThat(res.updatedBy).isEqualTo(updatedByUser)

      stubEvents.assertHasEvent(
        event = OutboundEvent.CONTACT_UPDATED,
        additionalInfo = ContactInfo(contactIdThatHasDOB, Source.DPS, "read_write_user", "BXI"),
        personReference = PersonReference(dpsContactId = contactIdThatHasDOB),
      )
    }

    @Test
    fun `should successfully patch the date of birth with a value`() {
      val req = PatchContactRequest(
        dateOfBirth = JsonNullable.of(LocalDate.of(2000, 12, 25)),
      )

      val res = testAPIClient.patchAContact(req, "/contact/$contactIdThatHasDOB")

      assertThat(res.dateOfBirth).isEqualTo(LocalDate.of(2000, 12, 25))
      assertThat(res.updatedBy).isEqualTo(updatedByUser)

      stubEvents.assertHasEvent(
        event = OutboundEvent.CONTACT_UPDATED,
        additionalInfo = ContactInfo(contactIdThatHasDOB, Source.DPS, "read_write_user", "BXI"),
        personReference = PersonReference(dpsContactId = contactIdThatHasDOB),
      )
    }
  }

  @Nested
  inner class PatchNames {
    private var contactThatHasAllNameFields = 0L

    @BeforeEach
    fun createContact() {
      contactThatHasAllNameFields = testAPIClient.createAContact(
        CreateContactRequest(
          titleCode = "MR",
          lastName = "Last",
          firstName = "First",
          middleNames = "Middle Names",
        ),

      ).id
    }

    @Test
    fun `should not patch names when not provided`() {
      val req = PatchContactRequest()
      val res = testAPIClient.patchAContact(req, "/contact/$contactThatHasAllNameFields")

      assertThat(res.firstName).isEqualTo("First")
      assertThat(res.lastName).isEqualTo("Last")
      assertThat(res.middleNames).isEqualTo("Middle Names")
      assertThat(res.titleCode).isEqualTo("MR")
      assertThat(res.updatedBy).isEqualTo(updatedByUser)

      stubEvents.assertHasEvent(
        event = OutboundEvent.CONTACT_UPDATED,
        additionalInfo = ContactInfo(contactThatHasAllNameFields, Source.DPS, "read_write_user", "BXI"),
        personReference = PersonReference(dpsContactId = contactThatHasAllNameFields),
      )
    }

    @Test
    fun `should patch first and last names`() {
      val res = webTestClient.patch()
        .uri("/contact/$contactThatHasAllNameFields")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisationUsingCurrentUser())
        .bodyValue(
          """
          { "firstName": "update first", "lastName": "update last", "middleNames": "update middle", "titleCode": "MRS" }
          """.trimIndent(),
        )
        .exchange()
        .expectStatus()
        .isOk
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody(PatchContactResponse::class.java)
        .returnResult().responseBody!!

      assertThat(res.firstName).isEqualTo("update first")
      assertThat(res.lastName).isEqualTo("update last")
      assertThat(res.middleNames).isEqualTo("update middle")
      assertThat(res.titleCode).isEqualTo("MRS")
      assertThat(res.updatedBy).isEqualTo(updatedByUser)

      stubEvents.assertHasEvent(
        event = OutboundEvent.CONTACT_UPDATED,
        additionalInfo = ContactInfo(contactThatHasAllNameFields, Source.DPS, "read_write_user", "BXI"),
        personReference = PersonReference(dpsContactId = contactThatHasAllNameFields),
      )
    }

    @Test
    fun `should successfully patch middle name and title with null values`() {
      val req = PatchContactRequest(
        titleCode = JsonNullable.of(null),
        middleNames = JsonNullable.of(null),
      )
      val res = testAPIClient.patchAContact(req, "/contact/$contactThatHasAllNameFields")

      assertThat(res.firstName).isEqualTo("First")
      assertThat(res.lastName).isEqualTo("Last")
      assertThat(res.middleNames).isNull()
      assertThat(res.titleCode).isNull()
      assertThat(res.updatedBy).isEqualTo(updatedByUser)

      stubEvents.assertHasEvent(
        event = OutboundEvent.CONTACT_UPDATED,
        additionalInfo = ContactInfo(contactThatHasAllNameFields, Source.DPS, "read_write_user", "BXI"),
        personReference = PersonReference(dpsContactId = contactThatHasAllNameFields),
      )
    }

    @Test
    fun `should successfully patch middle name and title with a value`() {
      val req = PatchContactRequest(
        titleCode = JsonNullable.of("MRS"),
        middleNames = JsonNullable.of("Updated Middle"),
      )
      val res = testAPIClient.patchAContact(req, "/contact/$contactThatHasAllNameFields")

      assertThat(res.firstName).isEqualTo("First")
      assertThat(res.lastName).isEqualTo("Last")
      assertThat(res.middleNames).isEqualTo("Updated Middle")
      assertThat(res.titleCode).isEqualTo("MRS")
      assertThat(res.updatedBy).isEqualTo(updatedByUser)

      stubEvents.assertHasEvent(
        event = OutboundEvent.CONTACT_UPDATED,
        additionalInfo = ContactInfo(contactThatHasAllNameFields, Source.DPS, "read_write_user", "BXI"),
        personReference = PersonReference(dpsContactId = contactThatHasAllNameFields),
      )
    }

    @Test
    fun `should not be able to patch to an invalid title value`() {
      val req = PatchContactRequest(
        titleCode = JsonNullable.of("FOO"),
      )

      val uri = UriComponentsBuilder.fromPath("/contact/$contactThatHasAllNameFields")
        .build()
        .toUri()
      val errors = testAPIClient.getBadResponseErrorsWithPatch(req, uri)
      assertThat(errors.userMessage).isEqualTo("Validation failure: Unsupported title (FOO)")

      stubEvents.assertHasNoEvents(
        event = OutboundEvent.CONTACT_UPDATED,
        additionalInfo = ContactInfo(contactThatHasAllNameFields, Source.DPS, "read_write_user", "BXI"),
      )
    }

    @Test
    fun `should not be able to patch middle name when it's too long`() {
      val req = PatchContactRequest(
        middleNames = JsonNullable.of("".padEnd(36, 'X')),
      )

      val uri = UriComponentsBuilder.fromPath("/contact/$contactThatHasAllNameFields")
        .build()
        .toUri()
      val errors = testAPIClient.getBadResponseErrorsWithPatch(req, uri)
      assertThat(errors.userMessage).isEqualTo("Validation failure(s): middleNames must be <= 35 characters")

      stubEvents.assertHasNoEvents(
        event = OutboundEvent.CONTACT_UPDATED,
        additionalInfo = ContactInfo(contactThatHasAllNameFields, Source.DPS, "read_write_user", "BXI"),
      )
    }
  }

  @Nested
  inner class PatchGender {
    private var contactWithAGender = 0L

    @BeforeEach
    fun createContact() {
      contactWithAGender = testAPIClient.createAContact(
        CreateContactRequest(
          lastName = "Last",
          firstName = "First",
        ),

      ).id
      val entity = contactRepository.findById(contactWithAGender).get()
      contactRepository.saveAndFlush(entity.copy(gender = "NS"))
    }

    @Test
    fun `should not patch gender if undefined`() {
      val req = PatchContactRequest()
      val res = testAPIClient.patchAContact(req, "/contact/$contactWithAGender")

      assertThat(res.genderCode).isEqualTo("NS")
      assertThat(res.updatedBy).isEqualTo(updatedByUser)

      stubEvents.assertHasEvent(
        event = OutboundEvent.CONTACT_UPDATED,
        additionalInfo = ContactInfo(contactWithAGender, Source.DPS, "read_write_user", "BXI"),
        personReference = PersonReference(dpsContactId = contactWithAGender),
      )
    }

    @Test
    fun `should successfully patch gender with null values`() {
      val req = PatchContactRequest(
        genderCode = JsonNullable.of(null),
      )
      val res = testAPIClient.patchAContact(req, "/contact/$contactWithAGender")

      assertThat(res.genderCode).isNull()
      assertThat(res.updatedBy).isEqualTo(updatedByUser)

      stubEvents.assertHasEvent(
        event = OutboundEvent.CONTACT_UPDATED,
        additionalInfo = ContactInfo(contactWithAGender, Source.DPS, "read_write_user", "BXI"),
        personReference = PersonReference(dpsContactId = contactWithAGender),
      )
    }

    @Test
    fun `should successfully patch gender with a value`() {
      val req = PatchContactRequest(
        genderCode = JsonNullable.of("M"),
      )
      val res = testAPIClient.patchAContact(req, "/contact/$contactWithAGender")

      assertThat(res.genderCode).isEqualTo("M")
      assertThat(res.updatedBy).isEqualTo(updatedByUser)

      stubEvents.assertHasEvent(
        event = OutboundEvent.CONTACT_UPDATED,
        additionalInfo = ContactInfo(contactWithAGender, Source.DPS, "read_write_user", "BXI"),
        personReference = PersonReference(dpsContactId = contactWithAGender),
      )
    }

    @Test
    fun `should not be able to patch to an invalid gender value`() {
      val req = PatchContactRequest(
        genderCode = JsonNullable.of("FOO"),
      )

      val uri = UriComponentsBuilder.fromPath("/contact/$contactWithAGender")
        .build()
        .toUri()
      val errors = testAPIClient.getBadResponseErrorsWithPatch(req, uri)
      assertThat(errors.userMessage).isEqualTo("Validation failure: Unsupported gender (FOO)")

      stubEvents.assertHasNoEvents(OutboundEvent.CONTACT_UPDATED, ContactInfo(contactWithAGender, Source.DPS, "read_write_user", "BXI"))
    }
  }

  @Nested
  inner class DeceasedDate {
    private var contactWithDeceasedDate = 0L
    private val originalDeceasedDate = LocalDate.of(2000, 6, 15)

    @BeforeEach
    fun createContactWithDeceasedDate() {
      contactWithDeceasedDate = testAPIClient.createAContact(
        CreateContactRequest(
          lastName = "Date of death",
          firstName = "Has",
        ),
      ).id
      testAPIClient.patchAContact(
        PatchContactRequest(
          deceasedDate = JsonNullable.of(originalDeceasedDate),
        ),
        "/contact/$contactWithDeceasedDate",
      )
    }

    @Test
    fun `should not patch the deceased date when not provided`() {
      val res = testAPIClient.patchAContact(
        PatchContactRequest(),
        "/contact/$contactWithDeceasedDate",
      )

      assertThat(res.deceasedDate).isEqualTo(originalDeceasedDate)
      assertThat(res.updatedBy).isEqualTo(updatedByUser)

      stubEvents.assertHasEvent(
        event = OutboundEvent.CONTACT_UPDATED,
        additionalInfo = ContactInfo(contactWithDeceasedDate, Source.DPS, "read_write_user", "BXI"),
        personReference = PersonReference(dpsContactId = contactWithDeceasedDate),
      )
    }

    @Test
    fun `should successfully patch the deceased date with null value`() {
      val res = testAPIClient.patchAContact(
        PatchContactRequest(
          deceasedDate = JsonNullable.of(null),
        ),
        "/contact/$contactWithDeceasedDate",
      )

      assertThat(res.deceasedDate).isNull()
      assertThat(res.updatedBy).isEqualTo(updatedByUser)

      stubEvents.assertHasEvent(
        event = OutboundEvent.CONTACT_UPDATED,
        additionalInfo = ContactInfo(contactWithDeceasedDate, Source.DPS, "read_write_user", "BXI"),
        personReference = PersonReference(dpsContactId = contactWithDeceasedDate),
      )
    }

    @Test
    fun `should successfully patch the deceased date with a value`() {
      val req = PatchContactRequest(
        deceasedDate = JsonNullable.of(LocalDate.of(2000, 12, 25)),
      )

      val res = testAPIClient.patchAContact(req, "/contact/$contactWithDeceasedDate")

      assertThat(res.deceasedDate).isEqualTo(LocalDate.of(2000, 12, 25))
      assertThat(res.updatedBy).isEqualTo(updatedByUser)

      stubEvents.assertHasEvent(
        event = OutboundEvent.CONTACT_UPDATED,
        additionalInfo = ContactInfo(contactWithDeceasedDate, Source.DPS, "read_write_user", "BXI"),
        personReference = PersonReference(dpsContactId = contactWithDeceasedDate),
      )
    }
  }
}
