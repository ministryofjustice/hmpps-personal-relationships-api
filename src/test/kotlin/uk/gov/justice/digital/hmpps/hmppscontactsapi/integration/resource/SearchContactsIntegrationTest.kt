package uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.resource

import org.apache.commons.lang3.RandomStringUtils
import org.apache.commons.lang3.RandomUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.util.UriComponentsBuilder
import uk.gov.justice.digital.hmpps.hmppscontactsapi.client.organisationsapi.model.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.SecureAPIIntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.CreateContactRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.util.StubUser
import java.net.URI
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class SearchContactsIntegrationTest : SecureAPIIntegrationTestBase() {

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
    "contact/search?lastName=%00%00%27%7C%7C(SELECT%20version())%7C%7C%27,Validation failure(s): lastName must be a letter or punctuation",
    "contact/search?lastName=foo&middleNames=%00%00%27%7C%7C(SELECT%20version())%7C%7C%27,Validation failure(s): middleNames must be a letter or punctuation",
    "contact/search?lastName=foo&firstName=%00%00%27%7C%7C(SELECT%20version())%7C%7C%27,Validation failure(s): firstName must be a letter or punctuation",
    "contact/search?lastName=   &middleNames=foo,Validation failure(s): lastName must not be blank",
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

  @Test
  fun `should return empty list if the contact doesn't exist`() {
    val url = UriComponentsBuilder.fromPath("contact/search")
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
      assertThat(page.totalPages).isEqualTo(0)
    }
  }

  @ParameterizedTest
  @ValueSource(strings = ["ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__R", "ROLE_CONTACTS__RW"])
  fun `should return contacts when first, middle names and date of birth is not in request parameters`(role: String) {
    setCurrentUser(StubUser.READ_ONLY_USER.copy(roles = listOf(role)))
    val url = UriComponentsBuilder.fromPath("contact/search")
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
  fun `should get the contact with when search by first, middle, last and date of birth`() {
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
  fun `should get the contacts when searched by first name and last name with partial match`() {
    val uri = UriComponentsBuilder.fromPath("contact/search")
      .queryParam("lastName", "Las")
      .queryParam("firstName", "ck")
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
  fun `should get the contacts with no addresses associated with them when searched by last name `() {
    val uri = UriComponentsBuilder.fromPath("contact/search")
      .queryParam("lastName", "NoAddress")
      .build()
      .toUri()

    val body = testAPIClient.getSearchContactResults(uri)

    with(body!!) {
      assertThat(content).isNotEmpty()
      assertThat(content.size).isEqualTo(2)
      assertThat(page.totalElements).isEqualTo(2)
      assertThat(page.totalPages).isEqualTo(1)

      val contact = content.first()
      assertThat(contact.id).isEqualTo(16)
      assertThat(contact.firstName).isEqualTo("Liam")

      val lastContact = content.last()
      assertThat(lastContact.id).isEqualTo(17)
      assertThat(lastContact.firstName).isEqualTo("Hannah")
    }
  }

  @Test
  fun `should get contacts with a deceased date`() {
    val uri = UriComponentsBuilder.fromPath("contact/search")
      .queryParam("lastName", "Dead")
      .build()
      .toUri()

    val body = testAPIClient.getSearchContactResults(uri)!!
    assertThat(body.page.totalElements).isEqualTo(1)
    assertThat(body.content[0].deceasedDate).isEqualTo(LocalDate.of(2000, 1, 1))
  }

  @Test
  fun `should get the contacts with minimal addresses associated with them when searched by last name`() {
    val uri = UriComponentsBuilder.fromPath("contact/search")
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
  fun `should sort by date of birth with nulls as eldest`() {
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
      UriComponentsBuilder.fromPath("contact/search")
        .queryParam("lastName", randomLastName)
        .queryParam("sort", "dateOfBirth,asc")
        .build()
        .toUri(),
    )!!
    assertThat(resultsEldestFirst.content).extracting("firstName").isEqualTo(
      listOf("None", "Eldest", "Youngest"),
    )

    val resultsYoungestFirst = testAPIClient.getSearchContactResults(
      UriComponentsBuilder.fromPath("contact/search")
        .queryParam("lastName", randomLastName)
        .queryParam("sort", "dateOfBirth,desc")
        .build()
        .toUri(),
    )!!
    assertThat(resultsYoungestFirst.content).extracting("firstName").isEqualTo(
      listOf("Youngest", "Eldest", "None"),
    )
  }

  @Test
  fun `should sort by names is specified order`() {
    val randomDob = LocalDate.now().minusDays(RandomUtils.secure().randomLong(100, 2000))
    doWithTemporaryWritePermission {
      testAPIClient.createAContact(
        CreateContactRequest(
          lastName = "AA",
          firstName = "B",
          middleNames = "C",
          dateOfBirth = randomDob,
        ),
      )
      testAPIClient.createAContact(
        CreateContactRequest(
          lastName = "AA",
          firstName = "B",
          dateOfBirth = randomDob,
        ),
      )
      testAPIClient.createAContact(
        CreateContactRequest(
          lastName = "AB",
          firstName = "A",
          dateOfBirth = randomDob,
        ),
      )
      testAPIClient.createAContact(
        CreateContactRequest(
          lastName = "AB",
          firstName = "B",
          dateOfBirth = randomDob,
        ),
      )
      testAPIClient.createAContact(
        CreateContactRequest(
          lastName = "AC",
          firstName = "C",
          middleNames = "A",
          dateOfBirth = randomDob,
        ),
      )
      testAPIClient.createAContact(
        CreateContactRequest(
          lastName = "AC",
          firstName = "C",
          middleNames = "B",
          dateOfBirth = randomDob,
        ),
      )
    }
    val expectedOrder = listOf(
      "AA, B C",
      "AA, B",
      "AB, A",
      "AB, B",
      "AC, C A",
      "AC, C B",
    )

    val ascendingName = testAPIClient.getSearchContactResults(
      UriComponentsBuilder.fromPath("contact/search")
        .queryParam("lastName", "A")
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
      UriComponentsBuilder.fromPath("contact/search")
        .queryParam("lastName", "A")
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
          lastName = "AA",
          firstName = "B",
          dateOfBirth = randomDob,
        ),
      ).id
      val highestId = testAPIClient.createAContact(
        CreateContactRequest(
          lastName = "AA",
          firstName = "B",
          dateOfBirth = randomDob,
        ),
      ).id

      listOf(
        lowestId,
        highestId,
      )
    }

    val ascendingName = testAPIClient.getSearchContactResults(
      UriComponentsBuilder.fromPath("contact/search")
        .queryParam("lastName", "A")
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
      UriComponentsBuilder.fromPath("contact/search")
        .queryParam("lastName", "A")
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
  fun `should get bad request when searched with empty last name`() {
    val uri = UriComponentsBuilder.fromPath("contact/search")
      .queryParam("lastName", "")
      .queryParam("firstName", "Jack")
      .build()
      .toUri()

    val errors = testAPIClient.getBadResponseErrors(uri)

    assertThat(errors.userMessage).isEqualTo("Validation failure(s): lastName must not be blank")
  }

  @Test
  fun `should get bad request when searched with invalid date format for date of birth`() {
    val uri: URI = UriComponentsBuilder.fromPath("contact/search")
      .queryParam("lastName", "Eleven")
      .queryParam("dateOfBirth", "=01-10-2001")
      .build()
      .toUri()

    val errors = testAPIClient.getBadResponseErrors(uri)

    assertThat(errors.userMessage).contains("Validation failure(s): dateOfBirth Failed to convert value of type 'java.lang.String' to required type 'java.time.LocalDate';")
  }

  @Test
  fun `should get bad request when searched with no last name`() {
    val uri: URI = UriComponentsBuilder.fromPath("contact/search")
      .build()
      .toUri()

    val errors = testAPIClient.getBadResponseErrors(uri)

    assertThat(errors.userMessage).contains("Validation failure(s): Parameter specified as non-null is null: ")
  }

  companion object {
    private val CONTACT_SEARCH_URL = UriComponentsBuilder.fromPath("contact/search")
      .queryParam("lastName", "Last")
      .queryParam("firstName", "Jack")
      .queryParam("middleNames", "Middle")
      .queryParam("dateOfBirth", "21/11/2000")
      .build()
      .toUri()
  }
}
