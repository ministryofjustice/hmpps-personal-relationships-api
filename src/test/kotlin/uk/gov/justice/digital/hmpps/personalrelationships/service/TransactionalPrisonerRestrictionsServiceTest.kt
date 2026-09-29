package uk.gov.justice.digital.hmpps.personalrelationships.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.personalrelationships.entity.PrisonerRestrictionDetailsEntity
import uk.gov.justice.digital.hmpps.personalrelationships.repository.PrisonerRestrictionDetailsRepository
import java.time.LocalDate
import java.time.LocalDateTime

class TransactionalPrisonerRestrictionsServiceTest {

  private val prisonerRestrictionDetailsRepository: PrisonerRestrictionDetailsRepository = mock()
  private val service = TransactionalPrisonerRestrictionsService(prisonerRestrictionDetailsRepository)

  @Test
  fun `getPrisonerRestrictions returns restrictions for prisoner`() {
    val prisonerNumber = "A1234BC"
    val restrictions = listOf(createPrisonerRestrictionEntity())
    whenever(prisonerRestrictionDetailsRepository.findByPrisonerNumber(prisonerNumber)).thenReturn(restrictions)

    val result = service.getPrisonerRestrictions(prisonerNumber)

    assertThat(result).isEqualTo(restrictions)
    verify(prisonerRestrictionDetailsRepository).findByPrisonerNumber(prisonerNumber)
  }

  private fun createPrisonerRestrictionEntity() = PrisonerRestrictionDetailsEntity(
    prisonerRestrictionId = 1L,
    prisonerNumber = "A1234BC",
    restrictionType = "CCTV",
    restrictionTypeDescription = "No Visits",
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
