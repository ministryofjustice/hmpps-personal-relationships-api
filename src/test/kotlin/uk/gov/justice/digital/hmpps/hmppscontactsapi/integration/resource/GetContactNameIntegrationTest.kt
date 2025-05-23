package uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.SecureAPIIntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.CreateContactRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.util.StubUser

class GetContactNameIntegrationTest : SecureAPIIntegrationTestBase() {

  @BeforeEach
  fun setUp() {
    setCurrentUser(StubUser.READ_ONLY_USER)
  }

  override val allowedRoles: Set<String> = setOf("ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__RW", "ROLE_CONTACTS__R")

  override fun baseRequestBuilder(): WebTestClient.RequestHeadersSpec<*> = webTestClient.get()
    .uri("/contact/1/name")
    .accept(MediaType.APPLICATION_JSON)

  @Test
  fun `should return 404 if the contact doesn't exist`() {
    webTestClient.get()
      .uri("/contact/123456")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationUsingCurrentUser())
      .exchange()
      .expectStatus()
      .isNotFound
  }

  @Test
  fun `should get the contact name with all fields if they have a title`() {
    val contact = doWithTemporaryWritePermission {
      testAPIClient.createAContact(
        CreateContactRequest(
          titleCode = "MR",
          lastName = "Last",
          firstName = "First",
          middleNames = "Middle Names",
        ),
      )
    }

    val name = testAPIClient.getContactName(contact.id)

    with(name) {
      assertThat(titleCode).isEqualTo("MR")
      assertThat(titleDescription).isEqualTo("Mr")
      assertThat(lastName).isEqualTo("Last")
      assertThat(firstName).isEqualTo("First")
      assertThat(middleNames).isEqualTo("Middle Names")
    }
  }

  @Test
  fun `should get the contact name with only optional fields`() {
    val contact = doWithTemporaryWritePermission {
      testAPIClient.createAContact(
        CreateContactRequest(
          titleCode = null,
          lastName = "Last",
          firstName = "First",
          middleNames = null,
        ),
      )
    }

    val name = testAPIClient.getContactName(contact.id)

    with(name) {
      assertThat(titleCode).isNull()
      assertThat(titleDescription).isNull()
      assertThat(lastName).isEqualTo("Last")
      assertThat(firstName).isEqualTo("First")
      assertThat(middleNames).isNull()
    }
  }
}
