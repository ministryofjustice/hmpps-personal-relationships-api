package uk.gov.justice.digital.hmpps.hmppscontactsapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.PrisonerRestriction
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.PrisonerRestrictionsRepository
import java.time.LocalDate
import java.time.LocalDateTime

class PrisonerRestrictionsServiceTest {

  private var prisonerRestrictionsRepository: PrisonerRestrictionsRepository = mock()
  private var service: PrisonerRestrictionsService = PrisonerRestrictionsService(prisonerRestrictionsRepository)

  @Test
  fun `getPrisonerRestrictions returns all restrictions paged when currentTermOnly is false`() {
    val prisonerNumber = "A1234BC"
    val restrictions = listOf(
      createPrisonerRestrictionEntity().copy(prisonerRestrictionId = 1L, currentTerm = true),
      createPrisonerRestrictionEntity().copy(prisonerRestrictionId = 2L, currentTerm = false),
      createPrisonerRestrictionEntity().copy(prisonerRestrictionId = 3L, currentTerm = true),
    )
    whenever(prisonerRestrictionsRepository.findByPrisonerNumber(prisonerNumber)).thenReturn(restrictions)

    val pageable = PageRequest.of(0, 2)
    val result = service.getPrisonerRestrictions(prisonerNumber, currentTermOnly = false, pageable)

    assertThat(result.content).hasSize(2)
    assertThat(result.metadata.totalElements).isEqualTo(3)
    assertThat(result.content.map { it.prisonerRestrictionId }).containsExactly(1L, 2L)
  }

  @Test
  fun `getPrisonerRestrictions returns only current term restrictions when currentTermOnly is true`() {
    val prisonerNumber = "A1234BC"
    val restrictions = listOf(
      createPrisonerRestrictionEntity().copy(prisonerRestrictionId = 1L, currentTerm = true),
      createPrisonerRestrictionEntity().copy(prisonerRestrictionId = 2L, currentTerm = false),
      createPrisonerRestrictionEntity().copy(prisonerRestrictionId = 3L, currentTerm = true),
    )
    whenever(prisonerRestrictionsRepository.findByPrisonerNumber(prisonerNumber)).thenReturn(restrictions)

    val pageable = PageRequest.of(0, 10)
    val result = service.getPrisonerRestrictions(prisonerNumber, currentTermOnly = true, pageable)

    assertThat(result.content).hasSize(2)
    assertThat(result.metadata.totalElements).isEqualTo(2)
    assertThat(result.content.map { it.prisonerRestrictionId }).containsExactly(1L, 3L)
  }

  @Test
  fun `getPrisonerRestrictions returns empty when no restrictions exist`() {
    val prisonerNumber = "A1234BC"
    whenever(prisonerRestrictionsRepository.findByPrisonerNumber(prisonerNumber)).thenReturn(emptyList())

    val pageable = PageRequest.of(0, 10)
    val result = service.getPrisonerRestrictions(prisonerNumber, currentTermOnly = false, pageable)

    assertThat(result.content).isEmpty()
    assertThat(result.metadata.totalElements).isEqualTo(0)
  }

  private fun createPrisonerRestrictionEntity() = PrisonerRestriction(
    prisonerRestrictionId = 1L,
    prisonerNumber = "A1234BC",
    restrictionType = "CCTV",
    effectiveDate = LocalDate.of(2024, 6, 11),
    expiryDate = LocalDate.of(2024, 12, 31),
    commentText = "No visits allowed",
    authorisedUsername = "JSMITH",
    createdBy = "JSMITH_ADM",
    createdTime = LocalDateTime.of(2024, 6, 11, 10, 0),
    currentTerm = true,
    updatedBy = null,
    updatedTime = null,
  )
}
