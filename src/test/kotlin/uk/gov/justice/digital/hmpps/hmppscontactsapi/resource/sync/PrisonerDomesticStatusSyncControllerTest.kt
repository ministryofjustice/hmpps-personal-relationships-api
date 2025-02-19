package uk.gov.justice.digital.hmpps.hmppscontactsapi.resource.sync

import com.github.tomakehurst.wiremock.admin.NotFoundException
import jakarta.validation.ValidationException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import uk.gov.justice.digital.hmpps.hmppscontactsapi.facade.sync.PrisonerDomesticStatusSyncFacade
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncUpdatePrisonerDomesticStatusRequest
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class PrisonerDomesticStatusSyncControllerTest {

  @Mock
  private lateinit var prisonerDomesticStatusSyncFacade: PrisonerDomesticStatusSyncFacade

  @InjectMocks
  private lateinit var domesticStatusSyncController: PrisonerDomesticStatusSyncController

  @Test
  fun `syncUpdateDomesticStatus should delegate to facade`() {
    // Given
    val prisonerNumber = "A1234BC"
    val request = SyncUpdatePrisonerDomesticStatusRequest(
      prisonerNumber = prisonerNumber,
      domesticStatusCode = "G",
      createdBy = "Admin",
      createdTime = LocalDateTime.now(),
    )

    // When
    domesticStatusSyncController.syncUpdateDomesticStatus(prisonerNumber, request)

    // Then
    verify(prisonerDomesticStatusSyncFacade).updateDomesticStatus(prisonerNumber, request)
    verifyNoMoreInteractions(prisonerDomesticStatusSyncFacade)
  }

  @Test
  fun `syncDeleteDomesticStatusById should delegate to facade`() {
    // Given
    val prisonerNumber = "A1234BC"

    // When
    domesticStatusSyncController.syncDeleteDomesticStatusById(prisonerNumber)

    // Then
    verify(prisonerDomesticStatusSyncFacade).deleteDomesticStatus(prisonerNumber)
    verifyNoMoreInteractions(prisonerDomesticStatusSyncFacade)
  }

  @Test
  fun `syncDeleteDomesticStatusById should handle not found scenario`() {
    // Given
    val prisonerNumber = "A1234BC"
    doThrow(NotFoundException("Domestic status not found"))
      .`when`(prisonerDomesticStatusSyncFacade).deleteDomesticStatus(prisonerNumber)

    // When/Then
    assertThrows<NotFoundException> {
      domesticStatusSyncController.syncDeleteDomesticStatusById(prisonerNumber)
    }
  }

  @Test
  fun `syncUpdateDomesticStatus should handle validation errors`() {
    // Given
    val prisonerNumber = "A1234BC"
    val request = SyncUpdatePrisonerDomesticStatusRequest(
      prisonerNumber = prisonerNumber,
      domesticStatusCode = "G",
      createdBy = "Admin",
      createdTime = LocalDateTime.now(),
    )
    doThrow(ValidationException("Invalid request"))
      .`when`(prisonerDomesticStatusSyncFacade).updateDomesticStatus(prisonerNumber, request)

    // When/Then
    assertThrows<ValidationException> {
      domesticStatusSyncController.syncUpdateDomesticStatus(prisonerNumber, request)
    }
  }
}
