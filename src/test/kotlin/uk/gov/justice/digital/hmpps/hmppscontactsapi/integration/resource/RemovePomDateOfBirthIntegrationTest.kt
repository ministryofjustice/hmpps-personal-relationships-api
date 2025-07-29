package uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.resource

import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.PostgresIntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.UpdatePomDobResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.ContactRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.ContactInfo
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.PersonReference
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.Source

@Sql("classpath:remove-pom-dob.tests/data-for-remove-pom-dob-test.sql")
@Sql(
  scripts = ["classpath:remove-pom-dob.tests/cleanup-pom-dob-test.sql"],
  executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD,
)
class RemovePomDateOfBirthIntegrationTest : PostgresIntegrationTestBase() {

  /*
   * The test data sets up 4 contacts and 5 relationships.
   * Each of the 4 contacts is in a POM relationship with prisoner A3333AA.
   * Contacts 1 & 2 have dates of birth and are only in official relationships.
   * Contact 3 has a date of birth but is also in a social relationship with prisoner A4444AA.
   * Contact 4 has no date of birth set.
   */

  @Autowired
  private lateinit var contactRepository: ContactRepository

  @Test
  fun `should find POMs with a date of birth and no social relationships to set their DOBs to null`() {
    assertThat(contactRepository.findById(30001).get().dateOfBirth).isNotNull()
    assertThat(contactRepository.findById(30002).get().dateOfBirth).isNotNull()
    assertThat(contactRepository.findById(30003).get().dateOfBirth).isNotNull()
    assertThat(contactRepository.findById(30004).get().dateOfBirth).isNull()

    webTestClient.put()
      .uri("/utility/remove-pom-dob")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus()
      .isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(UpdatePomDobResponse::class.java)
      .returnResult().responseBody!!

    /*
     * Expected results:
     * Contacts 30001 and 30002 will have their DOBs removed
     * Contact 30003 has a social relationship so will be left with its DOB intact.
     * Contact 30004 has no DOB, so will be ignored
     */

    stubEvents.assertHasEvent(
      event = OutboundEvent.CONTACT_UPDATED,
      additionalInfo = ContactInfo(30001, Source.DPS, "SYS", null),
      personReference = PersonReference(dpsContactId = 30001),
    )

    stubEvents.assertHasEvent(
      event = OutboundEvent.CONTACT_UPDATED,
      additionalInfo = ContactInfo(30002, Source.DPS, "SYS", null),
      personReference = PersonReference(dpsContactId = 30002),
    )

    assertThat(contactRepository.findById(30001).get().dateOfBirth).isNull()
    assertThat(contactRepository.findById(30002).get().dateOfBirth).isNull()
    assertThat(contactRepository.findById(30003).get().dateOfBirth).isNotNull()
    assertThat(contactRepository.findById(30004).get().dateOfBirth).isNull()
  }
}
