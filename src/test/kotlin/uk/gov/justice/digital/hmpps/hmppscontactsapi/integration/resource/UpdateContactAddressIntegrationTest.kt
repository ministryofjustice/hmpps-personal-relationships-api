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
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.CreateContactRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.address.CreateContactAddressRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.address.UpdateContactAddressRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.ContactAddressInfo
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.PersonReference
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.Source
import uk.gov.justice.digital.hmpps.hmppscontactsapi.util.StubUser
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

class UpdateContactAddressIntegrationTest : SecureAPIIntegrationTestBase() {
  private var savedContactId = 0L
  private var savedContactAddressId = 0L

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

    savedContactAddressId = testAPIClient.createAContactAddress(
      savedContactId,
      CreateContactAddressRequest(
        primaryAddress = false,
        mailFlag = false,
        addressType = "HOME",
        flat = "1A",
        property = "27",
        street = "Acacia Avenue",
        area = "Hoggs Bottom",
        postcode = "HB10 2NB",
        countryCode = "ENG",
      ),

    ).contactAddressId
    setCurrentUser(StubUser.UPDATING_USER)
  }

  override fun baseRequestBuilder(): WebTestClient.RequestHeadersSpec<*> = webTestClient.put()
    .uri("/contact/$savedContactId/address/$savedContactAddressId")
    .accept(MediaType.APPLICATION_JSON)
    .contentType(MediaType.APPLICATION_JSON)
    .bodyValue(aMinimalUpdateAddressRequest())

  @ParameterizedTest
  @CsvSource(
    value = [
      "countryCode must not be null;{\"primaryAddress\": true, \"countryCode\": null}",
      "countryCode must not be null;{\"primaryAddress\": true}",
      "primaryAddress must not be null;{\"countryCode\": \"ENG\"}",
    ],
    delimiter = ';',
  )
  fun `should return bad request if required fields are null`(expectedMessage: String, json: String) {
    val errors = webTestClient.put()
      .uri("/contact/$savedContactId/address/$savedContactAddressId")
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

    stubEvents.assertHasNoEvents(
      event = OutboundEvent.CONTACT_ADDRESS_UPDATED,
    )
  }

  @ParameterizedTest
  @MethodSource("allFieldConstraintViolations")
  fun `should enforce field constraints`(expectedMessage: String, request: UpdateContactAddressRequest) {
    val errors = webTestClient.put()
      .uri("/contact/$savedContactId/address/$savedContactAddressId")
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
      event = OutboundEvent.CONTACT_ADDRESS_UPDATED,
    )
  }

  @ParameterizedTest
  @MethodSource("referenceTypeNotFound")
  fun `should enforce reference type value validation`(expectedTypeDescription: String, request: UpdateContactAddressRequest) {
    val errors = webTestClient.put()
      .uri("/contact/$savedContactId/address/$savedContactAddressId")
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
      event = OutboundEvent.CONTACT_ADDRESS_UPDATED,
    )
  }

  @Test
  fun `should not update the address if the contact is not found`() {
    val request = aMinimalUpdateAddressRequest()

    val errors = webTestClient.put()
      .uri("/contact/-321/address/$savedContactAddressId")
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

    stubEvents.assertHasNoEvents(
      event = OutboundEvent.CONTACT_ADDRESS_UPDATED,
    )
  }

  @Test
  fun `should not update the address if the id is not found`() {
    val request = aMinimalUpdateAddressRequest()

    val errors = webTestClient.put()
      .uri("/contact/$savedContactId/address/-99")
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

    assertThat(errors.userMessage).isEqualTo("Entity not found : Contact address (-99) not found")

    stubEvents.assertHasNoEvents(
      event = OutboundEvent.CONTACT_ADDRESS_UPDATED,
    )
  }

  @ParameterizedTest
  @ValueSource(strings = ["ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__RW"])
  fun `should update the address`(role: String) {
    val request = UpdateContactAddressRequest(
      addressType = "HOME",
      primaryAddress = true,
      property = "28",
      street = "Acacia Avenue",
      area = "Hoggs Bottom",
      postcode = "HB10 1DJ",
      countryCode = "ENG",
    )

    val updated = testAPIClient.updateAContactAddress(
      savedContactId,
      savedContactAddressId,
      request,
      role,
    )

    with(updated) {
      assertThat(property).isEqualTo("28")
      assertThat(street).isEqualTo("Acacia Avenue")
      assertThat(area).isEqualTo("Hoggs Bottom")
      assertThat(postcode).isEqualTo("HB10 1DJ")
      assertThat(createdBy).isEqualTo("created")
      assertThat(createdTime).isNotNull()
      assertThat(updatedBy).isEqualTo("updated")
      assertThat(updatedTime).isNotNull()
    }

    stubEvents.assertHasEvent(
      event = OutboundEvent.CONTACT_ADDRESS_UPDATED,
      additionalInfo = ContactAddressInfo(savedContactAddressId, Source.DPS, "updated"),
      personReference = PersonReference(dpsContactId = savedContactId),
    )
  }

  @Test
  fun `should allow null on address type`() {
    val request = UpdateContactAddressRequest(
      addressType = null,
      primaryAddress = false,
      countryCode = "ENG",
    )

    val updated = testAPIClient.updateAContactAddress(
      savedContactId,
      savedContactAddressId,
      request,

    )
    assertThat(updated.addressType).isNull()

    stubEvents.assertHasEvent(
      event = OutboundEvent.CONTACT_ADDRESS_UPDATED,
      additionalInfo = ContactAddressInfo(savedContactAddressId, Source.DPS, "updated"),
      personReference = PersonReference(dpsContactId = savedContactId),
    )
  }

  @Test
  fun `should remove primary flag from current primary addresses if setting primary`() {
    val requestToCreatePrimary = aMinimalCreateAddressRequest().copy(primaryAddress = true)
    val primary = testAPIClient.createAContactAddress(savedContactId, requestToCreatePrimary)

    val request = aMinimalUpdateAddressRequest().copy(primaryAddress = true)
    val updated = testAPIClient.updateAContactAddress(
      savedContactId,
      savedContactAddressId,
      request,

    )

    val addresses = testAPIClient.getContact(savedContactId).addresses
    assertThat(addresses.find { it.contactAddressId == primary.contactAddressId }!!.primaryAddress).isFalse()
    assertThat(addresses.find { it.contactAddressId == updated.contactAddressId }!!.primaryAddress).isTrue()

    stubEvents.assertHasEvent(
      event = OutboundEvent.CONTACT_ADDRESS_UPDATED,
      additionalInfo = ContactAddressInfo(savedContactAddressId, Source.DPS, "updated"),
      personReference = PersonReference(dpsContactId = updated.contactId),
    )
    stubEvents.assertHasEvent(
      event = OutboundEvent.CONTACT_ADDRESS_UPDATED,
      additionalInfo = ContactAddressInfo(primary.contactAddressId, Source.DPS, "updated"),
      personReference = PersonReference(dpsContactId = updated.contactId),
    )
  }

  @Test
  fun `should not remove primary flag from current primary addresses if not setting primary`() {
    val requestToCreatePrimary = aMinimalCreateAddressRequest().copy(primaryAddress = true)
    val primary = testAPIClient.createAContactAddress(savedContactId, requestToCreatePrimary)

    val request = aMinimalUpdateAddressRequest().copy(primaryAddress = false)
    val updated = testAPIClient.updateAContactAddress(
      savedContactId,
      savedContactAddressId,
      request,

    )

    val addresses = testAPIClient.getContact(savedContactId).addresses
    assertThat(addresses.find { it.contactAddressId == primary.contactAddressId }!!.primaryAddress).isTrue()
    assertThat(addresses.find { it.contactAddressId == updated.contactAddressId }!!.primaryAddress).isFalse()

    stubEvents.assertHasEvent(
      event = OutboundEvent.CONTACT_ADDRESS_UPDATED,
      additionalInfo = ContactAddressInfo(savedContactAddressId, Source.DPS, "updated"),
      personReference = PersonReference(dpsContactId = updated.contactId),
    )
  }

  @Test
  fun `should remove mail flag from current mail addresses if setting mail`() {
    val requestToCreateMail = aMinimalCreateAddressRequest().copy(mailFlag = true)
    val mail = testAPIClient.createAContactAddress(savedContactId, requestToCreateMail)

    val request = aMinimalUpdateAddressRequest().copy(mailFlag = true)
    val updated = testAPIClient.updateAContactAddress(
      savedContactId,
      savedContactAddressId,
      request,

    )

    val addresses = testAPIClient.getContact(savedContactId).addresses
    assertThat(addresses.find { it.contactAddressId == mail.contactAddressId }!!.mailFlag).isFalse()
    assertThat(addresses.find { it.contactAddressId == updated.contactAddressId }!!.mailFlag).isTrue()

    stubEvents.assertHasEvent(
      event = OutboundEvent.CONTACT_ADDRESS_UPDATED,
      additionalInfo = ContactAddressInfo(savedContactAddressId, Source.DPS, "updated"),
      personReference = PersonReference(dpsContactId = updated.contactId),
    )
    stubEvents.assertHasEvent(
      event = OutboundEvent.CONTACT_ADDRESS_UPDATED,
      additionalInfo = ContactAddressInfo(mail.contactAddressId, Source.DPS, "updated"),
      personReference = PersonReference(dpsContactId = updated.contactId),
    )
  }

  @Test
  fun `should not remove mail flag from current mail addresses if not setting mail`() {
    val requestToCreateMail = aMinimalCreateAddressRequest().copy(mailFlag = true, primaryAddress = false)
    val mail = testAPIClient.createAContactAddress(savedContactId, requestToCreateMail)

    val request = aMinimalUpdateAddressRequest().copy(mailFlag = false)
    val updated = testAPIClient.updateAContactAddress(
      savedContactId,
      savedContactAddressId,
      request,

    )

    val addresses = testAPIClient.getContact(savedContactId).addresses
    assertThat(addresses.find { it.contactAddressId == mail.contactAddressId }!!.mailFlag).isTrue()
    assertThat(addresses.find { it.contactAddressId == updated.contactAddressId }!!.mailFlag).isFalse()

    stubEvents.assertHasEvent(
      event = OutboundEvent.CONTACT_ADDRESS_UPDATED,
      additionalInfo = ContactAddressInfo(savedContactAddressId, Source.DPS, "updated"),
      personReference = PersonReference(dpsContactId = updated.contactId),
    )
  }

  @Test
  fun `should remove primary and mail flag from current primary and mail addresses if setting primary and mail`() {
    val requestToCreatePrimary = aMinimalCreateAddressRequest().copy(primaryAddress = true, mailFlag = false)
    val primary = testAPIClient.createAContactAddress(savedContactId, requestToCreatePrimary)

    val requestToCreateMail = aMinimalCreateAddressRequest().copy(primaryAddress = false, mailFlag = true)
    val mail = testAPIClient.createAContactAddress(savedContactId, requestToCreateMail)

    val requestToCreateOtherAddress = aMinimalCreateAddressRequest().copy(primaryAddress = false, mailFlag = false)
    val other = testAPIClient.createAContactAddress(savedContactId, requestToCreateOtherAddress)

    val request = aMinimalUpdateAddressRequest().copy(primaryAddress = true, mailFlag = true)
    val updated = testAPIClient.updateAContactAddress(
      savedContactId,
      savedContactAddressId,
      request,

    )

    val addresses = testAPIClient.getContact(savedContactId).addresses
    assertThat(addresses.find { it.contactAddressId == primary.contactAddressId }!!.primaryAddress).isFalse()
    assertThat(addresses.find { it.contactAddressId == mail.contactAddressId }!!.mailFlag).isFalse()
    assertThat(addresses.find { it.contactAddressId == updated.contactAddressId }!!.primaryAddress).isTrue()
    assertThat(addresses.find { it.contactAddressId == updated.contactAddressId }!!.mailFlag).isTrue()

    stubEvents.assertHasEvent(
      event = OutboundEvent.CONTACT_ADDRESS_UPDATED,
      additionalInfo = ContactAddressInfo(savedContactAddressId, Source.DPS, "updated"),
      personReference = PersonReference(dpsContactId = updated.contactId),
    )
    stubEvents.assertHasEvent(
      event = OutboundEvent.CONTACT_ADDRESS_UPDATED,
      additionalInfo = ContactAddressInfo(primary.contactAddressId, Source.DPS, "updated"),
      personReference = PersonReference(dpsContactId = updated.contactId),
    )
    stubEvents.assertHasEvent(
      event = OutboundEvent.CONTACT_ADDRESS_UPDATED,
      additionalInfo = ContactAddressInfo(mail.contactAddressId, Source.DPS, "updated"),
      personReference = PersonReference(dpsContactId = updated.contactId),
    )
    stubEvents.assertHasNoEvents(
      event = OutboundEvent.CONTACT_ADDRESS_UPDATED,
      additionalInfo = ContactAddressInfo(other.contactAddressId, Source.DPS, "updated"),
    )
  }

  @Test
  fun `should remove primary and mail flag from current combined primary mail address if setting primary and mail`() {
    val requestToCreatePrimaryAndMail = aMinimalCreateAddressRequest().copy(primaryAddress = true, mailFlag = true)
    val primaryAndMail = testAPIClient.createAContactAddress(
      savedContactId,
      requestToCreatePrimaryAndMail,

    )

    val request = aMinimalUpdateAddressRequest().copy(primaryAddress = true, mailFlag = true)
    val updated = testAPIClient.updateAContactAddress(
      savedContactId,
      savedContactAddressId,
      request,

    )

    val addresses = testAPIClient.getContact(savedContactId).addresses
    assertThat(addresses.find { it.contactAddressId == primaryAndMail.contactAddressId }!!.primaryAddress).isFalse()
    assertThat(addresses.find { it.contactAddressId == primaryAndMail.contactAddressId }!!.mailFlag).isFalse()
    assertThat(addresses.find { it.contactAddressId == updated.contactAddressId }!!.primaryAddress).isTrue()
    assertThat(addresses.find { it.contactAddressId == updated.contactAddressId }!!.mailFlag).isTrue()

    stubEvents.assertHasEvent(
      event = OutboundEvent.CONTACT_ADDRESS_UPDATED,
      additionalInfo = ContactAddressInfo(savedContactAddressId, Source.DPS, "updated"),
      personReference = PersonReference(dpsContactId = updated.contactId),
    )
    stubEvents.assertHasEvent(
      event = OutboundEvent.CONTACT_ADDRESS_UPDATED,
      additionalInfo = ContactAddressInfo(primaryAndMail.contactAddressId, Source.DPS, "updated"),
      personReference = PersonReference(dpsContactId = updated.contactId),
    )
  }

  companion object {
    @JvmStatic
    fun allFieldConstraintViolations(): List<Arguments> = listOf(
      Arguments.of("flat must be <= 30 characters", aMinimalUpdateAddressRequest().copy(flat = "".padStart(31, 'X'))),
      Arguments.of(
        "property must be <= 50 characters",
        aMinimalUpdateAddressRequest().copy(property = "".padStart(51, 'X')),
      ),
      Arguments.of(
        "street must be <= 160 characters",
        aMinimalUpdateAddressRequest().copy(street = "".padStart(161, 'X')),
      ),
      Arguments.of("area must be <= 70 characters", aMinimalUpdateAddressRequest().copy(area = "".padStart(71, 'X'))),
      Arguments.of(
        "postcode must be <= 12 characters",
        aMinimalUpdateAddressRequest().copy(postcode = "".padStart(13, 'X')),
      ),
      Arguments.of(
        "comments must be <= 240 characters",
        aMinimalUpdateAddressRequest().copy(comments = "".padStart(241, 'X')),
      ),
    )

    @JvmStatic
    fun referenceTypeNotFound(): List<Arguments> = listOf(
      Arguments.of(
        "address type",
        aMinimalUpdateAddressRequest().copy(addressType = "FOO"),
      ),
      Arguments.of(
        "city",
        aMinimalUpdateAddressRequest().copy(cityCode = "FOO"),
      ),
      Arguments.of(
        "county",
        aMinimalUpdateAddressRequest().copy(countyCode = "FOO"),
      ),
      Arguments.of(
        "country",
        aMinimalUpdateAddressRequest().copy(countryCode = "FOO"),
      ),
    )

    private fun aMinimalUpdateAddressRequest() = UpdateContactAddressRequest(
      addressType = "HOME",
      primaryAddress = false,
      mailFlag = false,
      property = "27",
      street = "Hello Road",
      countryCode = "ENG",
    )

    private fun aMinimalCreateAddressRequest() = CreateContactAddressRequest(
      addressType = "HOME",
      primaryAddress = false,
      mailFlag = false,
      property = "27",
      street = "Hello Road",
      countryCode = "ENG",
    )
  }
}
