package uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.resource

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
import uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.SecureAPIIntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.CreateContactAddressRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.CreateContactRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.ContactAddressResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.ContactAddressInfo
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.PersonReference
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.Source
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

class CreateContactAddressIntegrationTest : SecureAPIIntegrationTestBase() {
  private var savedContactId = 0L

  override val allowedRoles: Set<String> = setOf("ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__RW")

  @BeforeEach
  fun initialiseData() {
    savedContactId = testAPIClient.createAContact(
      CreateContactRequest(
        lastName = "address",
        firstName = "has",
        createdBy = "created",
      ),
    ).id
  }

  override fun baseRequestBuilder(): WebTestClient.RequestHeadersSpec<*> = webTestClient.post()
    .uri("/contact/$savedContactId/address")
    .accept(MediaType.APPLICATION_JSON)
    .contentType(MediaType.APPLICATION_JSON)
    .bodyValue(aMinimalAddressRequest())

  @Test
  fun `should return forbidden if wrong role`() {
    webTestClient.post()
      .uri("/contact/$savedContactId/address")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(aMinimalAddressRequest())
      .headers(setAuthorisation(roles = listOf("ROLE_WRONG")))
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @ParameterizedTest
  @CsvSource(
    value = [
      "createdBy must not be null;{\"createdBy\": null}",
      "createdBy must not be null;{}",
    ],
    delimiter = ';',
  )
  fun `should return bad request if required fields are null`(expectedMessage: String, json: String) {
    val errors = webTestClient.post()
      .uri("/contact/$savedContactId/address")
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

  @ParameterizedTest
  @MethodSource("referenceTypeNotFound")
  fun `should enforce reference type value validation`(expectedMessage: String, request: CreateContactAddressRequest) {
    val errors = webTestClient.post()
      .uri("/contact/$savedContactId/address")
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

    assertThat(errors.userMessage).isEqualTo("Entity not found : $expectedMessage")

    stubEvents.assertHasNoEvents(
      event = OutboundEvent.CONTACT_ADDRESS_CREATED,
      additionalInfo = ContactAddressInfo(savedContactId, Source.DPS),
    )
  }

  @Test
  fun `should not create the address if the contact is not found`() {
    val request = aMinimalAddressRequest()

    val errors = webTestClient.post()
      .uri("/contact/-321/address")
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

    assertThat(errors.userMessage).isEqualTo("Entity not found : Contact (-321) not found")
  }

  @ParameterizedTest
  @ValueSource(strings = ["ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__RW"])
  fun `should create the contact address`(role: String) {
    val request = aMinimalAddressRequest()

    val created = testAPIClient.createAContactAddress(savedContactId, request, role)

    assertEqualsExcludingTimestamps(created, request)

    stubEvents.assertHasEvent(
      event = OutboundEvent.CONTACT_ADDRESS_CREATED,
      additionalInfo = ContactAddressInfo(created.contactAddressId, Source.DPS),
      personReference = PersonReference(dpsContactId = created.contactId),
    )
  }

  @Test
  fun `should be able to create the contact address with no address type`() {
    val request = aMinimalAddressRequest().copy(addressType = null)

    val created = testAPIClient.createAContactAddress(savedContactId, request)
    assertThat(created.addressType).isNull()

    assertThat(testAPIClient.getContact(savedContactId).addresses.find { it.contactAddressId == created.contactAddressId }).isNotNull

    stubEvents.assertHasEvent(
      event = OutboundEvent.CONTACT_ADDRESS_CREATED,
      additionalInfo = ContactAddressInfo(created.contactAddressId, Source.DPS),
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
      additionalInfo = ContactAddressInfo(created.contactAddressId, Source.DPS),
      personReference = PersonReference(dpsContactId = created.contactId),
    )
    stubEvents.assertHasEvent(
      event = OutboundEvent.CONTACT_ADDRESS_UPDATED,
      additionalInfo = ContactAddressInfo(primary.contactAddressId, Source.DPS),
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
      additionalInfo = ContactAddressInfo(created.contactAddressId, Source.DPS),
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
      additionalInfo = ContactAddressInfo(created.contactAddressId, Source.DPS),
      personReference = PersonReference(dpsContactId = created.contactId),
    )
    stubEvents.assertHasEvent(
      event = OutboundEvent.CONTACT_ADDRESS_UPDATED,
      additionalInfo = ContactAddressInfo(mail.contactAddressId, Source.DPS),
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
      additionalInfo = ContactAddressInfo(created.contactAddressId, Source.DPS),
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
      additionalInfo = ContactAddressInfo(created.contactAddressId, Source.DPS),
      personReference = PersonReference(dpsContactId = created.contactId),
    )
    stubEvents.assertHasEvent(
      event = OutboundEvent.CONTACT_ADDRESS_UPDATED,
      additionalInfo = ContactAddressInfo(primary.contactAddressId, Source.DPS),
      personReference = PersonReference(dpsContactId = created.contactId),
    )
    stubEvents.assertHasEvent(
      event = OutboundEvent.CONTACT_ADDRESS_UPDATED,
      additionalInfo = ContactAddressInfo(mail.contactAddressId, Source.DPS),
      personReference = PersonReference(dpsContactId = created.contactId),
    )
    stubEvents.assertHasNoEvents(
      event = OutboundEvent.CONTACT_ADDRESS_UPDATED,
      additionalInfo = ContactAddressInfo(other.contactAddressId, Source.DPS),
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
      additionalInfo = ContactAddressInfo(created.contactAddressId, Source.DPS),
      personReference = PersonReference(dpsContactId = created.contactId),
    )
    stubEvents.assertHasEvent(
      event = OutboundEvent.CONTACT_ADDRESS_UPDATED,
      additionalInfo = ContactAddressInfo(primaryAndMail.contactAddressId, Source.DPS),
      personReference = PersonReference(dpsContactId = created.contactId),
    )
  }
  companion object {
    @JvmStatic
    fun referenceTypeNotFound(): List<Arguments> = listOf(
      Arguments.of(
        "No reference data found for groupCode: CITY and code: INVALID",
        aMinimalAddressRequest().copy(cityCode = "INVALID"),
      ),
      Arguments.of(
        "No reference data found for groupCode: COUNTY and code: INVALID",
        aMinimalAddressRequest().copy(countyCode = "INVALID"),
      ),
      Arguments.of(
        "No reference data found for groupCode: COUNTRY and code: INVALID",
        aMinimalAddressRequest().copy(countryCode = "INVALID"),
      ),
    )

    private fun assertEqualsExcludingTimestamps(address: ContactAddressResponse, request: CreateContactAddressRequest) {
      with(address) {
        assertThat(addressType).isEqualTo(request.addressType)
        assertThat(primaryAddress).isEqualTo(request.primaryAddress)
        assertThat(property).isEqualTo(request.property)
        assertThat(street).isEqualTo(request.street)
        assertThat(postcode).isEqualTo(request.postcode)
        assertThat(createdBy).isEqualTo(request.createdBy)
        assertThat(createdTime).isNotNull()
      }
    }

    private fun aMinimalAddressRequest() = CreateContactAddressRequest(
      addressType = "HOME",
      primaryAddress = true,
      property = "27",
      street = "Hello Road",
      createdBy = "created",
    )
  }
}
