package uk.gov.justice.digital.hmpps.hmppscontactsapi.facade.sync

import PrisonerRestrictionId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.web.PagedModel
import uk.gov.justice.digital.hmpps.hmppscontactsapi.config.User
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.migrate.PrisonerRestrictionDetailsRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.MergePrisonerRestrictionsRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.ResetPrisonerRestrictionsRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.ChangedRestrictionsResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events.Source
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.sync.PrisonerRestrictionsAdminService
import java.time.LocalDate
import java.time.LocalDateTime

class PrisonerRestrictionsAdminFacadeTest {
  companion object {
    private val createdRestrictions = listOf(1L, 2L)
    private val deletedRestrictions = listOf(3L, 4L)
  }
  private val mergeService = mock<PrisonerRestrictionsAdminService>()
  private val outboundEventsService = mock<OutboundEventsService>()
  private val facade = PrisonerRestrictionsAdminFacade(mergeService, outboundEventsService)

  @Nested
  inner class MergeRestrictions {
    @Test
    fun `merge sends correct events`() {
      val keepingPrisonerNumber = "A1234BC"
      val removingPrisonerNumber = "A4567BC"
      val mergeRequest = MergePrisonerRestrictionsRequest(
        keepingPrisonerNumber = keepingPrisonerNumber,
        removingPrisonerNumber = removingPrisonerNumber,
        restrictions = listOf(
          prisonerRestrictionDetailsRequest(),
        ),
      )
      val mergePrisonerRestrictionsResponse = ChangedRestrictionsResponse(
        hasChanged = true,
        createdRestrictions = createdRestrictions,
        deletedRestrictions = deletedRestrictions,
      )
      whenever(
        mergeService.mergePrisonerRestrictions(mergeRequest),
      ).thenReturn(mergePrisonerRestrictionsResponse)

      facade.merge(mergeRequest)

      // Verify individual CREATED and DELETED events are sent
      createdRestrictions.forEach { id ->
        verify(outboundEventsService).send(
          outboundEvent = OutboundEvent.PRISONER_RESTRICTION_CREATED,
          identifier = id,
          source = Source.NOMIS,
          noms = keepingPrisonerNumber,
          user = User.SYS_USER,
        )
      }
      deletedRestrictions.forEach { id ->
        verify(outboundEventsService).send(
          outboundEvent = OutboundEvent.PRISONER_RESTRICTION_DELETED,
          identifier = id,
          source = Source.NOMIS,
          noms = removingPrisonerNumber,
          user = User.SYS_USER,
        )
      }
    }

    @Test
    fun `should not send event`() {
      val keepingPrisonerNumber = "A1234BC"
      val removingPrisonerNumber = "A4567BC"
      val mergeRequest = MergePrisonerRestrictionsRequest(
        keepingPrisonerNumber = keepingPrisonerNumber,
        removingPrisonerNumber = removingPrisonerNumber,
        restrictions = listOf(
          prisonerRestrictionDetailsRequest(),
        ),
      )
      whenever(mergeService.mergePrisonerRestrictions(mergeRequest)).thenReturn(
        ChangedRestrictionsResponse(
          hasChanged = false,
          createdRestrictions = createdRestrictions,
          deletedRestrictions = deletedRestrictions,
        ),
      )
      facade.merge(mergeRequest)
      verifyNoMoreInteractions(outboundEventsService)
    }

    private fun prisonerRestrictionDetailsRequest() = PrisonerRestrictionDetailsRequest(
      restrictionType = "NO_VISIT",
      effectiveDate = LocalDate.now(),
      expiryDate = LocalDate.now().plusDays(1),
      commentText = "Test comment",
      currentTerm = true,
      authorisedUsername = "user",
      createdBy = "user",
      createdTime = LocalDateTime.now(),
      updatedBy = "user",
      updatedTime = LocalDateTime.now(),
    )
  }

  @Nested
  inner class ResetRestrictions {

    @Test
    fun `should send events when restrictions were reset`() {
      // Given
      val request = createRequest()
      val response = ChangedRestrictionsResponse(
        hasChanged = true,
        createdRestrictions = createdRestrictions,
        deletedRestrictions = deletedRestrictions,
      )

      whenever(mergeService.resetPrisonerRestrictions(request)).thenReturn(response)

      // When
      facade.reset(request)

      // Then
      verify(mergeService).resetPrisonerRestrictions(request)

      // Verify individual CREATED and DELETED events are sent
      createdRestrictions.forEach { id ->
        verify(outboundEventsService).send(
          outboundEvent = OutboundEvent.PRISONER_RESTRICTION_CREATED,
          identifier = id,
          source = Source.NOMIS,
          noms = request.prisonerNumber,
          user = User.SYS_USER,
        )
      }

      deletedRestrictions.forEach { id ->
        verify(outboundEventsService).send(
          outboundEvent = OutboundEvent.PRISONER_RESTRICTION_DELETED,
          identifier = id,
          source = Source.NOMIS,
          noms = request.prisonerNumber,
          user = User.SYS_USER,
        )
      }
    }

    @Test
    fun `should not send events when no restrictions were reset`() {
      // Given
      val request = createRequest()
      val response = ChangedRestrictionsResponse(
        hasChanged = false,
        createdRestrictions = createdRestrictions,
        deletedRestrictions = deletedRestrictions,
      )

      whenever(mergeService.resetPrisonerRestrictions(request)).thenReturn(response)

      // When
      facade.reset(request)

      // Then
      verify(mergeService).resetPrisonerRestrictions(request)
      verifyNoMoreInteractions(outboundEventsService)
    }

    private fun createRequest() = ResetPrisonerRestrictionsRequest(
      prisonerNumber = "A1234BC",
      restrictions = listOf(
        PrisonerRestrictionDetailsRequest(
          restrictionType = "NO_VISIT",
          effectiveDate = LocalDate.now(),
          expiryDate = LocalDate.now().plusDays(1),
          commentText = "Test comment",
          currentTerm = true,
          authorisedUsername = "user",
          createdBy = "user",
          createdTime = LocalDateTime.now(),
          updatedBy = "user",
          updatedTime = LocalDateTime.now(),
        ),
      ),
    )
  }

  @Nested
  inner class GetAllRestrictionIds {
    @Test
    fun `should delegate to service and return paged restriction IDs`() {
      val pageable = PageRequest.of(0, 10)
      val expected = PageImpl(listOf(PrisonerRestrictionId(1L), PrisonerRestrictionId(2L)), pageable, 2)
      whenever(mergeService.getAllRestrictionIds(pageable)).thenReturn(expected)
      val result = facade.getAllRestrictionIds(pageable)
      assertThat(result).isEqualTo(
        PagedModel(expected),
      )
      verify(mergeService).getAllRestrictionIds(pageable)
    }
  }
}
