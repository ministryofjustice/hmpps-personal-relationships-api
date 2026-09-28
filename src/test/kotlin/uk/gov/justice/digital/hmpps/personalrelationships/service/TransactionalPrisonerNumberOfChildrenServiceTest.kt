package uk.gov.justice.digital.hmpps.personalrelationships.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.check
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.personalrelationships.entity.PrisonerNumberOfChildren
import uk.gov.justice.digital.hmpps.personalrelationships.helpers.aUser
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.CreateOrUpdatePrisonerNumberOfChildrenRequest
import uk.gov.justice.digital.hmpps.personalrelationships.repository.PrisonerNumberOfChildrenRepository
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class TransactionalPrisonerNumberOfChildrenServiceTest {

  @Mock
  private lateinit var prisonerService: PrisonerService

  @Mock
  private lateinit var prisonerNumberOfChildrenRepository: PrisonerNumberOfChildrenRepository

  @InjectMocks
  private lateinit var transactionalPrisonerNumberOfChildrenService: TransactionalPrisonerNumberOfChildrenService

  private val prisonerNumber = "A1234BC"
  private val user = aUser("test-user")

  @Nested
  inner class CreateOrUpdateNumberOfChildrenByPrisonerNumber {

    @Test
    fun `creates new numberOfChildren when none exists`() {
      // Given
      val request = CreateOrUpdatePrisonerNumberOfChildrenRequest(
        numberOfChildren = 1,
      )

      val newNumberOfChildrenCount = PrisonerNumberOfChildren(
        prisonerNumberOfChildrenId = 1,
        prisonerNumber = prisonerNumber,
        numberOfChildren = "1",
        active = true,
        createdBy = "USER1",
        createdTime = LocalDateTime.now(),
      )

      whenever(prisonerNumberOfChildrenRepository.save<PrisonerNumberOfChildren>(any()))
        .thenReturn(newNumberOfChildrenCount)

      // When
      val result = transactionalPrisonerNumberOfChildrenService.createOrUpdateNumberOfChildren(
        prisonerNumber,
        null,
        request,
        user,
      )

      // Then
      with(result) {
        assertThat(prisonerNumber).isEqualTo(prisonerNumber)
        assertThat(numberOfChildren).isEqualTo("1")
        assertThat(active).isTrue
      }
      verify(prisonerNumberOfChildrenRepository, times(1)).save(any())
    }

    @Test
    fun `should create new number of children with null value`() {
      // Given
      val request = CreateOrUpdatePrisonerNumberOfChildrenRequest(
        numberOfChildren = null,
      )

      val newNumberOfChildren = PrisonerNumberOfChildren(
        prisonerNumberOfChildrenId = 1,
        prisonerNumber = prisonerNumber,
        numberOfChildren = null,
        active = true,
        createdBy = "USER1",
        createdTime = LocalDateTime.now(),
      )

      whenever(prisonerNumberOfChildrenRepository.save<PrisonerNumberOfChildren>(any()))
        .thenReturn(newNumberOfChildren)

      // When
      val result = transactionalPrisonerNumberOfChildrenService.createOrUpdateNumberOfChildren(
        prisonerNumber,
        null,
        request,
        user,
      )

      // Then
      with(result) {
        assertThat(prisonerNumber).isEqualTo(prisonerNumber)
        assertThat(numberOfChildren).isEqualTo(null)
        assertThat(active).isTrue
      }
      verify(prisonerNumberOfChildrenRepository, times(1)).save(any())
    }

    @Test
    fun `should deactivates existing numberOfChildren and creates new one`() {
      // Given
      val existingNumberOfChildrenCount = PrisonerNumberOfChildren(
        prisonerNumberOfChildrenId = 1,
        prisonerNumber = prisonerNumber,
        numberOfChildren = "1",
        active = true,
        createdBy = "USER1",
        createdTime = LocalDateTime.now(),
      )

      val request = CreateOrUpdatePrisonerNumberOfChildrenRequest(
        numberOfChildren = 1,
      )

      whenever(prisonerNumberOfChildrenRepository.save<PrisonerNumberOfChildren>(any()))
        .thenReturn(existingNumberOfChildrenCount)
        .thenReturn(existingNumberOfChildrenCount.copy(numberOfChildren = "2", active = true))

      // When
      val result = transactionalPrisonerNumberOfChildrenService.createOrUpdateNumberOfChildren(
        prisonerNumber,
        existingNumberOfChildrenCount,
        request,
        user,
      )

      verify(prisonerNumberOfChildrenRepository, times(1)).save(
        check { savedNumberOfChildrenCount ->
          assertThat(savedNumberOfChildrenCount.active).isFalse()
          assertThat(savedNumberOfChildrenCount.numberOfChildren).isEqualTo("1")
        },
      )

      verify(prisonerNumberOfChildrenRepository, times(1)).save(
        check { savedNumberOfChildrenCount ->
          assertThat(savedNumberOfChildrenCount.active).isTrue()
          assertThat(savedNumberOfChildrenCount.numberOfChildren).isEqualTo("1")
        },
      )
      // new code is returned
      with(result) {
        assertThat(prisonerNumber).isEqualTo(prisonerNumber)
        assertThat(numberOfChildren).isEqualTo("2")
        assertThat(active).isTrue
      }
    }
  }
}
