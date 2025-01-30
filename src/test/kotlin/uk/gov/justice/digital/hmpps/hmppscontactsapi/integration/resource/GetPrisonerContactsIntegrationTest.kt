package uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.SecureAPIIntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.helper.TestAPIClient.PrisonerContactSummaryResponse

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

    val contacts = webTestClient.get()
      .uri(GET_PRISONER_CONTACT)
      .headers(setAuthorisation(roles = listOf("ROLE_CONTACTS_ADMIN")))
      .exchange()
      .expectStatus()
      .isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(PrisonerContactSummaryResponse::class.java)
      .returnResult().responseBody!!

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

    val contacts = webTestClient.get()
      .uri("/prisoner/A1234BB/contact")
      .headers(setAuthorisation(roles = listOf("ROLE_CONTACTS_ADMIN")))
      .exchange()
      .expectStatus()
      .isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(PrisonerContactSummaryResponse::class.java)
      .returnResult().responseBody!!

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

    val contacts = webTestClient.get()
      .uri("$GET_PRISONER_CONTACT?size=2&page=1&sort=contactId")
      .headers(setAuthorisation(roles = listOf("ROLE_CONTACTS_ADMIN")))
      .exchange()
      .expectStatus()
      .isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(PrisonerContactSummaryResponse::class.java)
      .returnResult().responseBody!!

    assertThat(contacts.content).hasSize(1)
    assertThat(contacts.totalPages).isEqualTo(2)
    assertThat(contacts.totalElements).isEqualTo(3)
    assertThat(contacts.number).isEqualTo(1)

    assertThat(contacts.content[0].contactId).isEqualTo(18)
  }

  @Test
  fun `should return sorted correctly`() {
    stubPrisonSearchWithResponse("A4385DZ")

    val firstPage = webTestClient.get()
      .uri("$GET_PRISONER_CONTACT?sort=lastName")
      .headers(setAuthorisation(roles = listOf("ROLE_CONTACTS_ADMIN")))
      .exchange()
      .expectStatus()
      .isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(PrisonerContactSummaryResponse::class.java)
      .returnResult().responseBody!!

    assertThat(firstPage.content).hasSize(3)
    assertThat(firstPage.totalPages).isEqualTo(1)
    assertThat(firstPage.totalElements).isEqualTo(3)
    assertThat(firstPage.number).isEqualTo(0)

    assertThat(firstPage.content[0].lastName).isEqualTo("Address")
    assertThat(firstPage.content[1].lastName).isEqualTo("Last")
    assertThat(firstPage.content[2].lastName).isEqualTo("Ten")
  }
}
