package uk.gov.justice.digital.hmpps.personalrelationships.facade

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.web.PagedModel
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.PrisonerRestrictionDetails
import uk.gov.justice.digital.hmpps.personalrelationships.service.PrisonerRestrictionsService
import java.time.LocalDate
import java.time.LocalDateTime

class PrisonerRestrictionsFacadeTest {
  private val prisonerRestrictionsService: PrisonerRestrictionsService = mock()
  private val facade = PrisonerRestrictionsFacade(prisonerRestrictionsService)

  @Test
  fun `getPrisonerRestrictions delegates to service and returns result`() {
    val prisonerNumber = "A1234BC"
    val currentTermOnly = true
    val pageable = PageRequest.of(0, 10)
    val paged = true
    val prisonerRestrictionDetails = PrisonerRestrictionDetails(
      prisonerRestrictionId = 1L,
      prisonerNumber = prisonerNumber,
      restrictionType = "NO_VISIT",
      restrictionTypeDescription = "No Visits",
      authorisedByDisplayName = "JAMES SMITH",
      effectiveDate = LocalDate.now(),
      expiryDate = null,
      commentText = "No visits allowed",
      authorisedUsername = "JSMITH",
      currentTerm = true,
      createdBy = "JSMITH_ADM",
      createdTime = LocalDateTime.now(),
      updatedBy = null,
      updatedTime = null,
    )
    val expected = PagedModel(PageImpl(listOf(prisonerRestrictionDetails)))

    whenever(prisonerRestrictionsService.getPrisonerRestrictions(prisonerNumber, currentTermOnly, pageable, paged)).thenReturn(expected)

    val result = facade.getPrisonerRestrictions(prisonerNumber, currentTermOnly, pageable, paged)

    assertThat(result).isEqualTo(expected)
    verify(prisonerRestrictionsService).getPrisonerRestrictions(prisonerNumber, currentTermOnly, pageable, paged)
  }
}
