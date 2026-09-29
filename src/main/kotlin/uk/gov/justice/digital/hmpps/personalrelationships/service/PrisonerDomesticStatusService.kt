package uk.gov.justice.digital.hmpps.personalrelationships.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.personalrelationships.config.User
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.CreateOrUpdatePrisonerDomesticStatusRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.PrisonerDomesticStatusResponse

@Service
class PrisonerDomesticStatusService(
  private val prisonerService: PrisonerService,
  private val transactionalPrisonerDomesticStatusService: TransactionalPrisonerDomesticStatusService,
) {

  fun getDomesticStatus(prisonerNumber: String): PrisonerDomesticStatusResponse = transactionalPrisonerDomesticStatusService.getDomesticStatus(prisonerNumber)

  /**
   * Creates a new domestic status record for a prisoner or updates an existing one.
   * If a record exists:
   * - The existing record is moved to history by setting it as inactive
   * - A new active record is created
   * If no record exists:
   * - A new active record is created
   */
  fun createOrUpdateDomesticStatus(
    prisonerNumber: String,
    request: CreateOrUpdatePrisonerDomesticStatusRequest,
    user: User,
  ): PrisonerDomesticStatusResponse {
    val prisoner = prisonerService.getPrisoner(prisonerNumber) ?: throw EntityNotFoundException("Prisoner number $prisonerNumber - not found")
    return transactionalPrisonerDomesticStatusService.createOrUpdateDomesticStatus(prisoner, request, user)
  }
}
