package uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.SecureAPIIntegrationTestBase

class GetContactPhoneIntegrationTest : SecureAPIIntegrationTestBase() {

  override val allowedRoles: Set<String> = setOf("ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__RW", "ROLE_CONTACTS__R")

  override fun baseRequestBuilder(): WebTestClient.RequestHeadersSpec<*> = webTestClient.get()
    .uri("/contact/1/phone/1")
    .accept(MediaType.APPLICATION_JSON)

  @ParameterizedTest
  @ValueSource(strings = ["ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__R", "ROLE_CONTACTS__RW"])
  fun `should get phone details`(role: String) {
    val phone = testAPIClient.getContactPhone(1, 2, role)

    with(phone) {
      assertThat(contactPhoneId).isEqualTo(2)
      assertThat(contactId).isEqualTo(1)
      assertThat(phoneType).isEqualTo("HOME")
      assertThat(phoneTypeDescription).isEqualTo("Home")
      assertThat(phoneNumber).isEqualTo("01111 777777")
      assertThat(extNumber).isEqualTo("+0123")
      assertThat(createdBy).isEqualTo("JAMES")
    }
  }
}
