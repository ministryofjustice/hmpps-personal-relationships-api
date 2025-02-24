package uk.gov.justice.digital.hmpps.hmppscontactsapi.service.sync

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
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

    whenever(numberOfChildrenRepository.findByPrisonerNumberAndActive(prisonerNumber, true))
      .thenReturn(numberOfChildren)

    // When
    val result = syncNumberOfChildrenService.getNumberOfChildrenByPrisonerNumber(prisonerNumber)

    // Then
    assertThat(result).isNotNull()
    assertThat(result.id).isEqualTo(numberOfChildren.prisonerNumberOfChildrenId)
    verify(numberOfChildrenRepository).findByPrisonerNumberAndActive(prisonerNumber, true)
  }

  @Test
  fun `getNumberOfChildrenByPrisonerNumber throws EntityNotFoundException when not found`() {
    // Given
    val prisonerNumber = "A1234BC"
    whenever(numberOfChildrenRepository.findByPrisonerNumberAndActive(prisonerNumber, true))
      .thenReturn(null)

    // When/Then
    val exception = assertThrows<EntityNotFoundException> {
      syncNumberOfChildrenService.getNumberOfChildrenByPrisonerNumber(prisonerNumber)
    }
    assertThat(exception.message).isEqualTo("NumberOfChildren not found for prisoner: A1234BC")
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

    whenever(numberOfChildrenRepository.findByPrisonerNumberAndActive(prisonerNumber, true))
      .thenReturn(existingNumberOfChildrenCount)

    val deactivatedNumberOfChildrenCount = existingNumberOfChildrenCount.copy(active = false)
    whenever(numberOfChildrenRepository.save(any())).thenReturn(deactivatedNumberOfChildrenCount)

    // When
    val response = syncNumberOfChildrenService.createOrUpdateNumberOfChildren(prisonerNumber, updateRequest)

    // Then
    verify(numberOfChildrenRepository).findByPrisonerNumberAndActive(prisonerNumber, true)
    assertThat(response.id).isEqualTo(1L)
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

    whenever(numberOfChildrenRepository.findByPrisonerNumberAndActive(prisonerNumber, true))
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
    verify(numberOfChildrenRepository).findByPrisonerNumberAndActive(prisonerNumber, true)
    val numberOfChildrenCaptor = argumentCaptor<PrisonerNumberOfChildren>()
    verify(numberOfChildrenRepository, times(1)).save(numberOfChildrenCaptor.capture())
    val savedNumberOfChildren = numberOfChildrenCaptor.firstValue
    assertThat(savedNumberOfChildren.prisonerNumber).isEqualTo(prisonerNumber)
    assertThat(savedNumberOfChildren.numberOfChildren).isEqualTo("1")
    assertThat(savedNumberOfChildren.createdBy).isEqualTo("user")
    assertThat(savedNumberOfChildren.createdTime).isNotNull()
  }

  @Test
  fun `deactivateDomesticStatus deactivates existing status`() {
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

    whenever(numberOfChildrenRepository.findByPrisonerNumberAndActive(prisonerNumber, true))
      .thenReturn(existingNumberOfChildrenCount)

    // When
    syncNumberOfChildrenService.deactivateNumberOfChildren(prisonerNumber)

    // Then
    val nocCaptor = argumentCaptor<PrisonerNumberOfChildren>()
    verify(numberOfChildrenRepository).save(nocCaptor.capture())
    val savedDomesticStatus = nocCaptor.firstValue
    assertThat(savedDomesticStatus.active).isFalse()
    assertThat(savedDomesticStatus.prisonerNumber).isEqualTo(prisonerNumber)
  }

  @Test
  fun `deactivateNumberOfChildren throws EntityNotFoundException when not found`() {
    // Given
    val prisonerNumber = "A1234BC"
    whenever(numberOfChildrenRepository.findByPrisonerNumberAndActive(prisonerNumber, true))
      .thenReturn(null)

    // When/Then
    val exception = assertThrows<EntityNotFoundException> {
      syncNumberOfChildrenService.deactivateNumberOfChildren(prisonerNumber)
    }
    assertThat(exception.message).isEqualTo("NumberOfChildren not found for prisoner: A1234BC")
  }
}
