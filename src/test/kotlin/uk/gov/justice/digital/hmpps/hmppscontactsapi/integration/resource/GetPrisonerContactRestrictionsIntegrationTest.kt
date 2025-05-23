package uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppscontactsapi.client.manage.users.UserDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.SecureAPIIntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.ContactRelationship
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.CreateContactRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.util.StubUser
import java.time.LocalDate
import java.time.LocalDateTime

class GetPrisonerContactRestrictionsIntegrationTest : SecureAPIIntegrationTestBase() {

  @BeforeEach
  fun setUp() {
    setCurrentUser(StubUser.READ_ONLY_USER)
  }

  override val allowedRoles: Set<String> = setOf("ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__RW", "ROLE_CONTACTS__R")

  override fun baseRequestBuilder(): WebTestClient.RequestHeadersSpec<*> = webTestClient.get()
    .uri("/prisoner-contact/1/restriction")

  @Test
  fun `should return not found if the prisoner contact relationship is not found`() {
    webTestClient.get()
      .uri("/prisoner-contact/-1/restriction")
      .headers(setAuthorisationUsingCurrentUser())
      .exchange()
      .expectStatus()
      .isNotFound
  }

  @ParameterizedTest
  @ValueSource(strings = ["ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__R", "ROLE_CONTACTS__RW"])
  fun `should return all relationship and global restrictions for a contact`(role: String) {
    setCurrentUser(StubUser.READ_ONLY_USER.copy(roles = listOf(role)))
    stubGetUserByUsername(UserDetails("officer", "The Officer"))
    stubGetUserByUsername(UserDetails("editor", "The Editor"))
    stubGetUserByUsername(UserDetails("JBAKER_GEN", "James Test"))

    val restrictions = testAPIClient.getPrisonerContactRestrictions(10)

    assertThat(restrictions.contactGlobalRestrictions).hasSize(2)
    with(restrictions.contactGlobalRestrictions[0]) {
      assertThat(contactRestrictionId).isNotNull()
      assertThat(contactId).isEqualTo(3)
      assertThat(restrictionType).isEqualTo("CCTV")
      assertThat(restrictionTypeDescription).isEqualTo("CCTV")
      assertThat(startDate).isEqualTo(LocalDate.of(2000, 11, 21))
      assertThat(expiryDate).isEqualTo(LocalDate.of(2001, 11, 21))
      assertThat(comments).isEqualTo("N/A")
      assertThat(enteredByUsername).isEqualTo("JBAKER_GEN")
      assertThat(enteredByDisplayName).isEqualTo("James Test")
    }
    with(restrictions.contactGlobalRestrictions[1]) {
      assertThat(contactRestrictionId).isNotNull()
      assertThat(contactId).isEqualTo(3)
      assertThat(restrictionType).isEqualTo("BAN")
      assertThat(restrictionTypeDescription).isEqualTo("Banned")
      assertThat(startDate).isNull()
      assertThat(expiryDate).isNull()
      assertThat(comments).isNull()
      assertThat(enteredByUsername).isEqualTo("FOO_USER")
      assertThat(enteredByDisplayName).isEqualTo("FOO_USER")
    }

    assertThat(restrictions.prisonerContactRestrictions).hasSize(2)
    with(restrictions.prisonerContactRestrictions[0]) {
      assertThat(prisonerContactRestrictionId).isNotNull()
      assertThat(contactId).isEqualTo(3L)
      assertThat(prisonerNumber).isEqualTo("G4793VF")
      assertThat(restrictionType).isEqualTo("PREINF")
      assertThat(restrictionTypeDescription).isEqualTo("Previous info")
      assertThat(startDate).isEqualTo(LocalDate.of(2024, 1, 1))
      assertThat(expiryDate).isEqualTo(LocalDate.of(2024, 12, 31))
      assertThat(comments).isEqualTo("Restriction due to ongoing investigation")
      assertThat(enteredByUsername).isEqualTo("editor")
      assertThat(enteredByDisplayName).isEqualTo("The Editor")
      assertThat(createdBy).isEqualTo("admin")
      assertThat(createdTime).isEqualTo(LocalDateTime.of(2024, 10, 1, 12, 0, 0))
      assertThat(updatedBy).isEqualTo("editor")
      assertThat(updatedTime).isEqualTo(LocalDateTime.of(2024, 10, 2, 15, 30, 0))
    }
    with(restrictions.prisonerContactRestrictions[1]) {
      assertThat(prisonerContactRestrictionId).isNotNull()
      assertThat(contactId).isEqualTo(3L)
      assertThat(prisonerNumber).isEqualTo("G4793VF")
      assertThat(restrictionType).isEqualTo("BAN")
      assertThat(restrictionTypeDescription).isEqualTo("Banned")
      assertThat(startDate).isNull()
      assertThat(expiryDate).isNull()
      assertThat(comments).isNull()
      assertThat(enteredByUsername).isEqualTo("officer")
      assertThat(enteredByDisplayName).isEqualTo("The Officer")
      assertThat(createdBy).isEqualTo("officer")
      assertThat(createdTime).isEqualTo(LocalDateTime.of(2022, 8, 14, 11, 0, 0))
      assertThat(updatedBy).isNull()
      assertThat(updatedTime).isNull()
    }
  }

  @Test
  fun `should return empty list if no restrictions for a contact`() {
    val prisonerNumber = "G4793VF"
    stubPrisonSearchWithResponse(prisonerNumber)
    val created = doWithTemporaryWritePermission {
      testAPIClient.createAContactWithARelationship(
        CreateContactRequest(
          lastName = "Last",
          firstName = "First",
          relationship = ContactRelationship(
            prisonerNumber = prisonerNumber,
            relationshipTypeCode = "S",
            relationshipToPrisonerCode = "FRI",
            isNextOfKin = false,
            isEmergencyContact = false,
            isApprovedVisitor = false,
            comments = null,
          ),
        ),
      )
    }
    val restrictions = testAPIClient.getPrisonerContactRestrictions(
      created.createdRelationship!!.prisonerContactId,

    )
    assertThat(restrictions.prisonerContactRestrictions).isEmpty()
    assertThat(restrictions.contactGlobalRestrictions).isEmpty()
  }
}
