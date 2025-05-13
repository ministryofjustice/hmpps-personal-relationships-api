package uk.gov.justice.digital.hmpps.hmppscontactsapi.service

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.check
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.PrisonerNumberOfChildren
import uk.gov.justice.digital.hmpps.hmppscontactsapi.helpers.aUser
import uk.gov.justice.digital.hmpps.hmppscontactsapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppscontactsapi.helpers.prisoner
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.CreateOrUpdatePrisonerNumberOfChildrenRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.PrisonerNumberOfChildrenRepository
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class PrisonerNumberOfChildrenServiceTest {

  @Mock
  private lateinit var prisonerNumberOfChildrenRepository: PrisonerNumberOfChildrenRepository

  @Mock
  private lateinit var prisonerService: PrisonerService

  @InjectMocks
  private lateinit var prisonerNumberOfChildrenService: PrisonerNumberOfChildrenService

  private val prisonerNumber = "A1234BC"
  private val user = aUser("test-user")

  @Nested
  inner class GetNumberOfChildrenByPrisonerNumber {

    @Test
    fun `should returns correct response when numberOfChildren exists`() {
      // Given
      val newNumberOfChildrenCount = PrisonerNumberOfChildren(
        prisonerNumberOfChildrenId = 1,
        prisonerNumber = prisonerNumber,
        numberOfChildren = "1",
        active = true,
        createdBy = "USER1",
        createdTime = LocalDateTime.now(),
      )

      whenever(prisonerNumberOfChildrenRepository.findByPrisonerNumberAndActiveTrue(prisonerNumber))
        .thenReturn(newNumberOfChildrenCount)

      // When
      val result = prisonerNumberOfChildrenService.getNumberOfChildren(prisonerNumber)

      // Then
      with(result) {
        assertThat(prisonerNumber).isEqualTo(prisonerNumber)
        assertThat(numberOfChildren).isEqualTo("1")
        assertThat(active).isTrue
      }
    }

    @Test
    fun `should throws EntityNotFoundException when numberOfChildren does not exist`() {
      // Given
      whenever(prisonerNumberOfChildrenRepository.findByPrisonerNumberAndActiveTrue(prisonerNumber))
        .thenReturn(null)

      // When/Then
      assertThrows<EntityNotFoundException> {
        prisonerNumberOfChildrenService.getNumberOfChildren(prisonerNumber)
      }.message isEqualTo ("No number of children found for prisoner number: $prisonerNumber")
    }
  }

  @Nested
  inner class CreateOrUpdateNumberOfChildrenByPrisonerNumber {

    @Test
    fun `creates new numberOfChildren when none exists`() {
      // Given
      val request = CreateOrUpdatePrisonerNumberOfChildrenRequest(
        numberOfChildren = 1,
      )

      whenever(prisonerService.getPrisoner(any())).thenReturn(prisoner("A1234BC", prisonId = "MDI"))
      val newNumberOfChildrenCount = PrisonerNumberOfChildren(
        prisonerNumberOfChildrenId = 1,
        prisonerNumber = prisonerNumber,
        numberOfChildren = "1",
        active = true,
        createdBy = "USER1",
        createdTime = LocalDateTime.now(),
      )

      whenever(prisonerNumberOfChildrenRepository.saveAndFlush(any()))
        .thenReturn(newNumberOfChildrenCount)

      // When
      val result = prisonerNumberOfChildrenService.createOrUpdateNumberOfChildren(
        prisonerNumber,
        request,
        user,
      )

      // Then
      with(result) {
        assertThat(prisonerNumber).isEqualTo(prisonerNumber)
        assertThat(numberOfChildren).isEqualTo("1")
        assertThat(active).isTrue
      }
      verify(prisonerNumberOfChildrenRepository, times(1)).saveAndFlush(any())
    }

    @Test
    fun `should create new number of children with null value`() {
      // Given
      val request = CreateOrUpdatePrisonerNumberOfChildrenRequest(
        numberOfChildren = null,
      )
      whenever(prisonerService.getPrisoner(any())).thenReturn(prisoner("A1234BC", prisonId = "MDI"))

      val newNumberOfChildren = PrisonerNumberOfChildren(
        prisonerNumberOfChildrenId = 1,
        prisonerNumber = prisonerNumber,
        numberOfChildren = null,
        active = true,
        createdBy = "USER1",
        createdTime = LocalDateTime.now(),
      )

      whenever(prisonerNumberOfChildrenRepository.saveAndFlush(any()))
        .thenReturn(newNumberOfChildren)

      // When
      val result = prisonerNumberOfChildrenService.createOrUpdateNumberOfChildren(
        prisonerNumber,
        request,
        user,
      )

      // Then
      with(result) {
        assertThat(prisonerNumber).isEqualTo(prisonerNumber)
        assertThat(numberOfChildren).isEqualTo(null)
        assertThat(active).isTrue
      }
      verify(prisonerNumberOfChildrenRepository, times(1)).saveAndFlush(any())
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

      whenever(prisonerNumberOfChildrenRepository.saveAndFlush(any()))
        .thenReturn(existingNumberOfChildrenCount.copy(numberOfChildren = "2", active = true))
      whenever(prisonerService.getPrisoner(any())).thenReturn(prisoner("A1234BC", prisonId = "MDI"))

      // When
      val result = prisonerNumberOfChildrenService.createOrUpdateNumberOfChildren(
        prisonerNumber,
        request,
        user,
      )

      verify(prisonerNumberOfChildrenRepository, times(1)).deactivateExistingActiveRecord(prisonerNumber)

      verify(prisonerNumberOfChildrenRepository, times(1)).saveAndFlush(
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

    @Test
    fun `should throws exception when prisoner doesn't exist`() {
      // Given
      val request = CreateOrUpdatePrisonerNumberOfChildrenRequest(
        numberOfChildren = 1,
      )
      whenever(prisonerService.getPrisoner(any())).thenReturn(null)

      // When/Then
      assertThrows<EntityNotFoundException> {
        prisonerNumberOfChildrenService.createOrUpdateNumberOfChildren(
          prisonerNumber,
          request,
          user,
        )
      }.message isEqualTo "Prisoner number $prisonerNumber - not found"
    }
  }
}
