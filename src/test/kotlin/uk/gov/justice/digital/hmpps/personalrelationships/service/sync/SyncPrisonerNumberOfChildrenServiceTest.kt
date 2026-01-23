package uk.gov.justice.digital.hmpps.personalrelationships.service.sync

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
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.personalrelationships.entity.PrisonerNumberOfChildren
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.sync.SyncUpdatePrisonerNumberOfChildrenRequest
import uk.gov.justice.digital.hmpps.personalrelationships.repository.PrisonerNumberOfChildrenRepository
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

    whenever(numberOfChildrenRepository.findByPrisonerNumberAndActiveTrue(prisonerNumber))
      .thenReturn(numberOfChildren)

    // When
    val result = syncNumberOfChildrenService.getNumberOfChildrenByPrisonerNumber(prisonerNumber)

    // Then
    assertThat(result).isNotNull()
    assertThat(result.id).isEqualTo(numberOfChildren.prisonerNumberOfChildrenId)
    assertThat(result.numberOfChildren).isEqualTo(numberOfChildren.numberOfChildren)
    assertThat(result.createdBy).isEqualTo(numberOfChildren.createdBy)
    assertThat(result.active).isEqualTo(numberOfChildren.active)
    verify(numberOfChildrenRepository).findByPrisonerNumberAndActiveTrue(prisonerNumber)
  }

  @Test
  fun `getNumberOfChildrenByPrisonerNumber throws EntityNotFoundException when not found`() {
    // Given
    val prisonerNumber = "A1234BC"
    whenever(numberOfChildrenRepository.findByPrisonerNumberAndActiveTrue(prisonerNumber))
      .thenReturn(null)

    // When/Then
    val exception = assertThrows<EntityNotFoundException> {
      syncNumberOfChildrenService.getNumberOfChildrenByPrisonerNumber(prisonerNumber)
    }
    assertThat(exception.message).isEqualTo("Could not find the number of children for prisoner: A1234BC")
  }

  @Test
  fun `createOrUpdateDomesticStatus deactivates existing status and creates new one when existing value is different`() {
    // Given
    val prisonerNumber = "A1234BC"
    val existingNumberOfChildrenCount = PrisonerNumberOfChildren(
      prisonerNumberOfChildrenId = 1L,
      prisonerNumber = prisonerNumber,
      numberOfChildren = "0",
      createdBy = "user",
      createdTime = LocalDateTime.now(),
      active = true,
    )

    val updateRequest = SyncUpdatePrisonerNumberOfChildrenRequest(
      numberOfChildren = "1",
      createdBy = "user",
      createdTime = LocalDateTime.now(),
    )

    whenever(numberOfChildrenRepository.findByPrisonerNumberAndActiveTrue(prisonerNumber))
      .thenReturn(existingNumberOfChildrenCount)

    val deactivatedNumberOfChildrenCount = existingNumberOfChildrenCount.copy(active = false)
    whenever(numberOfChildrenRepository.save(any())).thenReturn(deactivatedNumberOfChildrenCount)

    // When
    val response = syncNumberOfChildrenService.createOrUpdateNumberOfChildren(prisonerNumber, updateRequest)

    // Then
    verify(numberOfChildrenRepository).findByPrisonerNumberAndActiveTrue(prisonerNumber)
    assertThat(response.data.id).isEqualTo(1L)
  }

  @Test
  fun `createOrUpdateDomesticStatus unchanged existing status when existing value is same`() {
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

    whenever(numberOfChildrenRepository.findByPrisonerNumberAndActiveTrue(prisonerNumber))
      .thenReturn(existingNumberOfChildrenCount)

    // When
    syncNumberOfChildrenService.createOrUpdateNumberOfChildren(prisonerNumber, updateRequest)

    // Then
    verify(numberOfChildrenRepository).findByPrisonerNumberAndActiveTrue(prisonerNumber)
    verify(numberOfChildrenRepository, never()).save(any())
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

    whenever(numberOfChildrenRepository.findByPrisonerNumberAndActiveTrue(prisonerNumber))
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
    verify(numberOfChildrenRepository).findByPrisonerNumberAndActiveTrue(prisonerNumber)
    val numberOfChildrenCaptor = argumentCaptor<PrisonerNumberOfChildren>()
    verify(numberOfChildrenRepository, times(1)).save(numberOfChildrenCaptor.capture())
    val savedNumberOfChildren = numberOfChildrenCaptor.firstValue
    assertThat(savedNumberOfChildren.prisonerNumber).isEqualTo(prisonerNumber)
    assertThat(savedNumberOfChildren.numberOfChildren).isEqualTo("1")
    assertThat(savedNumberOfChildren.createdBy).isEqualTo("user")
    assertThat(savedNumberOfChildren.createdTime).isNotNull()
  }

  @Test
  fun `should return active existing number of children`() {
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

    whenever(numberOfChildrenRepository.findByPrisonerNumberAndActiveTrue(prisonerNumber))
      .thenReturn(numberOfChildren)

    // When
    val result = syncNumberOfChildrenService.getPrisonerNumberOfChildrenActive(prisonerNumber)

    // Then
    assertThat(result?.prisonerNumberOfChildrenId).isEqualTo(numberOfChildren.prisonerNumberOfChildrenId)
    verify(numberOfChildrenRepository).findByPrisonerNumberAndActiveTrue(prisonerNumber)
  }

  @Test
  fun `should not return number of children when there are no active records`() {
    // Given
    val prisonerNumber = "A1234BC"
    whenever(numberOfChildrenRepository.findByPrisonerNumberAndActiveTrue(prisonerNumber))
      .thenReturn(null)

    // When
    val result = syncNumberOfChildrenService.getPrisonerNumberOfChildrenActive(prisonerNumber)

    // Then
    assertThat(result).isNull()
    verify(numberOfChildrenRepository).findByPrisonerNumberAndActiveTrue(prisonerNumber)
  }
}
