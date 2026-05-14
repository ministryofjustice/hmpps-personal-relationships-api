package uk.gov.justice.digital.hmpps.personalrelationships.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.personalrelationships.client.manage.users.UserDetails
import uk.gov.justice.digital.hmpps.personalrelationships.integration.SecureAPIIntegrationTestBase
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.ContactIdsRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.CreateContactRequest
import uk.gov.justice.digital.hmpps.personalrelationships.util.StubUser
import java.time.LocalDate

class GetContactsGlobalRestrictionsIntegrationTest : SecureAPIIntegrationTestBase() {

  @BeforeEach
  fun setUp() {
    setCurrentUser(StubUser.READ_ONLY_USER)
  }

  override val allowedRoles: Set<String> = setOf("ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__RW", "ROLE_CONTACTS__R")

  override fun baseRequestBuilder(): WebTestClient.RequestHeadersSpec<*> = webTestClient.post()
    .uri("/contacts/restrictions")
    .bodyValue(ContactIdsRequest(listOf(1L)))

  @ParameterizedTest
  @ValueSource(strings = ["ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__R", "ROLE_CONTACTS__RW"])
  fun `should return all global restrictions for contacts`(role: String) {
    stubGetUserByUsername(UserDetails("JBAKER_GEN", "James Baker"))
    setCurrentUser(StubUser.READ_ONLY_USER.copy(roles = listOf(role)))

    val response = testAPIClient.getBulkContactGlobalRestrictions(listOf(3L))

    assertThat(response.contactRestrictions).hasSize(1)

    with(response.contactRestrictions[0]) {
      assertThat(contactId).isEqualTo(3L)
      assertThat(globalContactRestrictions).hasSize(2)

      with(globalContactRestrictions[0]) {
        assertThat(contactRestrictionId).isNotNull()
        assertThat(contactId).isEqualTo(3L)
        assertThat(restrictionType).isEqualTo("CCTV")
        assertThat(restrictionTypeDescription).isEqualTo("CCTV")
        assertThat(startDate).isEqualTo(LocalDate.of(2000, 11, 21))
        assertThat(expiryDate).isEqualTo(LocalDate.of(2001, 11, 21))
        assertThat(comments).isEqualTo("N/A")
        assertThat(enteredByUsername).isEqualTo("JBAKER_GEN")
        assertThat(enteredByDisplayName).isEqualTo("James Baker")
      }

      with(globalContactRestrictions[1]) {
        assertThat(contactRestrictionId).isNotNull()
        assertThat(contactId).isEqualTo(3L)
        assertThat(restrictionType).isEqualTo("BAN")
        assertThat(restrictionTypeDescription).isEqualTo("Banned")
        assertThat(startDate).isNull()
        assertThat(expiryDate).isNull()
        assertThat(comments).isNull()
        assertThat(enteredByUsername).isEqualTo("FOO_USER")
        assertThat(enteredByDisplayName).isEqualTo("FOO_USER")
      }
    }
  }

  @Test
  fun `should return empty restrictions list if contact has no restrictions`() {
    val createdContact = doWithTemporaryWritePermission {
      testAPIClient.createAContact(CreateContactRequest(lastName = "Last", firstName = "First"))
    }

    val response = testAPIClient.getBulkContactGlobalRestrictions(listOf(createdContact.id))

    assertThat(response.contactRestrictions).hasSize(1)

    with(response.contactRestrictions[0]) {
      assertThat(contactId).isEqualTo(createdContact.id)
      assertThat(globalContactRestrictions).isEmpty()
    }
  }

  @Test
  fun `should only return contacts where matches are found`() {
    stubGetUserByUsername(UserDetails("JBAKER_GEN", "James Baker"))

    val response = testAPIClient.getBulkContactGlobalRestrictions(listOf(3L, -1L))

    assertThat(response.contactRestrictions).hasSize(1)

    with(response.contactRestrictions[0]) {
      assertThat(contactId).isEqualTo(3L)
      assertThat(globalContactRestrictions).hasSize(2)
    }
  }

  @Test
  fun `should return empty list if no contacts are found`() {
    val response = testAPIClient.getBulkContactGlobalRestrictions(listOf(-1L, -2L))

    assertThat(response.contactRestrictions).isEmpty()
  }
}
