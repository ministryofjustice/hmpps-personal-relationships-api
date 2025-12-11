package uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.SecureAPIIntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.CreateContactRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.util.StubUser

class SearchContactsByIdIntegrationTest : SecureAPIIntegrationTestBase() {
  private var contactId = 0L

  @BeforeEach
  fun setUp() {
    setCurrentUser(StubUser.CREATING_USER)
    contactId = testAPIClient.createAContact(
      CreateContactRequest(
        lastName = "address",
        firstName = "has",
      ),
    ).id
  }

  override val allowedRoles: Set<String> = setOf("ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__RW", "ROLE_CONTACTS__R")

  override fun baseRequestBuilder(): WebTestClient.RequestHeadersSpec<*> = webTestClient.get()
    .uri("/contact/search/partial-contact-id?contactId=$contactId")
    .accept(MediaType.APPLICATION_JSON)

  @Test
  fun `should find contact by partial id`() {
    val partialContactId = contactId.toString().take(2)
    val response = testAPIClient.getSearchContactsById(partialContactId)
    assertThat(response?.content).isNotEmpty
    assertThat(response?.content?.any { it.id.toString().contains(partialContactId) }).isTrue()
  }
}
