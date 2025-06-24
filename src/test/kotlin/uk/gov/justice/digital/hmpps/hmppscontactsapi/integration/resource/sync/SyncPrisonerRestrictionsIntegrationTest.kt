package uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.resource.sync

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.hmppscontactsapi.config.User
import uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.PostgresIntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncCreatePrisonerRestrictionRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncUpdatePrisonerRestrictionRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync.SyncPrisonerRestriction
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.PersonReference
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.PrisonerRestrictionInfo
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.Source
import uk.gov.justice.digital.hmpps.hmppscontactsapi.util.StubUser
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.stream.Stream

class SyncPrisonerRestrictionsIntegrationTest : PostgresIntegrationTestBase() {

  @BeforeEach
  fun setUp() {
    setCurrentUser(StubUser.SYNC_AND_MIGRATE_USER)
  }

  @Test
  fun `should create a prisoner restriction`() {
    val prisonerNumber = "A1234BC"
    val now = LocalDateTime.now()
    val createRequest = syncCreatePrisonerRestrictionRequest(prisonerNumber, now)

    val created = webTestClient.post()
      .uri("/sync/prisoner-restriction")
      .headers(setAuthorisationUsingCurrentUser())
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(createRequest)
      .exchange()
      .expectStatus().isCreated
      .expectBody<SyncPrisonerRestriction>()
      .returnResult()
      .responseBody!!

    assertThat(created.prisonerNumber).isEqualTo(prisonerNumber)
    assertThat(created.restrictionType).isEqualTo("CCTV")
    assertThat(created.authorisedUsername).isEqualTo("JSMITH")

    stubEvents.assertHasEvent(
      event = OutboundEvent.PRISONER_RESTRICTIONS_CREATED,
      additionalInfo = PrisonerRestrictionInfo(created.prisonerRestrictionId, Source.NOMIS, created.createdBy, null),
      personReference = PersonReference(nomsNumber = created.prisonerNumber),
    )
  }

  @Test
  fun `should retrieve a prisoner restriction`() {
    val prisonerNumber = "A1234BC"
    val now = LocalDateTime.now()
    val createRequest = syncCreatePrisonerRestrictionRequest(prisonerNumber, now)

    val created = webTestClient.post()
      .uri("/sync/prisoner-restriction")
      .headers(setAuthorisationUsingCurrentUser())
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(createRequest)
      .exchange()
      .expectStatus().isCreated
      .expectBody<SyncPrisonerRestriction>()
      .returnResult()
      .responseBody!!

    stubEvents.reset()
    val retrieved = webTestClient.get()
      .uri("/sync/prisoner-restriction/${created.prisonerRestrictionId}")
      .headers(setAuthorisationUsingCurrentUser())
      .exchange()
      .expectStatus().isOk
      .expectBody<SyncPrisonerRestriction>()
      .returnResult()
      .responseBody!!

    assertThat(retrieved).usingRecursiveComparison().ignoringFields("createdTime").isEqualTo(created)
  }

  @Test
  fun `should update a prisoner restriction`() {
    val prisonerNumber = "A1234BC"
    val now = LocalDateTime.now()
    val createRequest = syncCreatePrisonerRestrictionRequest(prisonerNumber, now)

    val created = webTestClient.post()
      .uri("/sync/prisoner-restriction")
      .headers(setAuthorisationUsingCurrentUser())
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(createRequest)
      .exchange()
      .expectStatus().isCreated
      .expectBody<SyncPrisonerRestriction>()
      .returnResult()
      .responseBody!!

    stubEvents.reset()
    val updateRequest = syncUpdatePrisonerRestrictionRequest(prisonerNumber, now.plusDays(1))

    val updated = webTestClient.put()
      .uri("/sync/prisoner-restriction/${created.prisonerRestrictionId}")
      .headers(setAuthorisationUsingCurrentUser())
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(updateRequest)
      .exchange()
      .expectStatus().isOk
      .expectBody<SyncPrisonerRestriction>()
      .returnResult()
      .responseBody!!

    assertThat(updated.commentText).isEqualTo("Visits allowed after review")
    assertThat(updated.updatedBy).isEqualTo("JDOE_ADM")

    stubEvents.assertHasEvent(
      event = OutboundEvent.PRISONER_RESTRICTIONS_UPDATED,
      additionalInfo = PrisonerRestrictionInfo(created.prisonerRestrictionId, Source.NOMIS, updated.updatedBy!!, null),
      personReference = PersonReference(nomsNumber = created.prisonerNumber),
    )
  }

  @Test
  fun `should delete a prisoner restriction`() {
    val prisonerNumber = "A1234BC"
    val now = LocalDateTime.now()
    val createRequest = syncCreatePrisonerRestrictionRequest(prisonerNumber, now)

    val created = webTestClient.post()
      .uri("/sync/prisoner-restriction")
      .headers(setAuthorisationUsingCurrentUser())
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(createRequest)
      .exchange()
      .expectStatus().isCreated
      .expectBody<SyncPrisonerRestriction>()
      .returnResult()
      .responseBody!!

    webTestClient.delete()
      .uri("/sync/prisoner-restriction/${created.prisonerRestrictionId}")
      .headers(setAuthorisationUsingCurrentUser())
      .exchange()
      .expectStatus().isNoContent

    stubEvents.assertHasEvent(
      event = OutboundEvent.PRISONER_RESTRICTIONS_DELETED,
      additionalInfo = PrisonerRestrictionInfo(created.prisonerRestrictionId, Source.NOMIS, User.SYS_USER.username, null),
      personReference = PersonReference(nomsNumber = created.prisonerNumber),
    )

    webTestClient.get()
      .uri("/sync/prisoner-restriction/${created.prisonerRestrictionId}")
      .headers(setAuthorisationUsingCurrentUser())
      .exchange()
      .expectStatus().isNotFound
  }

  @ParameterizedTest
  @MethodSource("invalidCreateRequestProvider")
  fun `should return 400 for invalid create request`(expectedMessage: String, invalidRequest: Any) {
    webTestClient.post()
      .uri("/sync/prisoner-restriction")
      .headers(setAuthorisationUsingCurrentUser())
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(invalidRequest)
      .exchange()
      .expectStatus().isBadRequest
      .expectBody()
      .jsonPath("$.userMessage").value<String> {
        assertThat(it).contains(expectedMessage)
      }
  }

  @ParameterizedTest
  @MethodSource("invalidUpdateRequestProvider")
  fun `should return 400 for invalid update request`(expectedMessage: String, invalidRequest: Any) {
    webTestClient.put()
      .uri("/sync/prisoner-restriction/11223344")
      .headers(setAuthorisationUsingCurrentUser())
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(invalidRequest)
      .exchange()
      .expectStatus().isBadRequest
      .expectBody()
      .jsonPath("$.userMessage").value<String> {
        assertThat(it).contains(expectedMessage)
      }
  }

  companion object {
    @JvmStatic
    fun invalidCreateRequestProvider(): Stream<Arguments> = Stream.of(
      Arguments.of(
        "Validation failure(s): prisonerNumber must not be blank",
        syncCreatePrisonerRestrictionRequest().copy(prisonerNumber = ""),
      ),
      Arguments.of(
        "restrictionType must be less than or equal to 12 characters",
        syncCreatePrisonerRestrictionRequest().copy(restrictionType = "TOO_LONG_RESTRICTION_TYPE"),
      ),
      Arguments.of(
        "Validation failure(s): createdBy must not be blank",
        syncCreatePrisonerRestrictionRequest().copy(createdBy = ""),
      ),
      Arguments.of(
        "Validation failure(s): commentText size must be between 0 and 240",
        syncCreatePrisonerRestrictionRequest().copy(commentText = "a".repeat(241)),
      ),
      Arguments.of(
        "Validation failure: createdBy must not be null",
        """
        {
          "prisonerNumber": "A1234BC",
          "restrictionType": "CCTV",
          "effectiveDate": "2024-06-11",
          "expiryDate": "2024-12-31",
          "commentText": "No visits allowed",
          "authorisedUsername": 654321,
          "currentTerm": true,
          "createdTime": "2024-06-11T10:00:00",
          "updatedBy": null,
          "updatedTime": null
        }
        """.trimIndent(),
      ),
      Arguments.of(

        "Validation failure: authorisedUsername must not be null",
        """
        {
          "prisonerNumber": "A1234BC",
          "restrictionType": "CCTV",
          "effectiveDate": "2024-06-11",
          "expiryDate": "2024-12-31",
          "commentText": "No visits allowed",
          "createdTime": "2024-06-11T10:00:00",
          "currentTerm": true,
          "updatedBy": null,
          "updatedTime": null
        }
        """.trimIndent(),
      ),
    )

    @JvmStatic
    fun invalidUpdateRequestProvider(): Stream<Arguments> = Stream.of(
      Arguments.of(
        "Validation failure(s): prisonerNumber must not be blank",
        syncUpdatePrisonerRestrictionRequest().copy(prisonerNumber = ""),
      ),
      Arguments.of(
        "restrictionType must be less than or equal to 12 characters",
        syncUpdatePrisonerRestrictionRequest().copy(restrictionType = "TOO_LONG_RESTRICTION_TYPE"),
      ),
      Arguments.of(
        "Validation failure(s): commentText size must be between 0 and 240",
        syncUpdatePrisonerRestrictionRequest().copy(commentText = "a".repeat(241)),
      ),
      Arguments.of(

        "Validation failure: authorisedUsername must not be null",
        """
        {
          "prisonerNumber": "A1234BC",
          "restrictionType": "CCTV",
          "effectiveDate": "2024-06-11",
          "expiryDate": "2024-12-31",
          "commentText": "No visits allowed",
          "currentTerm": true,
          "updatedBy": null,
          "updatedTime": null
        }
        """.trimIndent(),
      ),
      Arguments.of(
        "Validation failure(s): updatedBy must not be blank",
        """
        {
          "prisonerNumber": "A1234BC",
          "restrictionType": "CCTV",
          "effectiveDate": "2024-06-11",
          "expiryDate": "2024-12-31",
          "commentText": "No visits allowed",
          "authorisedUsername": 654321,
          "currentTerm": true,
          "updatedBy": null,
          "updatedTime": null
        }
        """.trimIndent(),
      ),
      Arguments.of(

        "Validation failure: authorisedUsername must not be null",
        """
        {
          "prisonerNumber": "A1234BC",
          "restrictionType": "CCTV",
          "effectiveDate": "2024-06-11",
          "expiryDate": "2024-12-31",
          "commentText": "No visits allowed",
          "currentTerm": true,
          "updatedBy": null,
          "updatedTime": null
        }
        """.trimIndent(),
      ),
    )

    private fun syncCreatePrisonerRestrictionRequest(
      prisonerNumber: String = "A1234BC",
      now: LocalDateTime = LocalDateTime.now(),
    ) = SyncCreatePrisonerRestrictionRequest(
      prisonerNumber = prisonerNumber,
      restrictionType = "CCTV",
      effectiveDate = LocalDate.of(2024, 6, 11),
      expiryDate = LocalDate.of(2024, 12, 31),
      commentText = "No visits allowed",
      authorisedUsername = "JSMITH",
      currentTerm = true,
      createdBy = "JSMITH_ADM",
      createdTime = now,
    )

    private fun syncUpdatePrisonerRestrictionRequest(
      prisonerNumber: String = "A1234BC",
      now: LocalDateTime = LocalDateTime.now(),
    ) = SyncUpdatePrisonerRestrictionRequest(
      prisonerNumber = prisonerNumber,
      restrictionType = "CCTV",
      effectiveDate = LocalDate.of(2024, 6, 11),
      expiryDate = LocalDate.of(2024, 12, 31),
      commentText = "Visits allowed after review",
      authorisedUsername = "JSMITH",
      currentTerm = true,
      updatedBy = "JDOE_ADM",
      updatedTime = now,
    )
  }
}
