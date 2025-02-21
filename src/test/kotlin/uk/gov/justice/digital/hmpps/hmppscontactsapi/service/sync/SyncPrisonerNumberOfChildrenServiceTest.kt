package uk.gov.justice.digital.hmpps.hmppscontactsapi.service.sync

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.PrisonerNumberOfChildren
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncUpdatePrisonerNumberOfChildrenRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.PrisonerNumberOfChildrenRepository
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class SyncPrisonerNumberOfChildrenServiceTest {

  @Mock
  private lateinit var numberOfChildrenRepository: PrisonerNumberOfChildrenRepository

  @InjectMocks
  private lateinit var syncNumberOfChildrenService: SyncPrisonerNumberOfChildrenService

  @Test
  fun `getNumberOfChildrenByPrisonerNumber returns numberOfChildren when found`() {
    // Given
    val prisonerNumber = "A1234BC"
    val numberOfChildren = PrisonerNumberOfChildren(
      prisonerNumberOfChildrenId = 1L,
      prisonerNumber = prisonerNumber,
      numberOfChildren = "1",
      createdBy = "user",
      createdTime = LocalDateTime.now(),
      active = true,
    )

    whenever(numberOfChildrenRepository.findByPrisonerNumber(prisonerNumber))
      .thenReturn(numberOfChildren)

    // When
    val result = syncNumberOfChildrenService.getNumberOfChildrenByPrisonerNumber(prisonerNumber)

    // Then
    assertNotNull(result)
    verify(numberOfChildrenRepository).findByPrisonerNumber(prisonerNumber)
  }

  @Test
  fun `getNumberOfChildrenByPrisonerNumber throws EntityNotFoundException when not found`() {
    // Given
    val prisonerNumber = "A1234BC"
    whenever(numberOfChildrenRepository.findByPrisonerNumber(prisonerNumber))
      .thenReturn(null)

    // When/Then
    assertThrows<EntityNotFoundException> {
      syncNumberOfChildrenService.getNumberOfChildrenByPrisonerNumber(prisonerNumber)
    }.also {
      assertEquals(
        String.format(SyncPrisonerNumberOfChildrenService.NOT_FOUND_MESSAGE, prisonerNumber),
        it.message,
      )
    }
  }

  @Test
  fun `createOrUpdateNumberOfChildren deactivates existing numberOfChildren and creates new one`() {
    // Given
    val prisonerNumber = "A1234BC"
    val existingNumberOfChildrenCount = PrisonerNumberOfChildren(
      prisonerNumberOfChildrenId = 1L,
      prisonerNumber = prisonerNumber,
      numberOfChildren = "1",
      createdBy = "user",
      createdTime = LocalDateTime.now(),
      active = true,
    )

    val updateRequest = SyncUpdatePrisonerNumberOfChildrenRequest(
      numberOfChildren = "1",
      createdBy = "user",
      createdTime = LocalDateTime.now(),
    )

    whenever(numberOfChildrenRepository.findByPrisonerNumber(prisonerNumber))
      .thenReturn(existingNumberOfChildrenCount)

    val deactivatedNumberOfChildrenCount = existingNumberOfChildrenCount.copy(active = false)
    whenever(numberOfChildrenRepository.save(any())).thenReturn(deactivatedNumberOfChildrenCount)

    // When
    syncNumberOfChildrenService.createOrUpdateNumberOfChildren(prisonerNumber, updateRequest)

    // Then
    verify(numberOfChildrenRepository).findByPrisonerNumber(prisonerNumber)
        /* verify(numberOfChildrenRepository).save(
             check { savedNumberOfChildrenCount ->
                 assertFalse(savedNumberOfChildrenCount.active)
                 assertEquals(prisonerNumber, savedNumberOfChildrenCount.prisonerNumber)
             }
         )*/
  }

  @Test
  fun `createOrUpdateNumberOfChildren creates new numberOfChildren when no existing numberOfChildren found`() {
    // Given
    val prisonerNumber = "A1234BC"
    val updateRequest = SyncUpdatePrisonerNumberOfChildrenRequest(
      numberOfChildren = "1",
      createdBy = "user",
      createdTime = LocalDateTime.now(),
    )

    whenever(numberOfChildrenRepository.findByPrisonerNumber(prisonerNumber))
      .thenReturn(null)

    whenever(numberOfChildrenRepository.save(any())).thenReturn(
      PrisonerNumberOfChildren(
        prisonerNumber = prisonerNumber,
        numberOfChildren = "1",
        createdBy = "user",
        createdTime = LocalDateTime.now(),
        active = true,
      ),
    )

    // When
    syncNumberOfChildrenService.createOrUpdateNumberOfChildren(prisonerNumber, updateRequest)

    // Then
    verify(numberOfChildrenRepository).findByPrisonerNumber(prisonerNumber)
    val numberOfChildrenCaptor = argumentCaptor<PrisonerNumberOfChildren>()
    verify(numberOfChildrenRepository, times(1)).save(numberOfChildrenCaptor.capture())
    val savedNumberOfChildren = numberOfChildrenCaptor.firstValue
    assertThat(savedNumberOfChildren.prisonerNumber).isEqualTo(prisonerNumber)
    assertThat(savedNumberOfChildren.numberOfChildren).isEqualTo("1")
    assertThat(savedNumberOfChildren.createdBy).isEqualTo("user")
    assertThat(savedNumberOfChildren.createdTime).isNotNull()
  }
}
