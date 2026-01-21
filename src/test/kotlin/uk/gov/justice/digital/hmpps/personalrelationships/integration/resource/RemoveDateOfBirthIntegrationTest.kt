package uk.gov.justice.digital.hmpps.personalrelationships.integration.resource

import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.personalrelationships.integration.PostgresIntegrationTestBase
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.UpdateInternalOfficialDobResponse
import uk.gov.justice.digital.hmpps.personalrelationships.repository.ContactRepository
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.ContactInfo
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.PersonReference
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.Source

@Sql("classpath:remove-dob.tests/data-for-remove-internal-official-dob-test.sql")
@Sql(
  scripts = ["classpath:remove-dob.tests/cleanup-internal-official-dob-test.sql"],
  executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD,
)
class RemoveDateOfBirthIntegrationTest : PostgresIntegrationTestBase() {

  /*
   * The test data sets up 6 contacts, each of which is in an internal official relationship with prisoner A3333AA.
   *
   * Contact 1 has a date of birth and only in a POM relationship with A3333AA - will update
   * Contact 2 has a data of birth and only in a COM relationship with A3333AA - will update
   * Contact 3 has a date of birth and only in a PPA relationships with A3333AA - will update
   * Contact 4 has a date of birth, and internal relationships PROB for A3333AA, RO for A4444AA - will update
   * Contact 5 has a date of birth and is an RO for A3333AA, but has a social relationship FRI with A4444AA - no update
   * Contact 6 has no date of birth set but is in a CA relationship with A3333AA - no update
   */

  @Autowired
  private lateinit var contactRepository: ContactRepository

  @Test
  fun `should find internal official contacts with a date of birth and other relationships to set their DOBs to null`() {
    assertThat(contactRepository.findById(30001).get().dateOfBirth).isNotNull()
    assertThat(contactRepository.findById(30002).get().dateOfBirth).isNotNull()
    assertThat(contactRepository.findById(30003).get().dateOfBirth).isNotNull()
    assertThat(contactRepository.findById(30004).get().dateOfBirth).isNotNull()
    assertThat(contactRepository.findById(30005).get().dateOfBirth).isNotNull()
    assertThat(contactRepository.findById(30006).get().dateOfBirth).isNull()

    webTestClient.put()
      .uri("/utility/remove-internal-official-dob")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus()
      .isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(UpdateInternalOfficialDobResponse::class.java)
      .returnResult().responseBody!!

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

    stubEvents.assertHasEvent(
      event = OutboundEvent.CONTACT_UPDATED,
      additionalInfo = ContactInfo(30003, Source.DPS, "SYS", null),
      personReference = PersonReference(dpsContactId = 30003),
    )

    stubEvents.assertHasEvent(
      event = OutboundEvent.CONTACT_UPDATED,
      additionalInfo = ContactInfo(30004, Source.DPS, "SYS", null),
      personReference = PersonReference(dpsContactId = 30004),
    )

    assertThat(contactRepository.findById(30001).get().dateOfBirth).isNull()
    assertThat(contactRepository.findById(30002).get().dateOfBirth).isNull()
    assertThat(contactRepository.findById(30003).get().dateOfBirth).isNull()
    assertThat(contactRepository.findById(30004).get().dateOfBirth).isNull()
    assertThat(contactRepository.findById(30005).get().dateOfBirth).isNotNull()
    assertThat(contactRepository.findById(30006).get().dateOfBirth).isNull()
  }
}
