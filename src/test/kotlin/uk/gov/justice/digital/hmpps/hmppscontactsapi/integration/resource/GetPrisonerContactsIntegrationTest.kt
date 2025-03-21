package uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.resource

import org.apache.commons.lang3.RandomStringUtils
import org.apache.commons.lang3.RandomUtils
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.openapitools.jackson.nullable.JsonNullable
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.SecureAPIIntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.helper.TestAPIClient.PrisonerContactSummaryResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.ContactRelationship
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.CreateContactRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.PatchRelationshipRequest
import java.time.LocalDate

class GetPrisonerContactsIntegrationTest : SecureAPIIntegrationTestBase() {
  companion object {
    private const val GET_PRISONER_CONTACT = "/prisoner/A4385DZ/contact"
  }

  override val allowedRoles: Set<String> = setOf("ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__RW", "ROLE_CONTACTS__R")

  override fun baseRequestBuilder(): WebTestClient.RequestHeadersSpec<*> = webTestClient.get()
    .uri("/prisoner/P001/contact")

  @Test
  fun `should return not found if no prisoner found`() {
    stubPrisonSearchWithNotFoundResponse("A4385DZ")

    webTestClient.get()
      .uri("/prisoner/A4385DZ/contact")
      .headers(setAuthorisation(roles = listOf("ROLE_CONTACTS_ADMIN")))
      .exchange()
      .expectStatus()
      .isNotFound
  }

  @ParameterizedTest
  @ValueSource(strings = ["ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__R", "ROLE_CONTACTS__RW"])
  fun `should return social and official contacts`(role: String) {
    stubPrisonSearchWithResponse("A4162DZ")

    val contacts = webTestClient.get()
      .uri("/prisoner/A4162DZ/contact")
      .headers(setAuthorisation(roles = listOf(role)))
      .exchange()
      .expectStatus()
      .isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(PrisonerContactSummaryResponse::class.java)
      .returnResult().responseBody!!

    assertThat(contacts.content).hasSize(6)
  }

  @Test
  fun `should return OK`() {
    stubPrisonSearchWithResponse("A4385DZ")

    val contacts = getForUrl(GET_PRISONER_CONTACT)

    assertThat(contacts.content).hasSize(3)

    val contact = contacts.content.first()
    assertThat(contact.lastName).isEqualTo("Last")
    assertThat(contact.cityCode).isEqualTo("25343")
    assertThat(contact.cityDescription).isEqualTo("Sheffield")
    assertThat(contact.countyCode).isEqualTo("S.YORKSHIRE")
    assertThat(contact.countyDescription).isEqualTo("South Yorkshire")
    assertThat(contact.countryCode).isEqualTo("ENG")
    assertThat(contact.countryDescription).isEqualTo("England")

    val minimal = contacts.content.find { it.firstName == "Minimal" } ?: fail("Couldn't find 'Minimal' contact")
    assertThat(minimal.firstName).isEqualTo("Minimal")
    assertThat(minimal.cityCode).isNull()
    assertThat(minimal.cityDescription).isNull()
    assertThat(minimal.countyCode).isNull()
    assertThat(minimal.countyDescription).isNull()
    assertThat(minimal.countryCode).isNull()
    assertThat(minimal.countryDescription).isNull()
  }

  @Test
  fun `should return phone numbers with latest first`() {
    stubPrisonSearchWithResponse("A1234BB")

    val contacts = getForUrl("/prisoner/A1234BB/contact")

    assertThat(contacts.content).hasSize(1)

    val contact = contacts.content.first()
    assertThat(contact.contactId).isEqualTo(1)
    assertThat(contact.phoneType).isEqualTo("HOME")
    assertThat(contact.phoneTypeDescription).isEqualTo("Home")
    assertThat(contact.phoneNumber).isEqualTo("01111 777777")
    assertThat(contact.extNumber).isEqualTo("+0123")
  }

  @Test
  fun `should return results for the correct page`() {
    stubPrisonSearchWithResponse("A4385DZ")

    val firstPage = webTestClient.get()
      .uri("$GET_PRISONER_CONTACT?size=2&page=0&sort=contactId")
      .headers(setAuthorisation(roles = listOf("ROLE_CONTACTS_ADMIN")))
      .exchange()
      .expectStatus()
      .isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(PrisonerContactSummaryResponse::class.java)
      .returnResult().responseBody!!

    assertThat(firstPage.content).hasSize(2)
    assertThat(firstPage.totalPages).isEqualTo(2)
    assertThat(firstPage.totalElements).isEqualTo(3)
    assertThat(firstPage.number).isEqualTo(0)

    assertThat(firstPage.content[0].contactId).isEqualTo(1)
    assertThat(firstPage.content[1].contactId).isEqualTo(10)

    val contacts = getForUrl("$GET_PRISONER_CONTACT?size=2&page=1&sort=contactId")

    assertThat(contacts.content).hasSize(1)
    assertThat(contacts.totalPages).isEqualTo(2)
    assertThat(contacts.totalElements).isEqualTo(3)
    assertThat(contacts.number).isEqualTo(1)

    assertThat(contacts.content[0].contactId).isEqualTo(18)
  }

  @Test
  fun `should return sorted correctly`() {
    stubPrisonSearchWithResponse("A4385DZ")

    val firstPage = getForUrl("$GET_PRISONER_CONTACT?sort=lastName")

    assertThat(firstPage.content).hasSize(3)
    assertThat(firstPage.totalPages).isEqualTo(1)
    assertThat(firstPage.totalElements).isEqualTo(3)
    assertThat(firstPage.number).isEqualTo(0)

    assertThat(firstPage.content[0].lastName).isEqualTo("Address")
    assertThat(firstPage.content[1].lastName).isEqualTo("Last")
    assertThat(firstPage.content[2].lastName).isEqualTo("Ten")
  }

  @Test
  fun `should return with active or inactive correctly`() {
    val prisonerNumber = "Z1234ZZ"
    stubPrisonSearchWithResponse(prisonerNumber)
    testAPIClient.createAContactWithARelationship(
      CreateContactRequest(
        lastName = "Active",
        firstName = "Contact",
        relationship = ContactRelationship(
          prisonerNumber = prisonerNumber,
          relationshipTypeCode = "S",
          relationshipToPrisonerCode = "MOT",
          isNextOfKin = false,
          isEmergencyContact = false,
          isApprovedVisitor = false,
        ),
        createdBy = "USER1",
      ),
    )
    val prisonerContactIdToDeactivate = testAPIClient.createAContactWithARelationship(
      CreateContactRequest(
        lastName = "Inactive",
        firstName = "Contact",
        relationship = ContactRelationship(
          prisonerNumber = prisonerNumber,
          relationshipTypeCode = "S",
          relationshipToPrisonerCode = "MOT",
          isNextOfKin = false,
          isEmergencyContact = false,
          isApprovedVisitor = false,
        ),
        createdBy = "USER1",
      ),
    ).createdRelationship!!.prisonerContactId
    testAPIClient.updateRelationship(
      prisonerContactIdToDeactivate,
      PatchRelationshipRequest(isRelationshipActive = JsonNullable.of(false), updatedBy = "USER1"),
    )

    val withActiveOnly = getForUrl("/prisoner/$prisonerNumber/contact?active=true")
    assertThat(withActiveOnly.content).hasSize(1)
    assertThat(withActiveOnly.content.first().lastName).isEqualTo("Active")

    val withInactiveOnly = getForUrl("/prisoner/$prisonerNumber/contact?active=false")
    assertThat(withInactiveOnly.content).hasSize(1)
    assertThat(withInactiveOnly.content.first().lastName).isEqualTo("Inactive")

    val defaultToAllStates = getForUrl("/prisoner/$prisonerNumber/contact?sort=lastName")
    assertThat(defaultToAllStates.content).hasSize(2)
    assertThat(defaultToAllStates.content.first().lastName).isEqualTo("Active")
    assertThat(defaultToAllStates.content.last().lastName).isEqualTo("Inactive")
  }

  @Test
  fun `should return with relationship type filtered correctly`() {
    val prisonerNumber = "Z4567ZZ"
    stubPrisonSearchWithResponse(prisonerNumber)
    testAPIClient.createAContactWithARelationship(
      CreateContactRequest(
        lastName = "Social",
        firstName = "Contact",
        relationship = ContactRelationship(
          prisonerNumber = prisonerNumber,
          relationshipTypeCode = "S",
          relationshipToPrisonerCode = "MOT",
          isNextOfKin = false,
          isEmergencyContact = false,
          isApprovedVisitor = false,
        ),
        createdBy = "USER1",
      ),
    )
    testAPIClient.createAContactWithARelationship(
      CreateContactRequest(
        lastName = "Official",
        firstName = "Contact",
        relationship = ContactRelationship(
          prisonerNumber = prisonerNumber,
          relationshipTypeCode = "O",
          relationshipToPrisonerCode = "DR",
          isNextOfKin = false,
          isEmergencyContact = false,
          isApprovedVisitor = false,
        ),
        createdBy = "USER1",
      ),
    )

    val withNoTypeSpecified = getForUrl("/prisoner/$prisonerNumber/contact?sort=lastName")
    assertThat(withNoTypeSpecified.content).hasSize(2)
    assertThat(withNoTypeSpecified.content.first().lastName).isEqualTo("Official")
    assertThat(withNoTypeSpecified.content.last().lastName).isEqualTo("Social")

    val onlySocial = getForUrl("/prisoner/$prisonerNumber/contact?relationshipType=S&sort=lastName")
    assertThat(onlySocial.content).hasSize(1)
    assertThat(onlySocial.content.first().lastName).isEqualTo("Social")

    val onlyOfficial = getForUrl("/prisoner/$prisonerNumber/contact?relationshipType=O&sort=lastName")
    assertThat(onlyOfficial.content).hasSize(1)
    assertThat(onlyOfficial.content.first().lastName).isEqualTo("Official")
  }

  @Test
  fun `should return with emergency contact and next of kin filtered correctly`() {
    val prisonerNumber = "X4567XX"
    stubPrisonSearchWithResponse(prisonerNumber)
    testAPIClient.createAContactWithARelationship(
      CreateContactRequest(
        lastName = "NOK Only",
        firstName = "Contact",
        relationship = ContactRelationship(
          prisonerNumber = prisonerNumber,
          relationshipTypeCode = "S",
          relationshipToPrisonerCode = "MOT",
          isNextOfKin = true,
          isEmergencyContact = false,
          isApprovedVisitor = false,
        ),
        createdBy = "USER1",
      ),
    )
    testAPIClient.createAContactWithARelationship(
      CreateContactRequest(
        lastName = "EC Only",
        firstName = "Contact",
        relationship = ContactRelationship(
          prisonerNumber = prisonerNumber,
          relationshipTypeCode = "S",
          relationshipToPrisonerCode = "FRI",
          isNextOfKin = false,
          isEmergencyContact = true,
          isApprovedVisitor = false,
        ),
        createdBy = "USER1",
      ),
    )
    testAPIClient.createAContactWithARelationship(
      CreateContactRequest(
        lastName = "NOK And EC",
        firstName = "Contact",
        relationship = ContactRelationship(
          prisonerNumber = prisonerNumber,
          relationshipTypeCode = "S",
          relationshipToPrisonerCode = "FRI",
          isNextOfKin = true,
          isEmergencyContact = true,
          isApprovedVisitor = false,
        ),
        createdBy = "USER1",
      ),
    )
    testAPIClient.createAContactWithARelationship(
      CreateContactRequest(
        lastName = "Neither",
        firstName = "Contact",
        relationship = ContactRelationship(
          prisonerNumber = prisonerNumber,
          relationshipTypeCode = "S",
          relationshipToPrisonerCode = "FRI",
          isNextOfKin = false,
          isEmergencyContact = false,
          isApprovedVisitor = false,
        ),
        createdBy = "USER1",
      ),
    )

    val withNoTypeSpecified = getForUrl("/prisoner/$prisonerNumber/contact")
    assertThat(withNoTypeSpecified.content).hasSize(4)
    assertThat(withNoTypeSpecified.content).extracting("lastName").containsExactlyInAnyOrder("NOK Only", "EC Only", "NOK And EC", "Neither")

    val nextOfKin = getForUrl("/prisoner/$prisonerNumber/contact?nextOfKin=true")
    assertThat(nextOfKin.content).hasSize(2)

    assertThat(nextOfKin.content).extracting("lastName").containsExactlyInAnyOrder("NOK Only", "NOK And EC")
    val notNextOfKin = getForUrl("/prisoner/$prisonerNumber/contact?nextOfKin=false")
    assertThat(notNextOfKin.content).hasSize(2)
    assertThat(notNextOfKin.content).extracting("lastName").containsExactlyInAnyOrder("EC Only", "Neither")

    val emergencyContacts = getForUrl("/prisoner/$prisonerNumber/contact?emergencyContact=true")
    assertThat(emergencyContacts.content).hasSize(2)
    assertThat(emergencyContacts.content).extracting("lastName").containsExactlyInAnyOrder("EC Only", "NOK And EC")

    val notEmergencyContacts = getForUrl("/prisoner/$prisonerNumber/contact?emergencyContact=false")
    assertThat(notEmergencyContacts.content).hasSize(2)
    assertThat(notEmergencyContacts.content).extracting("lastName").containsExactlyInAnyOrder("NOK Only", "Neither")

    val emergencyContactOrNextOfKin = getForUrl("/prisoner/$prisonerNumber/contact?emergencyContactOrNextOfKin=true")
    assertThat(emergencyContactOrNextOfKin.content).hasSize(3)
    assertThat(emergencyContactOrNextOfKin.content).extracting("lastName").containsExactlyInAnyOrder("EC Only", "NOK Only", "NOK And EC")

    val neitherEmergencyContactOrNextOfKin = getForUrl("/prisoner/$prisonerNumber/contact?emergencyContactOrNextOfKin=false")
    assertThat(neitherEmergencyContactOrNextOfKin.content).hasSize(1)
    assertThat(neitherEmergencyContactOrNextOfKin.content).extracting("lastName").containsExactlyInAnyOrder("Neither")

    val emergencyContactAndNextOfKin = getForUrl("/prisoner/$prisonerNumber/contact?emergencyContact=true&nextOfKin=true")
    assertThat(emergencyContactAndNextOfKin.content).hasSize(1)
    assertThat(emergencyContactAndNextOfKin.content).extracting("lastName").containsExactlyInAnyOrder("NOK And EC")

    val notEmergencyContactAndNextOfKin = getForUrl("/prisoner/$prisonerNumber/contact?emergencyContact=false&nextOfKin=false")
    assertThat(notEmergencyContactAndNextOfKin.content).hasSize(1)
    assertThat(notEmergencyContactAndNextOfKin.content).extracting("lastName").containsExactlyInAnyOrder("Neither")

    val emergencyContactAndNotNextOfKin = getForUrl("/prisoner/$prisonerNumber/contact?emergencyContact=true&nextOfKin=false")
    assertThat(emergencyContactAndNotNextOfKin.content).hasSize(1)
    assertThat(emergencyContactAndNotNextOfKin.content).extracting("lastName").containsExactlyInAnyOrder("EC Only")

    val nextOfKinAndNotEmergencyContact = getForUrl("/prisoner/$prisonerNumber/contact?emergencyContact=false&nextOfKin=true")
    assertThat(nextOfKinAndNotEmergencyContact.content).hasSize(1)
    assertThat(nextOfKinAndNotEmergencyContact.content).extracting("lastName").containsExactlyInAnyOrder("NOK Only")
  }

  @Test
  fun `should sort by date of birth with nulls as eldest`() {
    val prisonerNumber = "Z5678ZZ"
    stubPrisonSearchWithResponse(prisonerNumber)
    val relationship = ContactRelationship(
      prisonerNumber = prisonerNumber,
      relationshipTypeCode = "S",
      relationshipToPrisonerCode = "FRI",
      isNextOfKin = false,
      isEmergencyContact = false,
      isApprovedVisitor = false,
    )
    val randomLastName = RandomStringUtils.secure().nextAlphabetic(35)
    testAPIClient.createAContact(
      CreateContactRequest(
        lastName = randomLastName,
        firstName = "Youngest",
        dateOfBirth = LocalDate.of(2025, 1, 1),
        relationship = relationship,
        createdBy = "USER1",
      ),
    )
    testAPIClient.createAContact(
      CreateContactRequest(
        lastName = randomLastName,
        firstName = "Eldest",
        dateOfBirth = LocalDate.of(1990, 1, 1),
        relationship = relationship,
        createdBy = "USER1",
      ),
    )
    testAPIClient.createAContact(
      CreateContactRequest(
        lastName = randomLastName,
        firstName = "None",
        dateOfBirth = null,
        relationship = relationship,
        createdBy = "USER1",
      ),
    )

    val resultsEldestFirst = getForUrl("/prisoner/$prisonerNumber/contact?sort=dateOfBirth,asc")
    assertThat(resultsEldestFirst.content).extracting("firstName").isEqualTo(
      listOf("None", "Eldest", "Youngest"),
    )

    val resultsYoungestFirst = getForUrl("/prisoner/$prisonerNumber/contact?sort=dateOfBirth,desc")
    assertThat(resultsYoungestFirst.content).extracting("firstName").isEqualTo(
      listOf("Youngest", "Eldest", "None"),
    )
  }

  @Test
  fun `should sort by names is specified order`() {
    val prisonerNumber = "Z6789ZZ"
    stubPrisonSearchWithResponse(prisonerNumber)
    val relationship = ContactRelationship(
      prisonerNumber = prisonerNumber,
      relationshipTypeCode = "S",
      relationshipToPrisonerCode = "FRI",
      isNextOfKin = false,
      isEmergencyContact = false,
      isApprovedVisitor = false,
    )

    val randomDob = LocalDate.now().minusDays(RandomUtils.secure().randomLong(100, 2000))
    testAPIClient.createAContact(
      CreateContactRequest(
        lastName = "AA",
        firstName = "B",
        middleNames = "C",
        dateOfBirth = randomDob,
        relationship = relationship,
        createdBy = "USER1",
      ),
    )
    testAPIClient.createAContact(
      CreateContactRequest(
        lastName = "AA",
        firstName = "B",
        dateOfBirth = randomDob,
        relationship = relationship,
        createdBy = "USER1",
      ),
    )
    testAPIClient.createAContact(
      CreateContactRequest(
        lastName = "AB",
        firstName = "A",
        dateOfBirth = randomDob,
        relationship = relationship,
        createdBy = "USER1",
      ),
    )
    testAPIClient.createAContact(
      CreateContactRequest(
        lastName = "AB",
        firstName = "B",
        dateOfBirth = randomDob,
        relationship = relationship,
        createdBy = "USER1",
      ),
    )
    testAPIClient.createAContact(
      CreateContactRequest(
        lastName = "AC",
        firstName = "C",
        middleNames = "A",
        dateOfBirth = randomDob,
        relationship = relationship,
        createdBy = "USER1",
      ),
    )
    testAPIClient.createAContact(
      CreateContactRequest(
        lastName = "AC",
        firstName = "C",
        middleNames = "B",
        dateOfBirth = randomDob,
        relationship = relationship,
        createdBy = "USER1",
      ),
    )

    val expectedOrder = listOf(
      "AA, B C",
      "AA, B",
      "AB, A",
      "AB, B",
      "AC, C A",
      "AC, C B",
    )

    val ascendingName = getForUrl("/prisoner/$prisonerNumber/contact?sort=lastName,asc&sort=firstName,asc&sort=middleNames,asc")
    assertThat(ascendingName.content.map { "${it.lastName}, ${it.firstName}${if (it.middleNames != null) " ${it.middleNames}" else ""}" })
      .isEqualTo(expectedOrder)

    val descendingName = getForUrl("/prisoner/$prisonerNumber/contact?sort=lastName,desc&sort=firstName,desc&sort=middleNames,desc")
    assertThat(descendingName.content.map { "${it.lastName}, ${it.firstName}${if (it.middleNames != null) " ${it.middleNames}" else ""}" })
      .isEqualTo(expectedOrder.reversed())
  }

  @Test
  fun `secondary sort by id should work`() {
    val prisonerNumber = "Z7890ZZ"
    stubPrisonSearchWithResponse(prisonerNumber)
    val relationship = ContactRelationship(
      prisonerNumber = prisonerNumber,
      relationshipTypeCode = "S",
      relationshipToPrisonerCode = "FRI",
      isNextOfKin = false,
      isEmergencyContact = false,
      isApprovedVisitor = false,
    )
    val randomDob = LocalDate.now().minusDays(RandomUtils.secure().randomLong(100, 2000))
    val lowestId = testAPIClient.createAContact(
      CreateContactRequest(
        lastName = "AA",
        firstName = "B",
        dateOfBirth = randomDob,
        relationship = relationship,
        createdBy = "USER1",
      ),
    ).id
    val highestId = testAPIClient.createAContact(
      CreateContactRequest(
        lastName = "AA",
        firstName = "B",
        dateOfBirth = randomDob,
        relationship = relationship,
        createdBy = "USER1",
      ),
    ).id

    val expectedOrder = listOf(
      lowestId,
      highestId,
    )

    val ascendingName = getForUrl("/prisoner/$prisonerNumber/contact?sort=lastName,asc&sort=firstName,asc&sort=middleNames,asc&sort=contactId,asc")
    assertThat(ascendingName.content).extracting("contactId").isEqualTo(expectedOrder)

    val descendingName = getForUrl("/prisoner/$prisonerNumber/contact?sort=lastName,desc&sort=firstName,desc&sort=middleNames,desc&sort=contactId,desc")
    assertThat(descendingName.content).extracting("contactId").isEqualTo(expectedOrder.reversed())
  }

  private fun getForUrl(url: String): PrisonerContactSummaryResponse {
    val withActiveOnly = webTestClient.get()
      .uri(url)
      .headers(setAuthorisation(roles = listOf("ROLE_CONTACTS_ADMIN")))
      .exchange()
      .expectStatus()
      .isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(PrisonerContactSummaryResponse::class.java)
      .returnResult().responseBody!!
    return withActiveOnly
  }
}
