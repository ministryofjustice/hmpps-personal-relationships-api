package uk.gov.justice.digital.hmpps.hmppscontactsapi.service.migrate

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.PrisonerRestriction
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.ReferenceCodeGroup
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.migrate.MigratePrisonerRestrictionsRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.migrate.PrisonerRestrictionDetailsRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.migrate.PrisonerRestrictionsMigrationResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.PrisonerRestrictionsRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.ReferenceCodeRepository
import java.time.LocalDate
import java.time.LocalDateTime

class PrisonerRestrictionsMigrationServiceTest {
  private val prisonerRestrictionsRepository: PrisonerRestrictionsRepository = mock()
  private val referenceCodeRepository: ReferenceCodeRepository = mock()
  private val migrationService = PrisonerRestrictionsMigrationService(
    prisonerRestrictionsRepository,
    referenceCodeRepository,
  )

  private val prisonerNumber = "A1234BC"
  private val now = LocalDateTime.of(2024, 6, 1, 12, 0)

  @Test
  fun `should delete existing and save new prisoner restrictions`() {
    val request = MigratePrisonerRestrictionsRequest(
      prisonerNumber = prisonerNumber,
      restrictions = listOf(
        PrisonerRestrictionDetailsRequest(
          restrictionType = "NO_VISIT",
          effectiveDate = LocalDate.of(2024, 1, 1),
          expiryDate = LocalDate.of(2024, 12, 31),
          commentText = "No visits allowed",
          authorisedUsername = "JSMITH",
          currentTerm = true,
          createdBy = "user1",
          createdTime = now,
          updatedBy = "user2",
          updatedTime = now.plusDays(1),
        ),
      ),
    )

    whenever(referenceCodeRepository.findByGroupCodeAndCode(ReferenceCodeGroup.RESTRICTION, "NO_VISIT")).thenReturn(mock())
    val savedEntity = PrisonerRestriction(
      prisonerRestrictionId = 99L,
      prisonerNumber = prisonerNumber,
      restrictionType = "NO_VISIT",
      effectiveDate = LocalDate.of(2024, 1, 1),
      expiryDate = LocalDate.of(2024, 12, 31),
      commentText = "No visits allowed",
      authorisedUsername = "JSMITH",
      currentTerm = true,
      createdBy = "user1",
      createdTime = now,
      updatedBy = "user2",
      updatedTime = now.plusDays(1),
    )
    whenever(prisonerRestrictionsRepository.saveAll(any<List<PrisonerRestriction>>())).thenReturn(listOf(savedEntity))

    val captor = argumentCaptor<List<PrisonerRestriction>>()

    val response = migrationService.migratePrisonerRestrictions(request)

    verify(prisonerRestrictionsRepository).deleteByPrisonerNumber(prisonerNumber)
    verify(prisonerRestrictionsRepository).saveAll(captor.capture())
    assertThat(captor.firstValue).hasSize(1)
    assertThat(response).isEqualTo(
      PrisonerRestrictionsMigrationResponse(
        prisonerNumber = prisonerNumber,
        prisonerRestrictionsIds = listOf(99L),
      ),
    )
  }

  @Test
  fun `should throw if restriction type does not exist in reference data`() {
    val request = MigratePrisonerRestrictionsRequest(
      prisonerNumber = prisonerNumber,
      restrictions = listOf(
        PrisonerRestrictionDetailsRequest(
          restrictionType = "INVALID_TYPE",
          effectiveDate = LocalDate.of(2024, 1, 1),
          expiryDate = LocalDate.of(2024, 12, 31),
          commentText = "Invalid restriction",
          authorisedUsername = "JSMITH",
          currentTerm = true,
          createdBy = "user1",
          createdTime = now,
          updatedBy = "user2",
          updatedTime = now.plusDays(1),
        ),
      ),
    )

    whenever(referenceCodeRepository.findByGroupCodeAndCode(ReferenceCodeGroup.RESTRICTION, "INVALID_TYPE")).thenReturn(null)

    assertThatThrownBy { migrationService.migratePrisonerRestrictions(request) }
      .isInstanceOf(EntityNotFoundException::class.java)
      .hasMessageContaining("No reference data found for groupCode: ReferenceCodeGroup.RESTRICTION and code: INVALID_TYPE")
  }
}
