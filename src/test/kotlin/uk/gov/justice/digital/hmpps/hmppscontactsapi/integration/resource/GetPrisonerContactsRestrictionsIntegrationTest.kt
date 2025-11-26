package uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppscontactsapi.client.manage.users.UserDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.SecureAPIIntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.PrisonerContactIdsRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.util.StubUser
import java.time.LocalDate

class GetPrisonerContactsRestrictionsIntegrationTest : SecureAPIIntegrationTestBase() {

  @BeforeEach
  fun setUp() {
    setCurrentUser(StubUser.READ_ONLY_USER)
  }

  override val allowedRoles: Set<String> = setOf("ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__RW", "ROLE_CONTACTS__R")

  override fun baseRequestBuilder(): WebTestClient.RequestHeadersSpec<*> = webTestClient.post()
    .uri("/prisoner-contact/restrictions")
    .bodyValue(PrisonerContactIdsRequest(emptyList()))

  @Test
  fun `should return no contacts if the prisoner contact ids are not found`() {
    val response = testAPIClient.postPrisonerContactsRestrictions(-1)

    assertThat(response.prisonerContactRestrictions).isEmpty()
  }

  @ParameterizedTest
  @ValueSource(strings = ["ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__R", "ROLE_CONTACTS__RW"])
  fun `should return all relationship and global restrictions for a contact`(role: String) {
    setCurrentUser(StubUser.READ_ONLY_USER.copy(roles = listOf(role)))
    stubGetUserByUsername(UserDetails("officer", "The Officer"))
    stubGetUserByUsername(UserDetails("editor", "The Editor"))
    stubGetUserByUsername(UserDetails("JBAKER_GEN", "James Test"))

    val response = testAPIClient.postPrisonerContactsRestrictions(10)

    val prisonerRestrictions = response.prisonerContactRestrictions.single()

    assertThat(prisonerRestrictions.prisonerContactRestrictions).hasSize(2)

    with(prisonerRestrictions.prisonerContactRestrictions.first()) {
      assertThat(prisonerContactRestrictionId).isNotNull()
      assertThat(contactId).isEqualTo(3L)
      assertThat(prisonerNumber).isEqualTo("G4793VF")
      assertThat(restrictionType).isEqualTo("PREINF")
      assertThat(restrictionTypeDescription).isEqualTo("Previous info")
      assertThat(startDate).isEqualTo(LocalDate.of(2024, 1, 1))
      assertThat(expiryDate).isEqualTo(LocalDate.of(2024, 12, 31))
      assertThat(comments).isEqualTo("Restriction due to ongoing investigation")
      assertThat(enteredByDisplayName).isEqualTo("The Editor")
    }
    with(prisonerRestrictions.prisonerContactRestrictions.last()) {
      assertThat(prisonerContactRestrictionId).isNotNull()
      assertThat(contactId).isEqualTo(3L)
      assertThat(prisonerNumber).isEqualTo("G4793VF")
      assertThat(restrictionType).isEqualTo("BAN")
      assertThat(restrictionTypeDescription).isEqualTo("Banned")
      assertThat(startDate).isNull()
      assertThat(expiryDate).isNull()
      assertThat(comments).isNull()
      assertThat(enteredByDisplayName).isEqualTo("The Officer")
    }

    assertThat(prisonerRestrictions.globalContactRestrictions).hasSize(2)

    with(prisonerRestrictions.globalContactRestrictions.first()) {
      assertThat(contactRestrictionId).isNotNull()
      assertThat(contactId).isEqualTo(3)
      assertThat(restrictionType).isEqualTo("CCTV")
      assertThat(restrictionTypeDescription).isEqualTo("CCTV")
      assertThat(startDate).isEqualTo(LocalDate.of(2000, 11, 21))
      assertThat(expiryDate).isEqualTo(LocalDate.of(2001, 11, 21))
      assertThat(comments).isEqualTo("N/A")
      assertThat(enteredByDisplayName).isEqualTo("James Test")
    }
    with(prisonerRestrictions.globalContactRestrictions.last()) {
      assertThat(contactRestrictionId).isNotNull()
      assertThat(contactId).isEqualTo(3)
      assertThat(restrictionType).isEqualTo("BAN")
      assertThat(restrictionTypeDescription).isEqualTo("Banned")
      assertThat(startDate).isNull()
      assertThat(expiryDate).isNull()
      assertThat(comments).isNull()
      assertThat(enteredByDisplayName).isEqualTo("FOO_USER")
    }
  }
}
