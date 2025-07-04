package uk.gov.justice.digital.hmpps.hmppscontactsapi.service.sync

import jakarta.validation.ValidationException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.PrisonerRestriction
import uk.gov.justice.digital.hmpps.hmppscontactsapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.ReferenceCodeGroup
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.migrate.PrisonerRestrictionDetailsRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.ResetPrisonerRestrictionsRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.ReferenceCode
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.PrisonerRestrictionsRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.ReferenceCodeService
import java.time.LocalDate
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class PrisonerRestrictionsAdminServiceTest {
  @Mock
  private lateinit var prisonerRestrictionsRepository: PrisonerRestrictionsRepository

  @Mock
  private lateinit var referenceCodeService: ReferenceCodeService

  @InjectMocks
  private lateinit var restrictionsAdminService: PrisonerRestrictionsAdminService

  private val retainingPrisonerNumber = "A1234BC"
  private val removingPrisonerNumber = "B2345CD"

  @Nested
  inner class PrisonerMergeRestrictions {

    @Test
    fun `should move restrictions from removing to retaining prisoner and delete old restrictions`() {
      // Given
      val prisonerRestrictionId0 = 0L
      val prisonerRestrictionId1 = 1L
      val databaseNextIndex = 5L
      val removingRestriction1 = restriction(prisonerRestrictionId0)
      val removingRestriction2 = restriction(prisonerRestrictionId1)
      val removingRestrictions = listOf(removingRestriction1, removingRestriction2)

      whenever(prisonerRestrictionsRepository.findByPrisonerNumber(removingPrisonerNumber))
        .thenReturn(removingRestrictions)
      whenever(prisonerRestrictionsRepository.saveAllAndFlush(any<List<PrisonerRestriction>>()))
        .thenAnswer { invocation ->
          val restrictions = invocation.getArgument<List<PrisonerRestriction>>(0)
          // Simulate saving by assigning new IDs based on the next index
          restrictions.mapIndexed { idx, restriction -> restriction.copy(prisonerRestrictionId = (idx + databaseNextIndex)) }
        }
      // When
      val result = restrictionsAdminService.mergePrisonerRestrictions(retainingPrisonerNumber, removingPrisonerNumber)

      // Then
      verify(prisonerRestrictionsRepository).findByPrisonerNumber(removingPrisonerNumber)
      verify(prisonerRestrictionsRepository).deleteByPrisonerNumber(removingPrisonerNumber)
      verify(prisonerRestrictionsRepository).saveAllAndFlush(any<List<PrisonerRestriction>>())
      assertThat(result.hasChanged).isTrue
    }

    @Test
    fun `should return default response when no restrictions to move`() {
      // Given
      whenever(prisonerRestrictionsRepository.findByPrisonerNumber(removingPrisonerNumber))
        .thenReturn(emptyList())

      // When
      val result = restrictionsAdminService.mergePrisonerRestrictions(retainingPrisonerNumber, removingPrisonerNumber)

      // Then
      verify(prisonerRestrictionsRepository).findByPrisonerNumber(removingPrisonerNumber)
      verify(prisonerRestrictionsRepository, never()).saveAllAndFlush(any<List<PrisonerRestriction>>())
      verify(prisonerRestrictionsRepository, never()).deleteByPrisonerNumber(any())
      assertThat(result.hasChanged).isFalse
    }

    @Test
    fun `should handle exception thrown by saveAllAndFlush and return default response`() {
      val removingRestriction = restriction(1L)
      whenever(prisonerRestrictionsRepository.findByPrisonerNumber(removingPrisonerNumber))
        .thenReturn(listOf(removingRestriction))
      whenever(prisonerRestrictionsRepository.saveAllAndFlush(any<List<PrisonerRestriction>>()))
        .thenThrow(RuntimeException("DB error"))

      assertThrows<RuntimeException> {
        restrictionsAdminService.mergePrisonerRestrictions(retainingPrisonerNumber, removingPrisonerNumber)
      }.message isEqualTo "DB error"
    }
  }

  @Nested
  inner class ResetPrisonerRestrictions {
    private val prisonerNumber = "A1234BC"
    private val referenceCode = ReferenceCode(
      referenceCodeId = 0,
      ReferenceCodeGroup.RESTRICTION,
      "CCTV",
      "CCTV",
      99,
      true,
    )

    @Test
    fun `should delete all existing restrictions and create new ones for prisoner`() {
      val removingRestrictions = listOf(
        restriction(1L, prisonerNumber),
        restriction(2L, prisonerNumber),
      )
      whenever(prisonerRestrictionsRepository.findByPrisonerNumber(prisonerNumber)).thenReturn(removingRestrictions)
      whenever(referenceCodeService.validateReferenceCode(ReferenceCodeGroup.RESTRICTION, "CCTV", true))
        .thenReturn(referenceCode)
      val request = createRequest()
      restrictionsAdminService.resetPrisonerRestrictions(request)

      verify(prisonerRestrictionsRepository).findByPrisonerNumber(prisonerNumber)
      verify(prisonerRestrictionsRepository).deleteAll(removingRestrictions)

      val addingRestrictions: List<PrisonerRestriction> = mapPrisonerRestrictionsEntities(request)
      verify(prisonerRestrictionsRepository).saveAllAndFlush(addingRestrictions)
    }

    @Test
    fun `should not publish events when repository throws exception`() {
      val restrictions = listOf(restriction(1L, prisonerNumber))
      whenever(prisonerRestrictionsRepository.findByPrisonerNumber(prisonerNumber)).thenReturn(restrictions)
      whenever(referenceCodeService.validateReferenceCode(ReferenceCodeGroup.RESTRICTION, "CCTV", true))
        .thenReturn(referenceCode)
      whenever(prisonerRestrictionsRepository.deleteAll(any<List<PrisonerRestriction>>()))
        .thenThrow(RuntimeException("DB error"))

      assertThrows<RuntimeException> {
        restrictionsAdminService.resetPrisonerRestrictions(createRequest())
      }.message isEqualTo "DB error"

      verify(prisonerRestrictionsRepository).findByPrisonerNumber(prisonerNumber)
      verify(prisonerRestrictionsRepository).deleteAll(restrictions)
    }

    @Test
    fun `should throw exception when restrictionType is not found`() {
      val restrictions = listOf(restriction(1L, prisonerNumber))
      whenever(prisonerRestrictionsRepository.findByPrisonerNumber(prisonerNumber)).thenReturn(restrictions)
      whenever(referenceCodeService.validateReferenceCode(ReferenceCodeGroup.RESTRICTION, "CCTV", true))
        .thenThrow(ValidationException("Unsupported Restriction (CCTV)"))
      val request = ResetPrisonerRestrictionsRequest(
        prisonerNumber = prisonerNumber,
        restrictions = listOf(
          prisonerRestrictionDetailsRequest("CCTV"),
          prisonerRestrictionDetailsRequest("UNKNOWN"), // Empty restriction type
        ),
      )

      assertThrows<ValidationException> {
        restrictionsAdminService.resetPrisonerRestrictions(request)
      }.message isEqualTo "Unsupported Restriction (CCTV)"

      verify(prisonerRestrictionsRepository).findByPrisonerNumber(prisonerNumber)
      verify(prisonerRestrictionsRepository, never()).deleteAll(any())
      verify(prisonerRestrictionsRepository, never()).saveAllAndFlush(any<List<PrisonerRestriction>>())
    }

    private fun createRequest() = ResetPrisonerRestrictionsRequest(
      prisonerNumber = "A1234BC",
      restrictions = listOf(
        prisonerRestrictionDetailsRequest("CCTV"),
        prisonerRestrictionDetailsRequest("CCTV"),
      ),
    )

    private fun prisonerRestrictionDetailsRequest(restrictionType: String = "CCTV") = PrisonerRestrictionDetailsRequest(
      restrictionType,
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

    private fun mapPrisonerRestrictionsEntities(request: ResetPrisonerRestrictionsRequest) = request.restrictions.map { restriction ->
      PrisonerRestriction(
        prisonerRestrictionId = 0, // Let JPA generate new ID
        prisonerNumber = request.prisonerNumber,
        restrictionType = restriction.restrictionType,
        effectiveDate = restriction.effectiveDate,
        expiryDate = restriction.expiryDate,
        commentText = restriction.commentText,
        currentTerm = restriction.currentTerm,
        authorisedUsername = restriction.authorisedUsername,
        createdBy = restriction.createdBy,
        createdTime = restriction.createdTime,
        updatedBy = restriction.updatedBy,
        updatedTime = restriction.updatedTime,
      )
    }
  }

  private fun restriction(
    prisonerRestrictionId: Long = 1L,
    prisonerNumber: String = removingPrisonerNumber,
    restrictionType: String = "CCTV",
    effectiveDate: LocalDate = LocalDate.of(2024, 1, 1),
    expiryDate: LocalDate = LocalDate.of(2024, 12, 31),
    commentText: String = "No visits allowed",
    currentTerm: Boolean = true,
  ) = PrisonerRestriction(
    prisonerRestrictionId,
    prisonerNumber = prisonerNumber,
    restrictionType = restrictionType,
    effectiveDate = effectiveDate,
    expiryDate = expiryDate,
    commentText = commentText,
    authorisedUsername = "JSMITH",
    currentTerm = currentTerm,
    createdBy = "user1",
    createdTime = LocalDateTime.of(2024, 6, 1, 12, 0),
    updatedBy = "user2",
    updatedTime = LocalDateTime.of(2024, 6, 1, 12, 0).plusDays(1),
  )
}
