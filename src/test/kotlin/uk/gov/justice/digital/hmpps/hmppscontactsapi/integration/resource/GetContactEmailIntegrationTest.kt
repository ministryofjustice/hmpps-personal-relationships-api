package uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.SecureAPIIntegrationTestBase

class GetContactEmailIntegrationTest : SecureAPIIntegrationTestBase() {

  override val allowedRoles: Set<String> = setOf("ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__RW", "ROLE_CONTACTS__R")

  override fun baseRequestBuilder(): WebTestClient.RequestHeadersSpec<*> = webTestClient.get()
    .uri("/contact/1/email/1")
    .accept(MediaType.APPLICATION_JSON)

  @ParameterizedTest
  @ValueSource(strings = ["ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__R", "ROLE_CONTACTS__RW"])
  fun `should get email details`(role: String) {
    val email = testAPIClient.getContactEmail(1, 1, role)

    with(email) {
      assertThat(contactEmailId).isEqualTo(1)
      assertThat(contactId).isEqualTo(1)
      assertThat(emailAddress).isEqualTo("mr.last@example.com")
      assertThat(createdBy).isEqualTo("TIM")
    }
  }
}
