package uk.gov.justice.digital.hmpps.personalrelationships.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.openapitools.jackson.nullable.JsonNullable
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.personalrelationships.integration.SecureAPIIntegrationTestBase
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.CreateContactRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.PatchContactRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.ContactAuditEntry
import uk.gov.justice.digital.hmpps.personalrelationships.util.StubUser

class GetContactHistoryIntegrationTest : SecureAPIIntegrationTestBase() {
  override val allowedRoles: Set<String> = setOf("ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__RW", "ROLE_CONTACTS__R")

  @BeforeEach
  fun setUp() {
    setCurrentUser(StubUser.READ_ONLY_USER)
  }

  override fun baseRequestBuilder() = webTestClient.get()
    .uri("/contact/1/history")
    .accept(MediaType.APPLICATION_JSON)

  @Test
  fun `should return 404 if contact does not exist`() {
    webTestClient.get()
      .uri("/contact/999999/history")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationUsingCurrentUser())
      .exchange()
      .expectStatus().isNotFound
  }

  @Test
  fun `should return initial creation audit for seeded contact`() {
    val history: List<ContactAuditEntry> = testAPIClient.getContactHistory(1)
    assertThat(history).isNotEmpty()
  }

  @Test
  fun `should include modified revision after patching first and last name`() {
    // Create a fresh contact so history is isolated
    val created = doWithTemporaryWritePermission { testAPIClient.createAContact(CreateContactRequest(lastName = "History", firstName = "Original")) }

    // Patch names
    doWithTemporaryWritePermission {
      testAPIClient.patchAContact(
        PatchContactRequest(
          firstName = JsonNullable.of("UpdatedFirst"),
          lastName = JsonNullable.of("UpdatedLast"),
        ),
        "/contact/${created.id}",
      )
    }

    val history: List<ContactAuditEntry> = testAPIClient.getContactHistory(created.id)

    assertThat(history.map(ContactAuditEntry::revisionType)).containsExactly("ADD", "MOD")
    val modEntry = history.last()
    assertThat(modEntry.firstName).isEqualTo("UpdatedFirst")
    assertThat(modEntry.lastName).isEqualTo("UpdatedLast")
    assertThat(modEntry.updatedBy).isEqualTo("Read Write")
    assertThat(modEntry.createdBy).isEqualTo("Read Write")
    assertThat(modEntry.updatedTime).isNotNull()
  }
}
