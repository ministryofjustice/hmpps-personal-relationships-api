package uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.SecureAPIIntegrationTestBase

class GetContactIdentityIntegrationTest : SecureAPIIntegrationTestBase() {

  override val allowedRoles: Set<String> = setOf("ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__RW", "ROLE_CONTACTS__R")

  override fun baseRequestBuilder(): WebTestClient.RequestHeadersSpec<*> = webTestClient.get()
    .uri("/contact/1/identity/1")

  @ParameterizedTest
  @ValueSource(strings = ["ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__R", "ROLE_CONTACTS__RW"])
  fun `should get identity details where the type is active`(role: String) {
    val identity = testAPIClient.getContactIdentity(1, 1, role)

    with(identity) {
      assertThat(contactIdentityId).isEqualTo(1)
      assertThat(contactId).isEqualTo(1)
      assertThat(identityType).isEqualTo("DL")
      assertThat(identityTypeDescription).isEqualTo("Driving Licence")
      assertThat(identityTypeIsActive).isTrue()
      assertThat(identityValue).isEqualTo("LAST-87736799M")
      assertThat(issuingAuthority).isEqualTo("DVLA")
      assertThat(createdBy).isEqualTo("TIM")
    }
  }

  @Test
  fun `should get identity details where the type is inactive`() {
    val identity = testAPIClient.getContactIdentity(4, 4)

    with(identity) {
      assertThat(contactIdentityId).isEqualTo(4)
      assertThat(contactId).isEqualTo(4)
      assertThat(identityType).isEqualTo("NHS")
      assertThat(identityTypeDescription).isEqualTo("NHS Number")
      assertThat(identityTypeIsActive).isFalse()
      assertThat(identityValue).isEqualTo("NHS999")
      assertThat(issuingAuthority).isEqualTo("National Health Service")
      assertThat(createdBy).isEqualTo("JAMES")
    }
  }
}
