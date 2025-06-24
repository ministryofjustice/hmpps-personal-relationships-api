package uk.gov.justice.digital.hmpps.hmppscontactsapi.facade

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppscontactsapi.config.User
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncCreatePrisonerRestrictionRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncUpdatePrisonerRestrictionRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync.SyncPrisonerRestriction
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.PrisonerRestrictionsService
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.Source
import uk.gov.justice.digital.hmpps.hmppscontactsapi.util.UserUtil
import java.time.LocalDate
import java.time.LocalDateTime

class SyncPrisonerRestrictionFacadeTest {

  private val prisonerRestrictionsService: PrisonerRestrictionsService = mock()
  private val outboundEventsService: OutboundEventsService = mock()
  private val userUtil: UserUtil = mock()
  private lateinit var facade: SyncPrisonerRestrictionFacade

  @BeforeEach
  fun setUp() {
    facade = SyncPrisonerRestrictionFacade(prisonerRestrictionsService, outboundEventsService, userUtil)
  }

  @Test
  fun `should get prisoner restriction by id`() {
    val restriction = sampleRestriction()
    whenever(prisonerRestrictionsService.getPrisonerRestrictionById(1L)).thenReturn(restriction)

    val result = facade.getPrisonerRestrictionById(1L)

    assertThat(result).isEqualTo(restriction)
  }

  @Test
  fun `should create prisoner restriction and fire event`() {
    val request = syncCreatePrisonerRestrictionRequest()
    val restriction = sampleRestriction()
    whenever(prisonerRestrictionsService.createPrisonerRestriction(request)).thenReturn(restriction)
    whenever(userUtil.userOrDefault(restriction.createdBy)).thenReturn(User(restriction.createdBy))
    val result = facade.createPrisonerRestriction(request)

    assertThat(result).isEqualTo(restriction)
    verify(outboundEventsService).send(
      outboundEvent = OutboundEvent.PRISONER_RESTRICTIONS_CREATED,
      identifier = restriction.prisonerRestrictionId,
      noms = restriction.prisonerNumber,
      source = Source.NOMIS,
      user = User(restriction.createdBy),
    )
  }

  @Test
  fun `should update prisoner restriction and fire event`() {
    val request = syncUpdatePrisonerRestrictionRequest()
    val restriction = sampleRestriction()
    whenever(prisonerRestrictionsService.updatePrisonerRestriction(1L, request)).thenReturn(restriction)

    whenever(userUtil.userOrDefault(request.updatedBy)).thenReturn(User(request.updatedBy!!))
    val result = facade.updatePrisonerRestriction(1L, request)

    assertThat(result).isEqualTo(restriction)
    verify(outboundEventsService).send(
      outboundEvent = OutboundEvent.PRISONER_RESTRICTIONS_UPDATED,
      identifier = restriction.prisonerRestrictionId,
      noms = restriction.prisonerNumber,
      source = Source.NOMIS,
      user = User(request.updatedBy!!),
    )
  }

  @Test
  fun `should delete prisoner restriction and fire event`() {
    val restriction = sampleRestriction()
    whenever(prisonerRestrictionsService.deletePrisonerRestriction(1L)).thenReturn(restriction)
    whenever(userUtil.userOrDefault()).thenReturn(User.SYS_USER)
    facade.deletePrisonerRestriction(1L)

    verify(outboundEventsService).send(
      outboundEvent = OutboundEvent.PRISONER_RESTRICTIONS_DELETED,
      identifier = restriction.prisonerRestrictionId,
      noms = restriction.prisonerNumber,
      source = Source.NOMIS,
      user = User.SYS_USER,
    )
  }

  private fun syncCreatePrisonerRestrictionRequest() = SyncCreatePrisonerRestrictionRequest(
    prisonerNumber = "A1234BC",
    restrictionType = "NO_VISIT",
    effectiveDate = LocalDate.of(2024, 6, 11),
    expiryDate = LocalDate.of(2024, 12, 31),
    commentText = "No visits allowed",
    authorisedUsername = "JSMITH",
    currentTerm = true,
    createdBy = "JSMITH_ADM",
    createdTime = LocalDateTime.of(2024, 6, 11, 10, 0),
  )

  private fun syncUpdatePrisonerRestrictionRequest() = SyncUpdatePrisonerRestrictionRequest(
    prisonerNumber = "A1234BC",
    restrictionType = "NO_VISIT",
    effectiveDate = LocalDate.of(2024, 6, 11),
    expiryDate = LocalDate.of(2024, 12, 31),
    commentText = "No visits allowed",
    authorisedUsername = "JSMITH",
    currentTerm = true,
    updatedBy = "USER_UPDATE",
    updatedTime = LocalDateTime.of(2025, 5, 11, 10, 0),
  )

  private fun sampleRestriction() = SyncPrisonerRestriction(
    prisonerRestrictionId = 1L,
    prisonerNumber = "A1234BC",
    restrictionType = "NO_VISIT",
    effectiveDate = LocalDate.of(2024, 6, 11),
    expiryDate = LocalDate.of(2024, 12, 31),
    commentText = "No visits allowed",
    authorisedUsername = "JSMITH",
    createdBy = "JSMITH_ADM",
    createdTime = LocalDateTime.of(2024, 6, 11, 10, 0),
    currentTerm = true,
    updatedBy = null,
    updatedTime = null,
  )
}
