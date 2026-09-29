package uk.gov.justice.digital.hmpps.personalrelationships.service

import jakarta.persistence.EntityNotFoundException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.personalrelationships.helpers.aUser
import uk.gov.justice.digital.hmpps.personalrelationships.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.CreateOrUpdatePrisonerDomesticStatusRequest

@ExtendWith(MockitoExtension::class)
class PrisonerDomesticStatusServiceTest {

  @Mock
  private lateinit var prisonerService: PrisonerService

  @Mock
  private lateinit var transactionalPrisonerDomesticStatusService: TransactionalPrisonerDomesticStatusService

  @InjectMocks
  private lateinit var prisonerDomesticStatusService: PrisonerDomesticStatusService

  private val prisonerNumber = "A1234BC"

  private val user = aUser("test-user")

  @Test
  fun `should throws exception when prisoner doesn't exist`() {
    // Given
    val request = CreateOrUpdatePrisonerDomesticStatusRequest(
      domesticStatusCode = "M",
    )
    whenever(prisonerService.getPrisoner(any())).thenReturn(null)

    // When/Then
    assertThrows<EntityNotFoundException> {
      prisonerDomesticStatusService.createOrUpdateDomesticStatus(
        prisonerNumber,
        request,
        user,
      )
    }.message isEqualTo "Prisoner number $prisonerNumber - not found"
  }
}
