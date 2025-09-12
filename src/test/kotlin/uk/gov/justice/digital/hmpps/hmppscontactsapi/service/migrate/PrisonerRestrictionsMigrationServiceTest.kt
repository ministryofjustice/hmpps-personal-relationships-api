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
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.migrate.MigratePrisonerRestrictionRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.migrate.MigratePrisonerRestrictionsRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.migrate.PrisonerRestrictionDetailsRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.migrate.PrisonerRestrictionMigrationResponse
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
      startDate = LocalDate.of(2024, 1, 1),
      expiryDate = LocalDate.of(2024, 12, 31),
      comments = "No visits allowed",
      authorisedUsername = "JSMITH",
      currentTerm = true,
      createdBy = "user1",
      createdTime = now,
      updatedBy = "user2",
      updatedTime = now.plusDays(1),
    )
    whenever(prisonerRestrictionsRepository.saveAllAndFlush(any<List<PrisonerRestriction>>())).thenReturn(listOf(savedEntity))

    val captor = argumentCaptor<List<PrisonerRestriction>>()

    val response = migrationService.migratePrisonerRestrictions(request)

    verify(prisonerRestrictionsRepository).deleteByPrisonerNumber(prisonerNumber)
    verify(prisonerRestrictionsRepository).saveAllAndFlush(captor.capture())
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

  @Test
  fun `should migrate a single prisoner restriction`() {
    val request = migratePrisonerRestrictionRequest("CCTV")

    whenever(referenceCodeRepository.findByGroupCodeAndCode(ReferenceCodeGroup.RESTRICTION, "CCTV")).thenReturn(mock())
    val savedEntity = prisonerRestriction()
    whenever(prisonerRestrictionsRepository.saveAndFlush(any<PrisonerRestriction>())).thenReturn(savedEntity)

    val response = migrationService.migratePrisonerRestriction(prisonerNumber, request)

    val captor = argumentCaptor<PrisonerRestriction>()
    verify(prisonerRestrictionsRepository).saveAndFlush(captor.capture())
    assertThat(captor.firstValue).usingRecursiveComparison().ignoringFields("prisonerRestrictionId").isEqualTo(savedEntity)
    assertThat(response).isEqualTo(
      PrisonerRestrictionMigrationResponse(
        prisonerRestrictionId = 123L,
        prisonerNumber = prisonerNumber,
      ),
    )
  }

  @Test
  fun `should throw if restriction type does not exist in reference data for single restriction`() {
    val request = migratePrisonerRestrictionRequest("INVALID_TYPE")

    whenever(referenceCodeRepository.findByGroupCodeAndCode(ReferenceCodeGroup.RESTRICTION, "INVALID_TYPE")).thenReturn(null)

    assertThatThrownBy { migrationService.migratePrisonerRestriction("A21KR21", request) }
      .isInstanceOf(EntityNotFoundException::class.java)
      .hasMessageContaining("No reference data found for groupCode: ReferenceCodeGroup.RESTRICTION and code: INVALID_TYPE")
  }

  private fun prisonerRestriction(restrictionType: String = "CCTV") = PrisonerRestriction(
    prisonerRestrictionId = 123L,
    prisonerNumber = prisonerNumber,
    restrictionType,
    startDate = now.toLocalDate(),
    expiryDate = now.toLocalDate().plusDays(10),
    comments = "CCTV",
    authorisedUsername = "JSMITH",
    currentTerm = true,
    createdBy = "user1",
    createdTime = now,
    updatedBy = "user2",
    updatedTime = now.plusDays(1),
  )

  private fun migratePrisonerRestrictionRequest(restrictionType: String = "CCTV") = MigratePrisonerRestrictionRequest(
    restrictionType,
    effectiveDate = now.toLocalDate(),
    expiryDate = now.toLocalDate().plusDays(10),
    commentText = "CCTV",
    authorisedUsername = "JSMITH",
    currentTerm = true,
    createdBy = "user1",
    createdTime = now,
    updatedBy = "user2",
    updatedTime = now.plusDays(1),
  )
}
