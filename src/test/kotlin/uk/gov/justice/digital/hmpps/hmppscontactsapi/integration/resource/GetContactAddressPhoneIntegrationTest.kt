package uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.SecureAPIIntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.CreateContactRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.address.CreateContactAddressRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.phone.CreateContactAddressPhoneRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.ContactAddressPhoneDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.util.StubUser
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.time.LocalDateTime

class GetContactAddressPhoneIntegrationTest : SecureAPIIntegrationTestBase() {
  private var savedContactId = 0L
  private var savedAddressId = 0L
  private var savedAddressPhoneId = 0L

  override val allowedRoles: Set<String> = setOf("ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__RW", "ROLE_CONTACTS__R")

  @BeforeEach
  fun initialiseData() {
    doWithTemporaryWritePermission {
      savedContactId = testAPIClient.createAContact(
        CreateContactRequest(
          lastName = "address-phone",
          firstName = "has",
        ),

      ).id

      savedAddressId = testAPIClient.createAContactAddress(
        savedContactId,
        CreateContactAddressRequest(
          addressType = "HOME",
          primaryAddress = true,
          property = "27",
          street = "Hello Road",
          countryCode = "ENG",
        ),

      ).contactAddressId

      savedAddressPhoneId = testAPIClient.createAContactAddressPhone(
        savedContactId,
        savedAddressId,
        CreateContactAddressPhoneRequest(
          contactAddressId = savedAddressId,
          phoneType = "HOME",
          phoneNumber = "123456",
          extNumber = "2",
        ),

      ).contactAddressPhoneId
    }
    setCurrentUser(StubUser.READ_ONLY_USER)
  }

  override fun baseRequestBuilder(): WebTestClient.RequestHeadersSpec<*> = webTestClient.get()
    .uri("/contact/$savedContactId/address/$savedAddressId/phone/$savedAddressPhoneId")
    .accept(MediaType.APPLICATION_JSON)

  @Test
  fun `should return not found if wrong ID provided`() {
    val errors = webTestClient.get()
      .uri("/contact/$savedContactId/address/$savedAddressId/phone/-100")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationUsingCurrentUser())
      .exchange()
      .expectStatus()
      .isNotFound
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody!!

    assertThat(errors.userMessage).isEqualTo("Entity not found : Contact address phone (-100) not found")
  }

  @ParameterizedTest
  @ValueSource(strings = ["ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__R", "ROLE_CONTACTS__RW"])
  fun `should return the details of the address-specific phone number`(role: String) {
    setCurrentUser(StubUser.CREATING_USER.copy(roles = listOf(role)))
    val result = webTestClient.get()
      .uri("/contact/$savedContactId/address/$savedAddressId/phone/$savedAddressPhoneId")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationUsingCurrentUser())
      .exchange()
      .expectStatus()
      .isOk()
      .expectBody(ContactAddressPhoneDetails::class.java)
      .returnResult().responseBody!!

    with(result) {
      assertThat(contactAddressPhoneId).isEqualTo(savedAddressPhoneId)
      assertThat(contactAddressId).isEqualTo(savedAddressId)
      assertThat(contactId).isEqualTo(savedContactId)
      assertThat(phoneNumber).isEqualTo("123456")
      assertThat(extNumber).isEqualTo("2")
      assertThat(createdBy).isEqualTo("read_write_user")
      assertThat(createdTime).isBefore(LocalDateTime.now())
    }
  }
}
