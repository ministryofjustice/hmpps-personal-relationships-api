package uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.util.UriComponentsBuilder
import uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.SecureAPIIntegrationTestBase
import java.net.URI

class SearchContactsIntegrationTest : SecureAPIIntegrationTestBase() {

  override val allowedRoles: Set<String> = setOf("ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__RW", "ROLE_CONTACTS__R")

  override fun baseRequestBuilder(): WebTestClient.RequestHeadersSpec<*> = webTestClient.get()
    .uri(CONTACT_SEARCH_URL.toString())
    .accept(MediaType.APPLICATION_JSON)

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
      assertThat(totalElements).isEqualTo(0)
      assertThat(totalPages).isEqualTo(0)
    }
  }

  @ParameterizedTest
  @ValueSource(strings = ["ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__R", "ROLE_CONTACTS__RW"])
  fun `should return contacts when first, middle names and date of birth is not in request parameters`(role: String) {
    val url = UriComponentsBuilder.fromPath("contact/search")
      .queryParam("lastName", "Twelve")
      .build()
      .toUri()

    val body = testAPIClient.getSearchContactResults(url, role)

    with(body!!) {
      assertThat(content).isNotEmpty()
      assertThat(content.size).isEqualTo(1)
      assertThat(totalElements).isEqualTo(1)

      assertThat(totalPages).isEqualTo(1)

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
      assertThat(contact.postCode).isEqualTo("S2 3LK")
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
      assertThat(totalElements).isEqualTo(1)
      assertThat(pageable.pageNumber).isEqualTo(0)
      assertThat(pageable.pageSize).isEqualTo(10)
      assertThat(pageable.sort.sorted).isEqualTo(true)
      assertThat(sort.sorted).isEqualTo(true)
      assertThat(first).isEqualTo(true)
      assertThat(size).isEqualTo(10)
      assertThat(number).isEqualTo(0)
      assertThat(totalPages).isEqualTo(1)

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
      assertThat(contact.postCode).isEqualTo("S2 3LK")
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
      assertThat(totalElements).isEqualTo(1)

      assertThat(totalPages).isEqualTo(1)

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
      assertThat(contact.postCode).isEqualTo("S2 3LK")
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
      assertThat(totalElements).isEqualTo(2)

      assertThat(totalPages).isEqualTo(1)

      val contact = content.first()
      assertThat(contact.id).isEqualTo(16)
      assertThat(contact.firstName).isEqualTo("Liam")

      val lastContact = content.last()
      assertThat(lastContact.id).isEqualTo(17)
      assertThat(lastContact.firstName).isEqualTo("Hannah")
    }
  }

  @Test
  fun `should get the contacts with minimal addresses associated with them when searched by last name `() {
    val uri = UriComponentsBuilder.fromPath("contact/search")
      .queryParam("lastName", "Address")
      .queryParam("firstName", "Minimal")
      .build()
      .toUri()

    val body = testAPIClient.getSearchContactResults(uri)

    with(body!!) {
      assertThat(content).isNotEmpty()
      assertThat(content.size).isEqualTo(1)
      assertThat(totalElements).isEqualTo(1)

      assertThat(totalPages).isEqualTo(1)

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
      assertThat(contact.postCode).isNull()
      assertThat(contact.countryCode).isNull()
      assertThat(contact.countryDescription).isNull()
    }
  }

  @Test
  fun `should get bad request when searched with empty last name`() {
    val uri = UriComponentsBuilder.fromPath("contact/search")
      .queryParam("lastName", "")
      .queryParam("firstName", "Jack")
      .build()
      .toUri()

    val errors = testAPIClient.getBadResponseErrors(uri)

    assertThat(errors.userMessage).isEqualTo("Validation failure(s): Last Name cannot be blank.")
  }

  @Test
  fun `should get bad request when searched with invalid date format for date of birth`() {
    val uri: URI = UriComponentsBuilder.fromPath("contact/search")
      .queryParam("lastName", "Eleven")
      .queryParam("dateOfBirth", "=01-10-2001")
      .build()
      .toUri()

    val errors = testAPIClient.getBadResponseErrors(uri)

    assertThat(errors.userMessage).contains("Validation failure(s): Failed to convert value of type 'java.lang.String' to required type 'java.time.LocalDate';")
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
