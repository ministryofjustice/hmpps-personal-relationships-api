package uk.gov.justice.digital.hmpps.personalrelationships.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.personalrelationships.integration.SecureAPIIntegrationTestBase
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.CreateContactRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.address.CreateContactAddressRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.phone.PhoneNumber
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.ContactAddressResponse
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.ContactAddressInfo
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.ContactAddressPhoneInfo
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.PersonReference
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.Source
import uk.gov.justice.digital.hmpps.personalrelationships.util.StubUser
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

class CreateContactAddressIntegrationTest : SecureAPIIntegrationTestBase() {
  private var savedContactId = 0L

  override val allowedRoles: Set<String> = setOf("ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__RW")

  @BeforeEach
  fun initialiseData() {
    setCurrentUser(StubUser.CREATING_USER)
    savedContactId = testAPIClient.createAContact(
      CreateContactRequest(
        lastName = "address",
        firstName = "has",
      ),
    ).id
  }

  override fun baseRequestBuilder(): WebTestClient.RequestHeadersSpec<*> = webTestClient.post()
    .uri("/contact/$savedContactId/address")
    .accept(MediaType.APPLICATION_JSON)
    .contentType(MediaType.APPLICATION_JSON)
    .bodyValue(aMinimalAddressRequest())

  @ParameterizedTest
  @CsvSource(
    value = [
      "countryCode must not be null;{\"countryCode\": null}",
      "countryCode must not be null;{}",
      "Unsupported phone type (UNKNOWN);{ \"phoneNumbers\": [ { \"phoneNumber\": \"01234567890\", \"phoneType\": \"UNKNOWN\" } ], \"countryCode\": \"ENG\"}",
      "phoneNumbers[0].phoneNumber must not be null;{ \"phoneNumbers\": [ { \"phoneType\": \"MOB\" } ], \"countryCode\": \"ENG\"}",
      "phoneNumbers[0].phoneType must not be null;{ \"phoneNumbers\": [ { \"phoneNumber\": \"01234567890\" } ], \"countryCode\": \"ENG\"}",
      "phoneNumbers must not be null;{ \"phoneNumbers\": null, \"countryCode\": \"ENG\"}",
    ],
    delimiter = ';',
  )
  fun `should return bad request if required fields are null`(expectedMessage: String, json: String) {
    val errors = webTestClient.post()
      .uri("/contact/$savedContactId/address")
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

  @ParameterizedTest
  @MethodSource("referenceTypeNotFound")
  fun `should enforce reference type value validation`(expectedTypeDescription: String, request: CreateContactAddressRequest) {
    val errors = webTestClient.post()
      .uri("/contact/$savedContactId/address")
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

    assertThat(errors.userMessage).isEqualTo("Validation failure: Unsupported $expectedTypeDescription (FOO)")

    stubEvents.assertHasNoEvents(
      event = OutboundEvent.CONTACT_ADDRESS_CREATED,
      additionalInfo = ContactAddressInfo(savedContactId, Source.DPS, "created", "BXI"),
    )
  }

  @ParameterizedTest
  @MethodSource("allFieldConstraintViolations")
  fun `should enforce field constraints`(expectedMessage: String, request: CreateContactAddressRequest) {
    val errors = webTestClient.post()
      .uri("/contact/$savedContactId/address")
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

    stubEvents.assertHasNoEvents(
      event = OutboundEvent.CONTACT_ADDRESS_CREATED,
      additionalInfo = ContactAddressInfo(savedContactId, Source.DPS, "created", "BXI"),
    )
  }

  @Test
  fun `should not create the address if the contact is not found`() {
    val request = aMinimalAddressRequest()

    val errors = webTestClient.post()
      .uri("/contact/-321/address")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationUsingCurrentUser())
      .bodyValue(request)
      .exchange()
      .expectStatus()
      .isNotFound
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody!!

    assertThat(errors.userMessage).isEqualTo("Entity not found : Contact (-321) not found")
  }

  @ParameterizedTest
  @ValueSource(strings = ["ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__RW"])
  fun `should create the contact address`(role: String) {
    setCurrentUser(StubUser.CREATING_USER.copy(roles = listOf(role)))
    val request = aMinimalAddressRequest()

    val created = testAPIClient.createAContactAddress(savedContactId, request)

    assertEqualsExcludingTimestamps(created, request)

    stubEvents.assertHasEvent(
      event = OutboundEvent.CONTACT_ADDRESS_CREATED,
      additionalInfo = ContactAddressInfo(created.contactAddressId, Source.DPS, "created", "BXI"),
      personReference = PersonReference(dpsContactId = created.contactId),
    )
  }

  @Test
  fun `should create the contact address with multiple address specific phone numbers`() {
    val request = CreateContactAddressRequest(
      addressType = "HOME",
      primaryAddress = true,
      property = "27",
      street = "Hello Road",
      countryCode = "ENG",
      phoneNumbers = listOf(
        PhoneNumber(phoneType = "MOB", phoneNumber = "07777123456", extNumber = null),
        PhoneNumber(phoneType = "BUS", phoneNumber = "07777123455", extNumber = null),
      ),
    )

    val created = testAPIClient.createAContactAddress(savedContactId, request)

    assertEqualsExcludingTimestamps(created, request)

    stubEvents.assertHasEvent(
      event = OutboundEvent.CONTACT_ADDRESS_CREATED,
      additionalInfo = ContactAddressInfo(created.contactAddressId, Source.DPS, "created", "BXI"),
      personReference = PersonReference(dpsContactId = created.contactId),
    )

    created.phoneNumberIds.map {
      stubEvents.assertHasEvent(
        event = OutboundEvent.CONTACT_ADDRESS_PHONE_CREATED,
        additionalInfo = ContactAddressPhoneInfo(it, created.contactAddressId, Source.DPS, "created", "BXI"),
        personReference = PersonReference(dpsContactId = created.contactId),
      )
    }
  }

  @Test
  fun `should rollback address creation when saving address phone number fails`() {
    val request = aMinimalAddressRequest().copy(
      phoneNumbers = listOf(
        PhoneNumber(phoneType = "INVALID", phoneNumber = "07777123456", extNumber = null),
      ),
    )

    webTestClient.post()
      .uri("/contact/$savedContactId/address")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationUsingCurrentUser())
      .bodyValue(request)
      .exchange()
      .expectStatus()
      .isBadRequest

    // Verify no address was created
    val contact = testAPIClient.getContact(savedContactId)
    assertThat(contact.addresses).isEmpty()

    // Verify no events were published
    stubEvents.assertHasNoEvents(event = OutboundEvent.CONTACT_ADDRESS_CREATED)
  }

  @Test
  fun `should be able to create the contact address with no address type`() {
    val request = aMinimalAddressRequest().copy(addressType = null)

    val created = testAPIClient.createAContactAddress(savedContactId, request)
    assertThat(created.addressType).isNull()

    assertThat(testAPIClient.getContact(savedContactId).addresses.find { it.contactAddressId == created.contactAddressId }).isNotNull

    stubEvents.assertHasEvent(
      event = OutboundEvent.CONTACT_ADDRESS_CREATED,
      additionalInfo = ContactAddressInfo(created.contactAddressId, Source.DPS, "created", "BXI"),
      personReference = PersonReference(dpsContactId = created.contactId),
    )
  }

  @Test
  fun `should remove primary flag from current primary addresses if setting primary`() {
    val requestToCreatePrimary = aMinimalAddressRequest().copy(primaryAddress = true)
    val primary = testAPIClient.createAContactAddress(savedContactId, requestToCreatePrimary)

    val request = aMinimalAddressRequest().copy(primaryAddress = true)
    val created = testAPIClient.createAContactAddress(savedContactId, request)

    val addresses = testAPIClient.getContact(savedContactId).addresses
    assertThat(addresses.find { it.contactAddressId == primary.contactAddressId }!!.primaryAddress).isFalse()
    assertThat(addresses.find { it.contactAddressId == created.contactAddressId }!!.primaryAddress).isTrue()

    stubEvents.assertHasEvent(
      event = OutboundEvent.CONTACT_ADDRESS_CREATED,
      additionalInfo = ContactAddressInfo(created.contactAddressId, Source.DPS, "created", "BXI"),
      personReference = PersonReference(dpsContactId = created.contactId),
    )
    stubEvents.assertHasEvent(
      event = OutboundEvent.CONTACT_ADDRESS_UPDATED,
      additionalInfo = ContactAddressInfo(primary.contactAddressId, Source.DPS, "created", "BXI"),
      personReference = PersonReference(dpsContactId = created.contactId),
    )
  }

  @Test
  fun `should not remove primary flag from current primary addresses if not setting primary`() {
    val requestToCreatePrimary = aMinimalAddressRequest().copy(primaryAddress = true)
    val primary = testAPIClient.createAContactAddress(savedContactId, requestToCreatePrimary)

    val request = aMinimalAddressRequest().copy(primaryAddress = false)
    val created = testAPIClient.createAContactAddress(savedContactId, request)

    val addresses = testAPIClient.getContact(savedContactId).addresses
    assertThat(addresses.find { it.contactAddressId == primary.contactAddressId }!!.primaryAddress).isTrue()
    assertThat(addresses.find { it.contactAddressId == created.contactAddressId }!!.primaryAddress).isFalse()

    stubEvents.assertHasEvent(
      event = OutboundEvent.CONTACT_ADDRESS_CREATED,
      additionalInfo = ContactAddressInfo(created.contactAddressId, Source.DPS, "created", "BXI"),
      personReference = PersonReference(dpsContactId = created.contactId),
    )
    stubEvents.assertHasNoEvents(event = OutboundEvent.CONTACT_ADDRESS_UPDATED)
  }

  @Test
  fun `should remove mail flag from current mail addresses if setting mail`() {
    val requestToCreateMail = aMinimalAddressRequest().copy(mailFlag = true)
    val mail = testAPIClient.createAContactAddress(savedContactId, requestToCreateMail)

    val request = aMinimalAddressRequest().copy(mailFlag = true)
    val created = testAPIClient.createAContactAddress(savedContactId, request)

    val addresses = testAPIClient.getContact(savedContactId).addresses
    assertThat(addresses.find { it.contactAddressId == mail.contactAddressId }!!.mailFlag).isFalse()
    assertThat(addresses.find { it.contactAddressId == created.contactAddressId }!!.mailFlag).isTrue()

    stubEvents.assertHasEvent(
      event = OutboundEvent.CONTACT_ADDRESS_CREATED,
      additionalInfo = ContactAddressInfo(created.contactAddressId, Source.DPS, "created", "BXI"),
      personReference = PersonReference(dpsContactId = created.contactId),
    )
    stubEvents.assertHasEvent(
      event = OutboundEvent.CONTACT_ADDRESS_UPDATED,
      additionalInfo = ContactAddressInfo(mail.contactAddressId, Source.DPS, "created", "BXI"),
      personReference = PersonReference(dpsContactId = created.contactId),
    )
  }

  @Test
  fun `should not remove mail flag from current mail addresses if not setting mail`() {
    val requestToCreateMail = aMinimalAddressRequest().copy(mailFlag = true, primaryAddress = false)
    val mail = testAPIClient.createAContactAddress(savedContactId, requestToCreateMail)

    val request = aMinimalAddressRequest().copy(mailFlag = false)
    val created = testAPIClient.createAContactAddress(savedContactId, request)

    val addresses = testAPIClient.getContact(savedContactId).addresses
    assertThat(addresses.find { it.contactAddressId == mail.contactAddressId }!!.mailFlag).isTrue()
    assertThat(addresses.find { it.contactAddressId == created.contactAddressId }!!.mailFlag).isFalse()

    stubEvents.assertHasEvent(
      event = OutboundEvent.CONTACT_ADDRESS_CREATED,
      additionalInfo = ContactAddressInfo(created.contactAddressId, Source.DPS, "created", "BXI"),
      personReference = PersonReference(dpsContactId = created.contactId),
    )
    stubEvents.assertHasNoEvents(event = OutboundEvent.CONTACT_ADDRESS_UPDATED)
  }

  @Test
  fun `should remove primary and mail flag from current primary and mail addresses if setting primary and mail`() {
    val requestToCreatePrimary = aMinimalAddressRequest().copy(primaryAddress = true, mailFlag = false)
    val primary = testAPIClient.createAContactAddress(savedContactId, requestToCreatePrimary)

    val requestToCreateMail = aMinimalAddressRequest().copy(primaryAddress = false, mailFlag = true)
    val mail = testAPIClient.createAContactAddress(savedContactId, requestToCreateMail)

    val requestToCreateOtherAddress = aMinimalAddressRequest().copy(primaryAddress = false, mailFlag = false)
    val other = testAPIClient.createAContactAddress(savedContactId, requestToCreateOtherAddress)

    val request = aMinimalAddressRequest().copy(primaryAddress = true, mailFlag = true)
    val created = testAPIClient.createAContactAddress(savedContactId, request)

    val addresses = testAPIClient.getContact(savedContactId).addresses
    assertThat(addresses.find { it.contactAddressId == primary.contactAddressId }!!.primaryAddress).isFalse()
    assertThat(addresses.find { it.contactAddressId == mail.contactAddressId }!!.mailFlag).isFalse()
    assertThat(addresses.find { it.contactAddressId == created.contactAddressId }!!.primaryAddress).isTrue()
    assertThat(addresses.find { it.contactAddressId == created.contactAddressId }!!.mailFlag).isTrue()

    stubEvents.assertHasEvent(
      event = OutboundEvent.CONTACT_ADDRESS_CREATED,
      additionalInfo = ContactAddressInfo(created.contactAddressId, Source.DPS, "created", "BXI"),
      personReference = PersonReference(dpsContactId = created.contactId),
    )
    stubEvents.assertHasEvent(
      event = OutboundEvent.CONTACT_ADDRESS_UPDATED,
      additionalInfo = ContactAddressInfo(primary.contactAddressId, Source.DPS, "created", "BXI"),
      personReference = PersonReference(dpsContactId = created.contactId),
    )
    stubEvents.assertHasEvent(
      event = OutboundEvent.CONTACT_ADDRESS_UPDATED,
      additionalInfo = ContactAddressInfo(mail.contactAddressId, Source.DPS, "created", "BXI"),
      personReference = PersonReference(dpsContactId = created.contactId),
    )
    stubEvents.assertHasNoEvents(
      event = OutboundEvent.CONTACT_ADDRESS_UPDATED,
      additionalInfo = ContactAddressInfo(other.contactAddressId, Source.DPS, "created", "BXI"),
    )
  }

  @Test
  fun `should remove primary and mail flag from current combined primary mail address if setting primary and mail`() {
    val requestToCreatePrimaryAndMail = aMinimalAddressRequest().copy(primaryAddress = true, mailFlag = true)
    val primaryAndMail = testAPIClient.createAContactAddress(
      savedContactId,
      requestToCreatePrimaryAndMail,
    )

    val request = aMinimalAddressRequest().copy(primaryAddress = true, mailFlag = true)
    val created = testAPIClient.createAContactAddress(savedContactId, request)

    val addresses = testAPIClient.getContact(savedContactId).addresses
    assertThat(addresses.find { it.contactAddressId == primaryAndMail.contactAddressId }!!.primaryAddress).isFalse()
    assertThat(addresses.find { it.contactAddressId == primaryAndMail.contactAddressId }!!.mailFlag).isFalse()
    assertThat(addresses.find { it.contactAddressId == created.contactAddressId }!!.primaryAddress).isTrue()
    assertThat(addresses.find { it.contactAddressId == created.contactAddressId }!!.mailFlag).isTrue()

    stubEvents.assertHasEvent(
      event = OutboundEvent.CONTACT_ADDRESS_CREATED,
      additionalInfo = ContactAddressInfo(created.contactAddressId, Source.DPS, "created", "BXI"),
      personReference = PersonReference(dpsContactId = created.contactId),
    )
    stubEvents.assertHasEvent(
      event = OutboundEvent.CONTACT_ADDRESS_UPDATED,
      additionalInfo = ContactAddressInfo(primaryAndMail.contactAddressId, Source.DPS, "created", "BXI"),
      personReference = PersonReference(dpsContactId = created.contactId),
    )
  }

  companion object {
    @JvmStatic
    fun referenceTypeNotFound(): List<Arguments> = listOf(
      Arguments.of(
        "address type",
        aMinimalAddressRequest().copy(addressType = "FOO"),
      ),
      Arguments.of(
        "city",
        aMinimalAddressRequest().copy(cityCode = "FOO"),
      ),
      Arguments.of(
        "county",
        aMinimalAddressRequest().copy(countyCode = "FOO"),
      ),
      Arguments.of(
        "country",
        aMinimalAddressRequest().copy(countryCode = "FOO"),
      ),
    )

    @JvmStatic
    fun allFieldConstraintViolations(): List<Arguments> = listOf(
      Arguments.of("flat must be <= 30 characters", aMinimalAddressRequest().copy(flat = "".padStart(31, 'X'))),
      Arguments.of("property must be <= 130 characters", aMinimalAddressRequest().copy(property = "".padStart(131, 'X'))),
      Arguments.of("street must be <= 160 characters", aMinimalAddressRequest().copy(street = "".padStart(161, 'X'))),
      Arguments.of("area must be <= 70 characters", aMinimalAddressRequest().copy(area = "".padStart(71, 'X'))),
      Arguments.of("postcode must be <= 12 characters", aMinimalAddressRequest().copy(postcode = "".padStart(13, 'X'))),
      Arguments.of("comments must be <= 240 characters", aMinimalAddressRequest().copy(comments = "".padStart(241, 'X'))),
      Arguments.of("phoneNumbers[0].phoneNumber must be <= 40 characters", aMinimalAddressRequest().copy(phoneNumbers = listOf(PhoneNumber(phoneType = "MOB", phoneNumber = "".padStart(41, 'X'), extNumber = null)))),
      Arguments.of("phoneNumbers[0].phoneType must be <= 12 characters", aMinimalAddressRequest().copy(phoneNumbers = listOf(PhoneNumber(phoneType = "".padStart(13, 'X'), phoneNumber = "07403322232", extNumber = null)))),
      Arguments.of("phoneNumbers[0].extNumber must be <= 7 characters", aMinimalAddressRequest().copy(phoneNumbers = listOf(PhoneNumber(phoneType = "MOB", phoneNumber = "07403322232", extNumber = "".padStart(8, 'X'))))),
    )

    private fun assertEqualsExcludingTimestamps(address: ContactAddressResponse, request: CreateContactAddressRequest) {
      with(address) {
        assertThat(addressType).isEqualTo(request.addressType)
        assertThat(primaryAddress).isEqualTo(request.primaryAddress)
        assertThat(property).isEqualTo(request.property)
        assertThat(street).isEqualTo(request.street)
        assertThat(postcode).isEqualTo(request.postcode)
        assertThat(createdTime).isNotNull()
      }
    }

    private fun aMinimalAddressRequest() = CreateContactAddressRequest(
      addressType = "HOME",
      primaryAddress = true,
      property = "27",
      street = "Hello Road",
      countryCode = "ENG",
    )
  }
}
