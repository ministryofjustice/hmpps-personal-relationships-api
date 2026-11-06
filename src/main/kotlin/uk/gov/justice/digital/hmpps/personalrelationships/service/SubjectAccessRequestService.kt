package uk.gov.justice.digital.hmpps.personalrelationships.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.SarPrisonerRestriction
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.SubjectAccessRequestData
import uk.gov.justice.digital.hmpps.personalrelationships.repository.PrisonerRestrictionsRepository
import uk.gov.justice.hmpps.kotlin.sar.HmppsPrisonSubjectAccessRequestService
import uk.gov.justice.hmpps.kotlin.sar.HmppsSubjectAccessRequestContent
import java.time.LocalDate

/**
 * Interim subject access request (SAR) support for prisoner restrictions, written so that restrictions are not
 * missed from a SAR while they are held in DPS (VB-7124).
 *
 * Prisoner restrictions are expected to be merged into Prisoner Alerts (epic VB-3971). Once that happens the data
 * will be covered by the hmpps-alerts-api SAR endpoint when it is migrated, and this service, its models and tests
 * should be removed rather than extended.
 */
@Service
class SubjectAccessRequestService(private val prisonerRestrictionsRepository: PrisonerRestrictionsRepository) : HmppsPrisonSubjectAccessRequestService {
  override fun getPrisonContentFor(prn: String, fromDate: LocalDate?, toDate: LocalDate?): HmppsSubjectAccessRequestContent? {
    val from = fromDate ?: LocalDate.EPOCH
    val to = toDate ?: LocalDate.now()

    val restrictions = prisonerRestrictionsRepository.findByPrisonerNumber(prn)
      .filter { it.createdTime.toLocalDate() in from..to }
      .sortedBy { it.prisonerRestrictionId }
      .map(::SarPrisonerRestriction)

    return if (restrictions.isEmpty()) {
      null
    } else {
      HmppsSubjectAccessRequestContent(SubjectAccessRequestData(prn, from, to, restrictions))
    }
  }
}
