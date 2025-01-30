package uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import org.openapitools.jackson.nullable.JsonNullable
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.SecureAPIIntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.CreateContactAddressRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.CreateContactRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.PatchContactAddressRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.ContactAddressInfo
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.PersonReference
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.Source
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.time.LocalDate

class PatchContactAddressIntegrationTest : SecureAPIIntegrationTestBase() {
  private var savedContactId = 0L
  private var savedContactAddressId = 0L

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
        createdBy = "created",
      ),

    ).contactAddressId
  }

  override fun baseRequestBuilder(): WebTestClient.RequestHeadersSpec<*> = webTestClient.patch()
    .uri("/contact/$savedContactId/address/$savedContactAddressId")
    .accept(MediaType.APPLICATION_JSON)
    .contentType(MediaType.APPLICATION_JSON)
    .bodyValue(aMinimalPatchAddressRequest())

  @ParameterizedTest
  @CsvSource(
    value = [
      "updatedBy must not be null;{\"updatedBy\": null}",
      "updatedBy must not be null;{}",
    ],
    delimiter = ';',
  )
  fun `should return bad request if required fields are null`(expectedMessage: String, json: String) {
    val errors = webTestClient.patch()
      .uri("/contact/$savedContactId/address/$savedContactAddressId")
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

    stubEvents.assertHasNoEvents(
      event = OutboundEvent.CONTACT_ADDRESS_UPDATED,
      additionalInfo = ContactAddressInfo(savedContactAddressId, Source.DPS),
    )
  }

  @ParameterizedTest
  @MethodSource("allFieldConstraintViolations")
  fun `should enforce field constraints`(expectedMessage: String, request: PatchContactAddressRequest) {
    val errors = webTestClient.patch()
      .uri("/contact/$savedContactId/address/$savedContactAddressId")
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

    stubEvents.assertHasNoEvents(
      event = OutboundEvent.CONTACT_ADDRESS_UPDATED,
      additionalInfo = ContactAddressInfo(savedContactAddressId, Source.DPS),
    )
  }

  @ParameterizedTest
  @MethodSource("referenceTypeNotFound")
  fun `should enforce reference type value validation`(expectedMessage: String, request: PatchContactAddressRequest) {
    val errors = webTestClient.patch()
      .uri("/contact/$savedContactId/address/$savedContactAddressId")
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
      event = OutboundEvent.CONTACT_ADDRESS_UPDATED,
      additionalInfo = ContactAddressInfo(savedContactAddressId, Source.DPS),
    )
  }

  @Test
  fun `should not update the address if the contact is not found`() {
    val request = aMinimalPatchAddressRequest()

    val errors = webTestClient.patch()
      .uri("/contact/-321/address/$savedContactAddressId")
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

    stubEvents.assertHasNoEvents(
      event = OutboundEvent.CONTACT_ADDRESS_UPDATED,
      additionalInfo = ContactAddressInfo(savedContactAddressId, Source.DPS),
    )
  }

  @Test
  fun `should not update the address if the id is not found`() {
    val request = aMinimalPatchAddressRequest()

    val errors = webTestClient.patch()
      .uri("/contact/$savedContactId/address/-99")
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

    assertThat(errors.userMessage).isEqualTo("Entity not found : Contact address (-99) not found")

    stubEvents.assertHasNoEvents(
      event = OutboundEvent.CONTACT_ADDRESS_UPDATED,
      additionalInfo = ContactAddressInfo(-99, Source.DPS),
    )
  }

  @ParameterizedTest
  @ValueSource(strings = ["ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__RW"])
  fun `should update the address`(role: String) {
    val request = PatchContactAddressRequest(
      addressType = JsonNullable.of("HOME"),
      primaryAddress = JsonNullable.of(true),
      flat = JsonNullable.of("13"),
      property = JsonNullable.of("28"),
      street = JsonNullable.of("Acacia Avenue"),
      area = JsonNullable.of("Hoggs Bottom"),
      cityCode = JsonNullable.of("11498"),
      countyCode = JsonNullable.of("SOMERSET"),
      postcode = JsonNullable.of("HB10 1DJ"),
      countryCode = JsonNullable.of("ENG"),
      verified = JsonNullable.of(false),
      mailFlag = JsonNullable.of(false),
      startDate = JsonNullable.of(LocalDate.of(2000, 12, 25)),
      endDate = JsonNullable.of(LocalDate.of(2001, 12, 25)),
      noFixedAddress = JsonNullable.of(false),
      comments = JsonNullable.of("No comments"),
      updatedBy = "updated",
    )

    val updated = testAPIClient.patchAContactAddress(
      savedContactId,
      savedContactAddressId,
      request,
      role,
    )

    with(updated) {
      assertThat(addressType).isEqualTo("HOME")
      assertThat(primaryAddress).isTrue()
      assertThat(flat).isEqualTo("13")
      assertThat(property).isEqualTo("28")
      assertThat(street).isEqualTo("Acacia Avenue")
      assertThat(area).isEqualTo("Hoggs Bottom")
      assertThat(cityCode).isEqualTo("11498")
      assertThat(countyCode).isEqualTo("SOMERSET")
      assertThat(postcode).isEqualTo("HB10 1DJ")
      assertThat(countryCode).isEqualTo("ENG")
      assertThat(verified).isFalse()
      assertThat(mailFlag).isFalse()
      assertThat(startDate).isEqualTo(LocalDate.of(2000, 12, 25))
      assertThat(endDate).isEqualTo(LocalDate.of(2001, 12, 25))
      assertThat(noFixedAddress).isFalse()
      assertThat(comments).isEqualTo("No comments")
      assertThat(createdBy).isEqualTo("created")
      assertThat(createdTime).isNotNull()
      assertThat(updatedBy).isEqualTo("updated")
      assertThat(updatedTime).isNotNull()
    }

    stubEvents.assertHasEvent(
      event = OutboundEvent.CONTACT_ADDRESS_UPDATED,
      additionalInfo = ContactAddressInfo(savedContactAddressId, Source.DPS),
      personReference = PersonReference(dpsContactId = savedContactId),
    )
  }

  @ParameterizedTest
  @MethodSource("setNullOnGivenField")
  fun `should allow null on nullable field`(expectedField: String, request: PatchContactAddressRequest) {
    val updated = testAPIClient.patchAContactAddress(
      savedContactId,
      savedContactAddressId,
      request,
    )
    val fieldValue = updated::class.members
      .firstOrNull { it.name == expectedField }
      ?.call(updated)

    assertThat(fieldValue).isNull()

    stubEvents.assertHasEvent(
      event = OutboundEvent.CONTACT_ADDRESS_UPDATED,
      additionalInfo = ContactAddressInfo(savedContactAddressId, Source.DPS),
      personReference = PersonReference(dpsContactId = savedContactId),
    )
  }

  @Test
  fun `should remove primary flag from current primary addresses if setting primary`() {
    val requestToCreatePrimary = aMinimalCreateAddressRequest().copy(primaryAddress = true)
    val primary = testAPIClient.createAContactAddress(savedContactId, requestToCreatePrimary)

    val request = aMinimalPatchAddressRequest().copy(primaryAddress = JsonNullable.of(true))
    val updated = testAPIClient.patchAContactAddress(
      savedContactId,
      savedContactAddressId,
      request,
    )

    val addresses = testAPIClient.getContact(savedContactId).addresses
    assertThat(addresses.find { it.contactAddressId == primary.contactAddressId }!!.primaryAddress).isFalse()
    assertThat(addresses.find { it.contactAddressId == updated.contactAddressId }!!.primaryAddress).isTrue()

    stubEvents.assertHasEvent(
      event = OutboundEvent.CONTACT_ADDRESS_UPDATED,
      additionalInfo = ContactAddressInfo(savedContactAddressId, Source.DPS),
      personReference = PersonReference(dpsContactId = updated.contactId),
    )
    stubEvents.assertHasEvent(
      event = OutboundEvent.CONTACT_ADDRESS_UPDATED,
      additionalInfo = ContactAddressInfo(primary.contactAddressId, Source.DPS),
      personReference = PersonReference(dpsContactId = updated.contactId),
    )
  }

  @Test
  fun `should not remove primary flag from current primary addresses if not setting primary`() {
    val requestToCreatePrimary = aMinimalCreateAddressRequest().copy(primaryAddress = true)
    val primary = testAPIClient.createAContactAddress(savedContactId, requestToCreatePrimary)

    val request = aMinimalPatchAddressRequest().copy(primaryAddress = JsonNullable.of(false))
    val updated = testAPIClient.patchAContactAddress(
      savedContactId,
      savedContactAddressId,
      request,
    )

    val addresses = testAPIClient.getContact(savedContactId).addresses
    assertThat(addresses.find { it.contactAddressId == primary.contactAddressId }!!.primaryAddress).isTrue()
    assertThat(addresses.find { it.contactAddressId == updated.contactAddressId }!!.primaryAddress).isFalse()

    stubEvents.assertHasEvent(
      event = OutboundEvent.CONTACT_ADDRESS_UPDATED,
      additionalInfo = ContactAddressInfo(savedContactAddressId, Source.DPS),
      personReference = PersonReference(dpsContactId = updated.contactId),
    )
  }

  @Test
  fun `should remove mail flag from current mail addresses if setting mail`() {
    val requestToCreateMail = aMinimalCreateAddressRequest().copy(mailFlag = true)
    val mail = testAPIClient.createAContactAddress(savedContactId, requestToCreateMail)

    val request = aMinimalPatchAddressRequest().copy(mailFlag = JsonNullable.of(true))
    val updated = testAPIClient.patchAContactAddress(
      savedContactId,
      savedContactAddressId,
      request,
    )

    val addresses = testAPIClient.getContact(savedContactId).addresses
    assertThat(addresses.find { it.contactAddressId == mail.contactAddressId }!!.mailFlag).isFalse()
    assertThat(addresses.find { it.contactAddressId == updated.contactAddressId }!!.mailFlag).isTrue()

    stubEvents.assertHasEvent(
      event = OutboundEvent.CONTACT_ADDRESS_UPDATED,
      additionalInfo = ContactAddressInfo(savedContactAddressId, Source.DPS),
      personReference = PersonReference(dpsContactId = updated.contactId),
    )
    stubEvents.assertHasEvent(
      event = OutboundEvent.CONTACT_ADDRESS_UPDATED,
      additionalInfo = ContactAddressInfo(mail.contactAddressId, Source.DPS),
      personReference = PersonReference(dpsContactId = updated.contactId),
    )
  }

  @Test
  fun `should not remove mail flag from current mail addresses if not setting mail`() {
    val requestToCreateMail = aMinimalCreateAddressRequest().copy(mailFlag = true, primaryAddress = false)
    val mail = testAPIClient.createAContactAddress(savedContactId, requestToCreateMail)

    val request = aMinimalPatchAddressRequest().copy(mailFlag = JsonNullable.of(false))
    val updated = testAPIClient.patchAContactAddress(
      savedContactId,
      savedContactAddressId,
      request,
    )

    val addresses = testAPIClient.getContact(savedContactId).addresses
    assertThat(addresses.find { it.contactAddressId == mail.contactAddressId }!!.mailFlag).isTrue()
    assertThat(addresses.find { it.contactAddressId == updated.contactAddressId }!!.mailFlag).isFalse()

    stubEvents.assertHasEvent(
      event = OutboundEvent.CONTACT_ADDRESS_UPDATED,
      additionalInfo = ContactAddressInfo(savedContactAddressId, Source.DPS),
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

    val request = aMinimalPatchAddressRequest().copy(
      primaryAddress = JsonNullable.of(true),
      mailFlag = JsonNullable.of(true),
    )
    val updated = testAPIClient.patchAContactAddress(
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
      additionalInfo = ContactAddressInfo(savedContactAddressId, Source.DPS),
      personReference = PersonReference(dpsContactId = updated.contactId),
    )
    stubEvents.assertHasEvent(
      event = OutboundEvent.CONTACT_ADDRESS_UPDATED,
      additionalInfo = ContactAddressInfo(primary.contactAddressId, Source.DPS),
      personReference = PersonReference(dpsContactId = updated.contactId),
    )
    stubEvents.assertHasEvent(
      event = OutboundEvent.CONTACT_ADDRESS_UPDATED,
      additionalInfo = ContactAddressInfo(mail.contactAddressId, Source.DPS),
      personReference = PersonReference(dpsContactId = updated.contactId),
    )
    stubEvents.assertHasNoEvents(
      event = OutboundEvent.CONTACT_ADDRESS_UPDATED,
      additionalInfo = ContactAddressInfo(other.contactAddressId, Source.DPS),
    )
  }

  @Test
  fun `should remove primary and mail flag from current combined primary mail address if setting primary and mail`() {
    val requestToCreatePrimaryAndMail = aMinimalCreateAddressRequest().copy(primaryAddress = true, mailFlag = true)
    val primaryAndMail = testAPIClient.createAContactAddress(
      savedContactId,
      requestToCreatePrimaryAndMail,
    )

    val request = aMinimalPatchAddressRequest().copy(
      primaryAddress = JsonNullable.of(true),
      mailFlag = JsonNullable.of(true),
    )
    val updated = testAPIClient.patchAContactAddress(
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
      additionalInfo = ContactAddressInfo(savedContactAddressId, Source.DPS),
      personReference = PersonReference(dpsContactId = updated.contactId),
    )
    stubEvents.assertHasEvent(
      event = OutboundEvent.CONTACT_ADDRESS_UPDATED,
      additionalInfo = ContactAddressInfo(primaryAndMail.contactAddressId, Source.DPS),
      personReference = PersonReference(dpsContactId = updated.contactId),
    )
  }

  companion object {
    @JvmStatic
    fun allFieldConstraintViolations(): List<Arguments> = listOf(
      Arguments.of(
        "addressType must be <= 12 characters",
        aMinimalPatchAddressRequest().copy(addressType = JsonNullable.of("".padStart(13))),
      ),
      Arguments.of(
        "cityCode must be <= 12 characters",
        aMinimalPatchAddressRequest().copy(cityCode = JsonNullable.of("".padStart(13))),
      ),
      Arguments.of(
        "countyCode must be <= 12 characters",
        aMinimalPatchAddressRequest().copy(countyCode = JsonNullable.of("".padStart(13))),
      ),
      Arguments.of(
        "countryCode must be <= 12 characters",
        aMinimalPatchAddressRequest().copy(countryCode = JsonNullable.of("".padStart(13))),
      ),
      Arguments.of(
        "updatedBy must be <= 100 characters",
        aMinimalPatchAddressRequest().copy(updatedBy = "".padStart(101)),
      ),
    )

    @JvmStatic
    fun referenceTypeNotFound(): List<Arguments> = listOf(
      Arguments.of(
        "No reference data found for groupCode: CITY and code: INVALID",
        aMinimalPatchAddressRequest().copy(cityCode = JsonNullable.of("INVALID")),
      ),
      Arguments.of(
        "No reference data found for groupCode: COUNTY and code: INVALID",
        aMinimalPatchAddressRequest().copy(countyCode = JsonNullable.of("INVALID")),
      ),
      Arguments.of(
        "No reference data found for groupCode: COUNTRY and code: INVALID",
        aMinimalPatchAddressRequest().copy(countryCode = JsonNullable.of("INVALID")),
      ),
    )

    @JvmStatic
    fun setNullOnGivenField(): List<Arguments> = listOf(

      Arguments.of(
        "addressType",
        aMinimalPatchAddressRequest().copy(addressType = JsonNullable.of(null)),
      ),
      Arguments.of(
        "flat",
        aMinimalPatchAddressRequest().copy(flat = JsonNullable.of(null)),
      ),
      Arguments.of(
        "property",
        aMinimalPatchAddressRequest().copy(property = JsonNullable.of(null)),
      ),
      Arguments.of(
        "street",
        aMinimalPatchAddressRequest().copy(street = JsonNullable.of(null)),
      ),
      Arguments.of(
        "area",
        aMinimalPatchAddressRequest().copy(area = JsonNullable.of(null)),
      ),
      Arguments.of(
        "cityCode",
        aMinimalPatchAddressRequest().copy(cityCode = JsonNullable.of(null)),
      ),
      Arguments.of(
        "countyCode",
        aMinimalPatchAddressRequest().copy(countyCode = JsonNullable.of(null)),
      ),
      Arguments.of(
        "countryCode",
        aMinimalPatchAddressRequest().copy(countryCode = JsonNullable.of(null)),
      ),
      Arguments.of(
        "postcode",
        aMinimalPatchAddressRequest().copy(postcode = JsonNullable.of(null)),
      ),
      Arguments.of(
        "startDate",
        aMinimalPatchAddressRequest().copy(startDate = JsonNullable.of(null)),
      ),
      Arguments.of(
        "endDate",
        aMinimalPatchAddressRequest().copy(endDate = JsonNullable.of(null)),
      ),

      Arguments.of(
        "comments",
        aMinimalPatchAddressRequest().copy(comments = JsonNullable.of(null)),
      ),
    )

    private fun aMinimalPatchAddressRequest() = PatchContactAddressRequest(
      updatedBy = "updated",
    )

    private fun aMinimalCreateAddressRequest() = CreateContactAddressRequest(
      addressType = "HOME",
      primaryAddress = false,
      mailFlag = false,
      property = "27",
      street = "Hello Road",
      createdBy = "created",
    )
  }
}
