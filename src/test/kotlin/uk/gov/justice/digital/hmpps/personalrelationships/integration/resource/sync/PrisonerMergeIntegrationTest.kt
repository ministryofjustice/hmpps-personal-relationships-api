package uk.gov.justice.digital.hmpps.personalrelationships.integration.resource.sync

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.personalrelationships.integration.PostgresIntegrationTestBase
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.migrate.DomesticStatusDetailsRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.migrate.MigratePrisonerDomesticStatusRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.migrate.MigratePrisonerNumberOfChildrenRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.migrate.NumberOfChildrenDetailsRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.sync.SyncPrisonerDomesticStatusResponse
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.sync.SyncPrisonerNumberOfChildrenResponse
import uk.gov.justice.digital.hmpps.personalrelationships.repository.PrisonerDomesticStatusRepository
import uk.gov.justice.digital.hmpps.personalrelationships.repository.PrisonerNumberOfChildrenRepository
import uk.gov.justice.digital.hmpps.personalrelationships.repository.PrisonerRestrictionsRepository
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.PersonReference
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.PrisonerDomesticStatus
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.PrisonerNumberOfChildren
import uk.gov.justice.digital.hmpps.personalrelationships.service.events.Source
import uk.gov.justice.digital.hmpps.personalrelationships.util.StubUser
import java.time.LocalDateTime

class PrisonerMergeIntegrationTest : PostgresIntegrationTestBase() {
  companion object {
    private const val KEEP_PRISONER = "A1234AA"
    private const val REMOVE_PRISONER = "B1234BB"
    private const val MERGE_URI = "/merge/keep/$KEEP_PRISONER/remove/$REMOVE_PRISONER"
  }

  @Autowired
  private lateinit var numberOfChildrenRepository: PrisonerNumberOfChildrenRepository

  @Autowired
  private lateinit var domesticStatusRepository: PrisonerDomesticStatusRepository

  @Autowired
  private lateinit var prisonerRestrictionRepository: PrisonerRestrictionsRepository

  @BeforeEach
  fun setUp() {
    stubEvents.reset()
    numberOfChildrenRepository.deleteAll()
    domesticStatusRepository.deleteAll()
    prisonerRestrictionRepository.deleteAll()
    setCurrentUser(StubUser.SYNC_AND_MIGRATE_USER)
  }

  @Test
  fun `should return unauthorized when no token provided for merge`() {
    webTestClient.put()
      .uri(MERGE_URI)
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `should return forbidden when user lacks authorised role for merge`() {
    setCurrentUser(StubUser.USER_WITH_NO_ROLES)
    webTestClient.put()
      .uri(MERGE_URI)
      .headers(setAuthorisationUsingCurrentUser())
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun `should not merge when remove prisoner has no records`() {
    val expectedNumberOfChildren = "4"
    createNumberOfChildrenRecords(numberOfChildrenRequest(KEEP_PRISONER, expectedNumberOfChildren, listOf("3")))
    val expectedDomesticStatus = "M"
    createDomesticStatusRecords(domesticStatusRequest(KEEP_PRISONER, expectedDomesticStatus, listOf("D")))

    performMerge()

    assertPrisonerMerge(expectedNumberOfChildren, expectedDomesticStatus)
  }

  @Test
  fun `should not merge when keep prisoner has no records`() {
    val expectedNumberOfChildren = "2"
    val expectedDomesticStatus = "S"
    createNumberOfChildrenRecords(numberOfChildrenRequest(REMOVE_PRISONER, expectedNumberOfChildren, listOf("1")))
    createDomesticStatusRecords(domesticStatusRequest(REMOVE_PRISONER, expectedDomesticStatus, listOf("C")))

    performMerge()

    assertDomesticStatusPresent(REMOVE_PRISONER, expectedDomesticStatus)
    assertNumberOfChildrenPresent(REMOVE_PRISONER, expectedNumberOfChildren)
    assertDomesticStatusNotPresent(KEEP_PRISONER)
    assertNumberOfChildrenNotPresent(KEEP_PRISONER)
  }

  @Test
  fun `should merge when removing prisoner's active record is newer than retaining prisoner's active record`() {
    val latestCreatedDate = LocalDateTime.now()
    val olderCreatedDate = latestCreatedDate.minusDays(1)
    createDomesticStatusRecords(
      MigratePrisonerDomesticStatusRequest(
        prisonerNumber = KEEP_PRISONER,
        current = DomesticStatusDetailsRequest(
          domesticStatusCode = "D",
          createdBy = "Admin",
          createdTime = olderCreatedDate,
        ),
      ),
    )

    val keepingDomesticStatus = "P"
    createDomesticStatusRecords(
      MigratePrisonerDomesticStatusRequest(
        prisonerNumber = REMOVE_PRISONER,
        current = DomesticStatusDetailsRequest(
          domesticStatusCode = keepingDomesticStatus,
          createdBy = "Admin",
          createdTime = latestCreatedDate,
        ),
      ),
    )

    createNumberOfChildrenRecords(
      MigratePrisonerNumberOfChildrenRequest(
        prisonerNumber = KEEP_PRISONER,
        current = NumberOfChildrenDetailsRequest(
          numberOfChildren = "2",
          createdBy = "Admin",
          createdTime = olderCreatedDate,
        ),
      ),
    )

    val keepingNumberOfChildren = "3"
    createNumberOfChildrenRecords(
      MigratePrisonerNumberOfChildrenRequest(
        prisonerNumber = REMOVE_PRISONER,
        current = NumberOfChildrenDetailsRequest(
          numberOfChildren = keepingNumberOfChildren,
          createdBy = "Admin",
          createdTime = latestCreatedDate,
        ),
      ),
    )

    performMerge()

    val retainedDomesticStatus = assertDomesticStatusPresent(KEEP_PRISONER, keepingDomesticStatus)
    assertDomesticStatusNotPresent(REMOVE_PRISONER)

    val retainedNumberOfChildren = assertNumberOfChildrenPresent(KEEP_PRISONER, keepingNumberOfChildren)
    assertNumberOfChildrenNotPresent(REMOVE_PRISONER)

    stubEvents.assertHasEvent(
      event = OutboundEvent.PRISONER_NUMBER_OF_CHILDREN_CREATED,
      additionalInfo = PrisonerNumberOfChildren(retainedNumberOfChildren.id, Source.DPS, "SYS", null),
      personReference = PersonReference(nomsNumber = KEEP_PRISONER),
    )

    stubEvents.assertHasEvent(
      event = OutboundEvent.PRISONER_DOMESTIC_STATUS_CREATED,
      additionalInfo = PrisonerDomesticStatus(retainedDomesticStatus.id, Source.DPS, "SYS", null),
      personReference = PersonReference(nomsNumber = KEEP_PRISONER),
    )
  }

  @Test
  fun `should merge when removing prisoner's active record is older than retaining prisoner's active record`() {
    val createdTime = LocalDateTime.now()
    val expectedDomesticStatus = "D"
    createDomesticStatusRecords(
      MigratePrisonerDomesticStatusRequest(
        prisonerNumber = KEEP_PRISONER,
        current = DomesticStatusDetailsRequest(
          domesticStatusCode = expectedDomesticStatus,
          createdBy = "Admin",
          createdTime = createdTime,
        ),
      ),
    )

    createDomesticStatusRecords(
      MigratePrisonerDomesticStatusRequest(
        prisonerNumber = REMOVE_PRISONER,
        current = DomesticStatusDetailsRequest(
          domesticStatusCode = "P",
          createdBy = "Admin",
          createdTime = createdTime.minusDays(1),
        ),
      ),
    )

    val expectedNumberOfChildren = "2"
    createNumberOfChildrenRecords(
      MigratePrisonerNumberOfChildrenRequest(
        prisonerNumber = KEEP_PRISONER,
        current = NumberOfChildrenDetailsRequest(
          numberOfChildren = expectedNumberOfChildren,
          createdBy = "Admin",
          createdTime = createdTime,
        ),
      ),
    )

    createNumberOfChildrenRecords(
      MigratePrisonerNumberOfChildrenRequest(
        prisonerNumber = REMOVE_PRISONER,
        current = NumberOfChildrenDetailsRequest(
          numberOfChildren = "3",
          createdBy = "Admin",
          createdTime = createdTime.minusDays(1),
        ),
      ),
    )

    performMerge()

    assertPrisonerMerge(expectedNumberOfChildren, expectedDomesticStatus)
    stubEvents.assertHasNoEvents(OutboundEvent.PRISONER_DOMESTIC_STATUS_CREATED)
    stubEvents.assertHasNoEvents(OutboundEvent.PRISONER_NUMBER_OF_CHILDREN_CREATED)
  }

  @Test
  fun `should merge when removing prisoner has no active records`() {
    val expectedNumberOfChildren = "4"
    val expectedDomesticStatus = "M"

    createNumberOfChildrenRecords(numberOfChildrenRequest(KEEP_PRISONER, expectedNumberOfChildren, listOf("3")))
    createDomesticStatusRecords(domesticStatusRequest(KEEP_PRISONER, expectedDomesticStatus, listOf("D")))

    performMerge()

    assertPrisonerMerge(expectedNumberOfChildren, expectedDomesticStatus)
    stubEvents.assertHasNoEvents(OutboundEvent.PRISONER_DOMESTIC_STATUS_CREATED)
    stubEvents.assertHasNoEvents(OutboundEvent.PRISONER_NUMBER_OF_CHILDREN_CREATED)
  }

  // --- Helper methods below ---

  private fun performMerge() {
    webTestClient.put()
      .uri(MERGE_URI)
      .headers(setAuthorisationUsingCurrentUser())
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isOk
  }

  private fun assertPrisonerMerge(
    expectedNumberOfChildren: String,
    expectedDomesticStatus: String,
  ) {
    assertDomesticStatusPresent(KEEP_PRISONER, expectedDomesticStatus)
    assertDomesticStatusNotPresent(REMOVE_PRISONER)
    assertNumberOfChildrenPresent(KEEP_PRISONER, expectedNumberOfChildren)
    assertNumberOfChildrenNotPresent(REMOVE_PRISONER)
  }

  private fun assertDomesticStatusNotPresent(prisonerNumber: String) {
    webTestClient.get()
      .uri("/sync/$prisonerNumber/domestic-status")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationUsingCurrentUser())
      .exchange()
      .expectStatus()
      .isNotFound
  }

  private fun assertDomesticStatusPresent(
    prisonerNumber: String,
    expectedValue: String,
  ): SyncPrisonerDomesticStatusResponse {
    val retainedDomesticStatus = webTestClient.get()
      .uri("/sync/$prisonerNumber/domestic-status")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationUsingCurrentUser())
      .exchange()
      .expectStatus()
      .isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(SyncPrisonerDomesticStatusResponse::class.java)
      .returnResult().responseBody!!

    assertThat(retainedDomesticStatus.id).isGreaterThan(0)
    assertThat(retainedDomesticStatus.domesticStatusCode).isEqualTo(expectedValue)
    assertThat(retainedDomesticStatus.createdBy).isEqualTo("Admin")
    assertThat(retainedDomesticStatus.createdTime).isNotNull
    assertThat(retainedDomesticStatus.active).isTrue
    return retainedDomesticStatus
  }

  private fun assertNumberOfChildrenNotPresent(prisonerNumber: String) {
    webTestClient.get()
      .uri("/sync/$prisonerNumber/number-of-children")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationUsingCurrentUser())
      .exchange()
      .expectStatus()
      .isNotFound
  }

  private fun assertNumberOfChildrenPresent(
    prisonerNumber: String,
    expectedValue: String,
  ): SyncPrisonerNumberOfChildrenResponse {
    val retainedNumberOfChildren = webTestClient.get()
      .uri("/sync/$prisonerNumber/number-of-children")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisationUsingCurrentUser())
      .exchange()
      .expectStatus()
      .isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(SyncPrisonerNumberOfChildrenResponse::class.java)
      .returnResult().responseBody!!

    assertThat(retainedNumberOfChildren.id).isGreaterThan(0)
    assertThat(retainedNumberOfChildren.numberOfChildren).isEqualTo(expectedValue)
    assertThat(retainedNumberOfChildren.createdBy).isEqualTo("Admin")
    assertThat(retainedNumberOfChildren.createdTime).isNotNull
    assertThat(retainedNumberOfChildren.active).isTrue
    return retainedNumberOfChildren
  }

  private fun createDomesticStatusRecords(request: MigratePrisonerDomesticStatusRequest) {
    webTestClient.post()
      .uri("/migrate/domestic-status")
      .headers(setAuthorisationUsingCurrentUser())
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(request)
      .exchange()
      .expectStatus()
      .isOk
  }

  private fun createNumberOfChildrenRecords(request: MigratePrisonerNumberOfChildrenRequest) {
    webTestClient.post()
      .uri("/migrate/number-of-children")
      .headers(setAuthorisationUsingCurrentUser())
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(request)
      .exchange()
      .expectStatus()
      .isOk
  }

  private fun domesticStatusRequest(
    prisonerNumber: String = KEEP_PRISONER,
    currentValue: String = "D",
    historyValues: List<String> = listOf("M"),
  ): MigratePrisonerDomesticStatusRequest {
    val history = historyValues.map {
      DomesticStatusDetailsRequest(
        domesticStatusCode = it,
        createdBy = "Admin",
        createdTime = LocalDateTime.now().minusDays(1),
      )
    }
    return MigratePrisonerDomesticStatusRequest(
      prisonerNumber = prisonerNumber,
      current = DomesticStatusDetailsRequest(
        domesticStatusCode = currentValue,
        createdBy = "Admin",
        createdTime = LocalDateTime.now(),
      ),
      history = history,
    )
  }

  private fun numberOfChildrenRequest(
    prisonerNumber: String = KEEP_PRISONER,
    currentValue: String = "2",
    historyValues: List<String> = listOf("1"),
  ): MigratePrisonerNumberOfChildrenRequest {
    val history = historyValues.map {
      NumberOfChildrenDetailsRequest(
        numberOfChildren = it,
        createdBy = "Admin",
        createdTime = LocalDateTime.now().minusDays(1),
      )
    }
    return MigratePrisonerNumberOfChildrenRequest(
      prisonerNumber = prisonerNumber,
      current = NumberOfChildrenDetailsRequest(
        numberOfChildren = currentValue,
        createdBy = "Admin",
        createdTime = LocalDateTime.now(),
      ),
      history = history,
    )
  }
}
