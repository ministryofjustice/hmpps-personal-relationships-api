package uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.resource.sync

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.groups.Tuple
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.PostgresIntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync.SyncContactReconcile
import uk.gov.justice.digital.hmpps.hmppscontactsapi.util.StubUser
import java.time.LocalDate

@Sql("classpath:reconcile.tests/data-for-reconcile-test.sql")
@Sql(
  scripts = ["classpath:reconcile.tests/cleanup-reconcile-test.sql"],
  executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD,
)
class SyncReconcileIntegrationTest : PostgresIntegrationTestBase() {

  @BeforeEach
  fun resetEvents() {
    stubEvents.reset()
    setCurrentUser(StubUser.SYNC_AND_MIGRATE_USER)
  }

  @Test
  fun `should return unauthorized if no token provided`() {
    webTestClient.get()
      .uri("/sync/contact/1/reconcile")
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `should return forbidden if incorrect role`() {
    setCurrentUser(StubUser.USER_WITH_WRONG_ROLES)
    webTestClient.get()
      .uri("/sync/contact/1/reconcile")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationUsingCurrentUser())
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `reconcile should return the correct details for the contact`() {
    val reconcileResponse = webTestClient.get()
      .uri("/sync/contact/30001/reconcile")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationUsingCurrentUser())
      .exchange()
      .expectStatus()
      .isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(SyncContactReconcile::class.java)
      .returnResult().responseBody!!

    with(reconcileResponse) {
      assertThat(contactId).isEqualTo(30001)
      assertThat(firstName).isEqualTo("John")
      assertThat(lastName).isEqualTo("Ma")
      assertThat(middleNames).isNullOrEmpty()
      assertThat(dateOfBirth).isEqualTo(LocalDate.of(2000, 11, 21))

      assertThat(phones).hasSize(2)
      assertThat(phones).extracting("phoneType", "phoneNumber").containsAll(
        listOf(
          Tuple("MOB", "01111 666666"),
          Tuple("HOME", "01111 777777"),
        ),
      )

      assertThat(emails).hasSize(2)
      assertThat(emails).extracting("emailAddress").contains("mr.last@example.com", "miss.last@example.com")

      assertThat(identities).hasSize(2)
      assertThat(identities).extracting("identityType").contains("DL", "PASS")

      assertThat(employments).hasSize(1)
      assertThat(employments).extracting("organisationId").containsOnly(57L)

      assertThat(restrictions).hasSize(2)
      assertThat(restrictions).extracting("restrictionType").contains("ACC", "BAN")

      assertThat(addresses).hasSize(3)

      assertThat(relationships).hasSize(4)
      assertThat(relationships).extracting("prisonerNumber", "contactType", "relationshipType")
        .containsAll(
          listOf(
            Tuple("A3333AA", "S", "BRO"),
            Tuple("A3333AA", "O", "POL"),
            Tuple("A4444AA", "S", "MOT"),
            Tuple("A4444AA", "S", "SIS"),
          ),
        )

      addresses.map { address ->
        if (address.contactAddressId == 40003L) {
          assertThat(address.addressPhones)
            .extracting("phoneNumber", "extNumber")
            .containsExactly(Tuple("01111 888888", "+0123"))
        }
      }

      relationships.map { rel ->
        when (rel.prisonerContactId) {
          40001L -> {
            assertThat(rel.relationshipRestrictions)
              .extracting("restrictionType", "startDate", "expiryDate")
              .containsExactly(
                Tuple("BAN", LocalDate.of(2025, 1, 1), LocalDate.of(2025, 5, 2)),
              )
          }

          40002L -> {
            assertThat(rel.relationshipRestrictions)
              .extracting("restrictionType", "startDate", "expiryDate")
              .containsExactly(
                Tuple("BAN", LocalDate.of(2025, 1, 2), LocalDate.of(2025, 5, 3)),
              )
          }

          else -> null
        }
      }
    }
  }
}
