package uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.resource

import org.apache.commons.lang3.RandomStringUtils
import org.apache.commons.lang3.RandomUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import org.openapitools.jackson.nullable.JsonNullable
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.util.UriComponentsBuilder
import uk.gov.justice.digital.hmpps.hmppscontactsapi.client.organisationsapi.model.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.SecureAPIIntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.AddContactRelationshipRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.ContactRelationship
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.CreateContactRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.PatchRelationshipRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.ExistingRelationshipToPrisoner
import uk.gov.justice.digital.hmpps.hmppscontactsapi.util.StubUser
import java.net.URI
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class SearchContactsV2IntegrationTest : SecureAPIIntegrationTestBase() {

  @BeforeEach
  fun setUp() {
    setCurrentUser(StubUser.READ_ONLY_USER)
  }

  override val allowedRoles: Set<String> = setOf("ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__RW", "ROLE_CONTACTS__R")

  override fun baseRequestBuilder(): WebTestClient.RequestHeadersSpec<*> = webTestClient.get()
    .uri(CONTACT_SEARCH_URL.toString())
    .accept(MediaType.APPLICATION_JSON)

  @ParameterizedTest
  @CsvSource(
    "contact/searchV2?lastName=%00%00%27%7C%7C(SELECT%20version())%7C%7C%27,Validation failure: searchContactsV2.lastName: must be a letter or punctuation",
    "contact/searchV2?lastName=foo&middleNames=%00%00%27%7C%7C(SELECT%20version())%7C%7C%27,Validation failure: searchContactsV2.middleNames: must be a letter or punctuation",
    "contact/searchV2?lastName=foo&firstName=%00%00%27%7C%7C(SELECT%20version())%7C%7C%27,Validation failure: searchContactsV2.firstName: must be a letter or punctuation",
    "contact/searchV2?includeAnyExistingRelationshipsToPrisoner=A-B-1&lastName=foo,Validation failure: searchContactsV2.includeAnyExistingRelationshipsToPrisoner: must contain only letters or numbers",
    "contact/searchV2?dateOfBirth=30/12/2150&lastName=foo,Validation failure: searchContactsV2.dateOfBirth: The date of birth must be in the past",
  )
  fun `should return bad request if the query params are invalid`(url: String, expectedError: String) {
    val body = webTestClient.get()
      .uri(url)
      .accept(MediaType.APPLICATION_JSON)
      .headers(testAPIClient.setAuthorisationUsingCurrentUser())
      .exchange()
      .expectStatus().isBadRequest
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody
    assertThat(body?.userMessage).isEqualTo(expectedError)
  }

  // This always finds one row!
  @Test
  fun `should return empty list if no contacts are found`() {
    val url = UriComponentsBuilder.fromPath("contact/searchV2")
      .queryParam("lastName", "NEW")
      .queryParam("firstName", "NEW")
      .queryParam("middleName", "Middle")
      .queryParam("dateOfBirth", "21/11/2000")
      .build()
      .toUri()

    val body = testAPIClient.getSearchContactResults(url)

    with(body!!) {
      assertThat(content).isEmpty()
      assertThat(page.totalElements).isEqualTo(0)
    }
  }

  @ParameterizedTest
  @ValueSource(strings = ["ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__R", "ROLE_CONTACTS__RW"])
  fun `should return contacts when only the last name is supplied`(role: String) {
    setCurrentUser(StubUser.READ_ONLY_USER.copy(roles = listOf(role)))
    val url = UriComponentsBuilder.fromPath("contact/searchV2")
      .queryParam("lastName", "Twelve")
      .build()
      .toUri()

    val body = testAPIClient.getSearchContactResults(url)

    with(body!!) {
      assertThat(content).isNotEmpty()
      assertThat(content.size).isEqualTo(1)
      assertThat(page.totalElements).isEqualTo(1)

      assertThat(page.totalPages).isEqualTo(1)

      val contact = content.first()
      assertThat(contact.id).isEqualTo(12)
      assertThat(contact.firstName).isEqualTo("Jane")
      assertThat(contact.lastName).isEqualTo("Twelve")
      assertThat(contact.middleNames).isEqualTo("Middle")
      assertThat(contact.dateOfBirth).isEqualTo("2000-11-26")
      assertThat(contact.createdBy).isEqualTo("TIM")
      assertThat(contact.createdTime).isInThePast()
      assertThat(contact.flat).isEqualTo("Flat 3b")
      assertThat(contact.property).isEqualTo("42")
      assertThat(contact.street).isEqualTo("Acacia Avenue")
      assertThat(contact.area).isEqualTo("Bunting")
      assertThat(contact.cityCode).isEqualTo("25343")
      assertThat(contact.cityDescription).isEqualTo("Sheffield")
      assertThat(contact.countyCode).isEqualTo("S.YORKSHIRE")
      assertThat(contact.countyDescription).isEqualTo("South Yorkshire")
      assertThat(contact.postcode).isEqualTo("S2 3LK")
      assertThat(contact.countryCode).isEqualTo("ENG")
      assertThat(contact.countryDescription).isEqualTo("England")
      assertThat(contact.mailAddress).isFalse()
      assertThat(contact.noFixedAddress).isFalse()
      assertThat(contact.startDate).isNull()
      assertThat(contact.startDate).isNull()
    }
  }

  @Test
  fun `should find contacts by date of birth and names matching`() {
    val body = testAPIClient.getSearchContactResults(CONTACT_SEARCH_URL)

    with(body!!) {
      assertThat(content).isNotEmpty()
      assertThat(content.size).isEqualTo(1)
      assertThat(page.totalElements).isEqualTo(1)
      assertThat(page.number).isEqualTo(0)
      assertThat(page.size).isEqualTo(10)
      assertThat(page.totalPages).isEqualTo(1)

      val contact = content.first()
      assertThat(contact.id).isEqualTo(1)
      assertThat(contact.firstName).isEqualTo("Jack")
      assertThat(contact.lastName).isEqualTo("Last")
      assertThat(contact.middleNames).isEqualTo("Middle")
      assertThat(contact.dateOfBirth).isEqualTo("2000-11-21")
      assertThat(contact.createdBy).isEqualTo("TIM")
      assertThat(contact.createdTime).isInThePast()
      assertThat(contact.property).isEqualTo("24")
      assertThat(contact.street).isEqualTo("Acacia Avenue")
      assertThat(contact.area).isEqualTo("Bunting")
      assertThat(contact.cityCode).isEqualTo("25343")
      assertThat(contact.cityDescription).isEqualTo("Sheffield")
      assertThat(contact.countyCode).isEqualTo("S.YORKSHIRE")
      assertThat(contact.countyDescription).isEqualTo("South Yorkshire")
      assertThat(contact.postcode).isEqualTo("S2 3LK")
      assertThat(contact.countryCode).isEqualTo("ENG")
      assertThat(contact.countryDescription).isEqualTo("England")
      assertThat(contact.mailAddress).isFalse()
      assertThat(contact.noFixedAddress).isFalse()
      assertThat(contact.comments).isEqualTo("Some comments")
      assertThat(contact.startDate).isNull()
      assertThat(contact.startDate).isNull()
    }
  }

  @Test
  fun `should find contacts by names matching first and last`() {
    val uri = UriComponentsBuilder.fromPath("contact/searchV2")
      .queryParam("lastName", "Las")
      .queryParam("firstName", "ack")
      .build()
      .toUri()

    val body = testAPIClient.getSearchContactResults(uri)

    with(body!!) {
      assertThat(content).isNotEmpty()
      assertThat(content.size).isEqualTo(1)
      assertThat(page.totalElements).isEqualTo(1)
      assertThat(page.totalPages).isEqualTo(1)

      val contact = content.first()
      assertThat(contact.id).isEqualTo(1)
      assertThat(contact.firstName).isEqualTo("Jack")
      assertThat(contact.lastName).isEqualTo("Last")
      assertThat(contact.middleNames).isEqualTo("Middle")
      assertThat(contact.dateOfBirth).isEqualTo("2000-11-21")
      assertThat(contact.createdBy).isEqualTo("TIM")
      assertThat(contact.createdTime).isInThePast()
      assertThat(contact.property).isEqualTo("24")
      assertThat(contact.street).isEqualTo("Acacia Avenue")
      assertThat(contact.area).isEqualTo("Bunting")
      assertThat(contact.cityCode).isEqualTo("25343")
      assertThat(contact.cityDescription).isEqualTo("Sheffield")
      assertThat(contact.countyCode).isEqualTo("S.YORKSHIRE")
      assertThat(contact.countyDescription).isEqualTo("South Yorkshire")
      assertThat(contact.postcode).isEqualTo("S2 3LK")
      assertThat(contact.countryCode).isEqualTo("ENG")
      assertThat(contact.countryDescription).isEqualTo("England")
      assertThat(contact.mailAddress).isFalse()
      assertThat(contact.noFixedAddress).isFalse()
      assertThat(contact.startDate).isNull()
      assertThat(contact.startDate).isNull()
    }
  }

  @Test
  fun `should find contacts by specific contact ID`() {
    val uri = UriComponentsBuilder.fromPath("contact/searchV2")
      .queryParam("contactId", "11041")
      .build()
      .toUri()

    val body = testAPIClient.getSearchContactResults(uri)

    with(body!!) {
      assertThat(content).isNotEmpty()
      assertThat(content.size).isEqualTo(1)
      assertThat(page.totalElements).isEqualTo(1)
      assertThat(page.totalPages).isEqualTo(1)

      assertThat(content).extracting("id").containsAll(listOf(11041L))
      assertThat(content).extracting("lastName").containsAll(listOf("NELSINWOOD"))
    }
  }

  @ParameterizedTest
  @CsvSource(
    "Smith,John,Jon|Smith|1980-01-01;John|Smithe|1980-01-01",
    "Brown,Stephen,Stephen|Brown|1985-05-15;Steven|Browne|1983-09-10",
    "Lewis,Catherine,Catherine|Lewis|1990-07-22",
    "Macdonald,Bryan,Bryan|Macdonald|1977-03-12;Brian|McDonald|1976-08-05",
    "Green,Geoffrey,Geoffrey|Green|1988-04-18",
    "Khan,Mohammed,Mohammed|Khan|1993-12-01;Muhammad|Kahn|1994-01-15",
    "Johnson,Sara,Sara|Johnson|1995-05-25;Sarah|Johnsen|1995-06-01",
    "Taylor,Nicolas,Nicolas|Taylor|1982-03-03;Nicholas|Tailor|1982-04-04",
    "King,Isabel,Isabel|King|2000-01-01;Isabelle|King|2000-01-01;Isobel|King|2000-01-01",
    "Black,Marc,Marc|Black|1981-09-09;Mark|Blake|1981-10-10",
  )
  fun `should find contacts by first and last names with sounds like option`(
    lastName: String,
    firstName: String,
    expected: String,
  ) {
    val uri = UriComponentsBuilder.fromPath("contact/searchV2")
      .queryParam("lastName", lastName)
      .queryParam("firstName", firstName)
      .queryParam("soundsLike", true)
      .build()
      .toUri()

    val body = testAPIClient.getSearchContactResults(uri)!!

    val expectedTriples = expected.split(";").map {
      val parts = it.split("|")
      Triple(parts[0], parts[1], parts.getOrNull(2)?.takeIf { s -> s.isNotBlank() }?.let(LocalDate::parse))
    }

    with(body) {
      assertThat(content).isNotEmpty()
      assertThat(content.size).isEqualTo(expectedTriples.size)
      assertThat(page.totalElements).isEqualTo(expectedTriples.size.toLong())
      assertThat(page.totalPages).isEqualTo(1L)

      val actualTriples = content.map { Triple(it.firstName, it.lastName, it.dateOfBirth) }
      assertThat(actualTriples).containsExactlyInAnyOrderElementsOf(expectedTriples)
    }
  }

  @Test
  fun `should find contacts by last name when no addresses are associated with them`() {
    val uri = UriComponentsBuilder.fromPath("contact/searchV2")
      .queryParam("lastName", "NoAddress")
      .build()
      .toUri()

    val body = testAPIClient.getSearchContactResults(uri)

    with(body!!) {
      assertThat(content).isNotEmpty()
      assertThat(content.size).isEqualTo(2)
      assertThat(page.totalElements).isEqualTo(2)
      assertThat(page.totalPages).isEqualTo(1)
      assertThat(content).extracting("firstName").containsAll(listOf("Liam", "Hannah"))
    }
  }

  @Test
  fun `should find contacts by last name even with a deceased date`() {
    val uri = UriComponentsBuilder.fromPath("contact/searchV2")
      .queryParam("lastName", "Dead")
      .build()
      .toUri()

    val body = testAPIClient.getSearchContactResults(uri)!!
    assertThat(body.page.totalElements).isEqualTo(1)
    assertThat(body.content[0].deceasedDate).isEqualTo(LocalDate.of(2000, 1, 1))
  }

  @Test
  fun `should find contacts by last name with minimal address data associated with them`() {
    val uri = UriComponentsBuilder.fromPath("contact/searchV2")
      .queryParam("lastName", "Address")
      .queryParam("firstName", "Minimal")
      .build()
      .toUri()

    val body = testAPIClient.getSearchContactResults(uri)

    with(body!!) {
      assertThat(content).isNotEmpty()
      assertThat(content.size).isEqualTo(1)
      assertThat(page.totalElements).isEqualTo(1)
      assertThat(page.totalPages).isEqualTo(1)

      val contact = content.first()
      assertThat(contact.id).isEqualTo(18)
      assertThat(contact.firstName).isEqualTo("Minimal")
      assertThat(contact.lastName).isEqualTo("Address")
      assertThat(contact.existingRelationships).isNull() // as it was not requested

      assertThat(contact.property).isNull()
      assertThat(contact.street).isNull()
      assertThat(contact.area).isNull()
      assertThat(contact.cityCode).isNull()
      assertThat(contact.cityDescription).isNull()
      assertThat(contact.countyCode).isNull()
      assertThat(contact.countyDescription).isNull()
      assertThat(contact.postcode).isNull()
      assertThat(contact.countryCode).isNull()
      assertThat(contact.countryDescription).isNull()
    }
  }

  @Test
  fun `should sort results by date of birth both ascending and descending`() {
    val randomLastName = RandomStringUtils.secure().nextAlphabetic(35)
    doWithTemporaryWritePermission {
      testAPIClient.createAContact(
        CreateContactRequest(
          lastName = randomLastName,
          firstName = "Youngest",
          dateOfBirth = LocalDate.of(2025, 1, 1),
        ),
      )
      testAPIClient.createAContact(
        CreateContactRequest(
          lastName = randomLastName,
          firstName = "Eldest",
          dateOfBirth = LocalDate.of(1990, 1, 1),
        ),
      )
      testAPIClient.createAContact(
        CreateContactRequest(
          lastName = randomLastName,
          firstName = "None",
          dateOfBirth = null,
        ),
      )
    }

    val resultsEldestFirst = testAPIClient.getSearchContactResults(
      UriComponentsBuilder.fromPath("contact/searchV2")
        .queryParam("lastName", randomLastName)
        .queryParam("sort", "dateOfBirth,asc")
        .build()
        .toUri(),
    )!!

    assertThat(resultsEldestFirst.content).extracting("firstName").isEqualTo(listOf("Eldest", "Youngest", "None"))

    val resultsYoungestFirst = testAPIClient.getSearchContactResults(
      UriComponentsBuilder.fromPath("contact/searchV2")
        .queryParam("lastName", randomLastName)
        .queryParam("sort", "dateOfBirth,desc")
        .build()
        .toUri(),
    )!!

    assertThat(resultsYoungestFirst.content).extracting("firstName").isEqualTo(listOf("None", "Youngest", "Eldest"))
  }

  @Test
  fun `should sort by last name, first name then middle names`() {
    val randomDob = LocalDate.now().minusDays(RandomUtils.secure().randomLong(100, 2000))
    doWithTemporaryWritePermission {
      testAPIClient.createAContact(
        CreateContactRequest(
          lastName = "AAA",
          firstName = "B",
          middleNames = "C",
          dateOfBirth = randomDob,
        ),
      )
      testAPIClient.createAContact(
        CreateContactRequest(
          lastName = "AAA",
          firstName = "B",
          dateOfBirth = randomDob,
        ),
      )
      testAPIClient.createAContact(
        CreateContactRequest(
          lastName = "AAB",
          firstName = "A",
          dateOfBirth = randomDob,
        ),
      )
      testAPIClient.createAContact(
        CreateContactRequest(
          lastName = "AAB",
          firstName = "B",
          dateOfBirth = randomDob,
        ),
      )
      testAPIClient.createAContact(
        CreateContactRequest(
          lastName = "AAC",
          firstName = "C",
          middleNames = "A",
          dateOfBirth = randomDob,
        ),
      )
      testAPIClient.createAContact(
        CreateContactRequest(
          lastName = "AAC",
          firstName = "C",
          middleNames = "B",
          dateOfBirth = randomDob,
        ),
      )
    }
    val expectedOrder = listOf(
      "AAA, B C",
      "AAA, B",
      "AAB, A",
      "AAB, B",
      "AAC, C A",
      "AAC, C B",
    )

    val ascendingName = testAPIClient.getSearchContactResults(
      UriComponentsBuilder.fromPath("contact/searchV2")
        .queryParam("lastName", "AA")
        .queryParam("dateOfBirth", randomDob.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
        .queryParam("sort", "lastName,asc")
        .queryParam("sort", "firstName,asc")
        .queryParam("sort", "middleNames,asc")
        .build()
        .toUri(),
    )!!

    assertThat(ascendingName.content.map { "${it.lastName}, ${it.firstName}${if (it.middleNames != null) " ${it.middleNames}" else ""}" })
      .isEqualTo(expectedOrder)

    val descendingName = testAPIClient.getSearchContactResults(
      UriComponentsBuilder.fromPath("contact/searchV2")
        .queryParam("lastName", "AA")
        .queryParam("dateOfBirth", randomDob.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
        .queryParam("sort", "lastName,desc")
        .queryParam("sort", "firstName,desc")
        .queryParam("sort", "middleNames,desc")
        .build()
        .toUri(),
    )!!

    assertThat(descendingName.content.map { "${it.lastName}, ${it.firstName}${if (it.middleNames != null) " ${it.middleNames}" else ""}" })
      .isEqualTo(expectedOrder.reversed())
  }

  @Test
  fun `secondary sort by id should work`() {
    val randomDob = LocalDate.now().minusDays(RandomUtils.secure().randomLong(100, 2000))
    val expectedOrder = doWithTemporaryWritePermission {
      val lowestId = testAPIClient.createAContact(
        CreateContactRequest(
          lastName = "AAA",
          firstName = "B",
          dateOfBirth = randomDob,
        ),
      ).id

      val highestId = testAPIClient.createAContact(
        CreateContactRequest(
          lastName = "AAA",
          firstName = "B",
          dateOfBirth = randomDob,
        ),
      ).id

      listOf(lowestId, highestId)
    }

    val ascendingName = testAPIClient.getSearchContactResults(
      UriComponentsBuilder.fromPath("contact/searchV2")
        .queryParam("lastName", "AA")
        .queryParam("dateOfBirth", randomDob.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
        .queryParam("sort", "lastName,asc")
        .queryParam("sort", "firstName,asc")
        .queryParam("sort", "middleNames,asc")
        .queryParam("sort", "id,asc")
        .build()
        .toUri(),
    )!!

    assertThat(ascendingName.content).extracting("id").isEqualTo(expectedOrder)

    val descendingName = testAPIClient.getSearchContactResults(
      UriComponentsBuilder.fromPath("contact/searchV2")
        .queryParam("lastName", "AA")
        .queryParam("dateOfBirth", randomDob.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
        .queryParam("sort", "lastName,desc")
        .queryParam("sort", "firstName,desc")
        .queryParam("sort", "middleNames,desc")
        .queryParam("sort", "id,desc")
        .build()
        .toUri(),
    )!!

    assertThat(descendingName.content).extracting("id").isEqualTo(expectedOrder.reversed())
  }

  @Test
  fun `should get bad request when searched with invalid date format for date of birth`() {
    val uri: URI = UriComponentsBuilder.fromPath("contact/searchV2")
      .queryParam("lastName", "Eleven")
      .queryParam("dateOfBirth", "=01-10-2001")
      .build()
      .toUri()

    val errors = testAPIClient.getBadResponseErrors(uri)

    assertThat(errors.developerMessage).contains("Method parameter 'dateOfBirth': Failed to convert value of type 'java.lang.String' to required type 'java.time.LocalDate'")
  }

  @Test
  fun `should get bad request when searched with no last name`() {
    val uri: URI = UriComponentsBuilder.fromPath("contact/searchV2")
      .build()
      .toUri()

    val errors = testAPIClient.getBadResponseErrors(uri)

    assertThat(errors.developerMessage).contains("Either contact ID, date of birth or a full or partial name must be provided for contact searches")
  }

  @Test
  fun `should return existing relationships to prisoner if requested`() {
    val prisonerNumber = "A1234BC"
    stubPrisonSearchWithResponse(prisonerNumber)
    val randomDob = LocalDate.now().minusDays(RandomUtils.secure().randomLong(100, 2000))
    val contactIdToExpectedRelationships: Map<Long, List<ExistingRelationshipToPrisoner>> =
      doWithTemporaryWritePermission {
        val contactSingle = testAPIClient.createAContactWithARelationship(
          CreateContactRequest(
            lastName = "ABCDEFG",
            firstName = "Single Relationship",
            dateOfBirth = randomDob,
            relationship = ContactRelationship(
              prisonerNumber = prisonerNumber,
              relationshipTypeCode = "S",
              relationshipToPrisonerCode = "FRI",
              isNextOfKin = false,
              isEmergencyContact = false,
              isApprovedVisitor = false,
              comments = null,
            ),
          ),
        )
        val contactMultiple = testAPIClient.createAContactWithARelationship(
          CreateContactRequest(
            lastName = "ABCDEFG",
            firstName = "Multiple Relationships",
            dateOfBirth = randomDob,
            relationship = ContactRelationship(
              prisonerNumber = prisonerNumber,
              relationshipTypeCode = "S",
              relationshipToPrisonerCode = "MOT",
              isNextOfKin = false,
              isEmergencyContact = false,
              isApprovedVisitor = false,
              comments = null,
            ),
          ),
        )
        // ensure includes inactive relationships
        testAPIClient.updateRelationship(
          contactMultiple.createdRelationship!!.prisonerContactId,
          PatchRelationshipRequest(isRelationshipActive = JsonNullable.of(false)),
        )

        val contactMultipleSecondRelationship = testAPIClient.addAContactRelationship(
          AddContactRelationshipRequest(
            contactId = contactMultiple.createdContact.id,
            relationship = ContactRelationship(
              prisonerNumber = prisonerNumber,
              relationshipTypeCode = "O",
              relationshipToPrisonerCode = "DR",
              isNextOfKin = false,
              isEmergencyContact = false,
              isApprovedVisitor = false,
              comments = null,
            ),
          ),
        )
        val contactNone = testAPIClient.createAContact(
          CreateContactRequest(
            lastName = "ABCDEFG",
            firstName = "No Relationships",
            dateOfBirth = randomDob,
          ),
        )
        mapOf(
          contactSingle.createdContact.id to listOf(
            ExistingRelationshipToPrisoner(
              prisonerContactId = contactSingle.createdRelationship!!.prisonerContactId,
              relationshipTypeCode = "S",
              relationshipTypeDescription = "Social",
              relationshipToPrisonerCode = "FRI",
              relationshipToPrisonerDescription = "Friend",
              isRelationshipActive = true,
            ),
          ),
          contactMultiple.createdContact.id to listOf(
            ExistingRelationshipToPrisoner(
              prisonerContactId = contactMultiple.createdRelationship!!.prisonerContactId,
              relationshipTypeCode = "S",
              relationshipTypeDescription = "Social",
              relationshipToPrisonerCode = "MOT",
              relationshipToPrisonerDescription = "Mother",
              isRelationshipActive = false,
            ),
            ExistingRelationshipToPrisoner(
              prisonerContactId = contactMultipleSecondRelationship.prisonerContactId,
              relationshipTypeCode = "O",
              relationshipTypeDescription = "Official",
              relationshipToPrisonerCode = "DR",
              relationshipToPrisonerDescription = "Doctor",
              isRelationshipActive = true,
            ),
          ),
          contactNone.id to emptyList(),
        )
      }

    val results = testAPIClient.getSearchContactResults(
      UriComponentsBuilder.fromPath("contact/searchV2")
        .queryParam("lastName", "ABCDEFG")
        .queryParam("dateOfBirth", randomDob.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
        .queryParam("includeAnyExistingRelationshipsToPrisoner", prisonerNumber)
        .build()
        .toUri(),
    )!!

    assertThat(results.content).hasSize(3)
    assertThat(contactIdToExpectedRelationships.keys).isEqualTo(results.content.map { it.id }.toSet())
    contactIdToExpectedRelationships.forEach { (contactId, expectedRelationships) ->
      val contactResult = results.content.find { it.id == contactId }
      assertThat(contactResult).isNotNull
      assertThat(contactResult!!.existingRelationships).containsExactlyInAnyOrderElementsOf(expectedRelationships)
    }
  }

  companion object {
    private val CONTACT_SEARCH_URL = UriComponentsBuilder.fromPath("contact/searchV2")
      .queryParam("lastName", "Last")
      .queryParam("firstName", "Jack")
      .queryParam("middleNames", "Middle")
      .queryParam("dateOfBirth", "21/11/2000")
      .build()
      .toUri()
  }
}
