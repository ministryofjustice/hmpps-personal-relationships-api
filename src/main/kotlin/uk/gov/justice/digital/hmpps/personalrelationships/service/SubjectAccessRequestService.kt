package uk.gov.justice.digital.hmpps.personalrelationships.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.SarPrisonerRestriction
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.SubjectAccessRequestData
import uk.gov.justice.digital.hmpps.personalrelationships.repository.PrisonerRestrictionsRepository
import uk.gov.justice.hmpps.kotlin.sar.HmppsPrisonSubjectAccessRequestService
import uk.gov.justice.hmpps.kotlin.sar.HmppsSubjectAccessRequestContent
import java.time.LocalDate

@Service
class SubjectAccessRequestService(private val prisonerRestrictionsRepository: PrisonerRestrictionsRepository) : HmppsPrisonSubjectAccessRequestService {
  override fun getPrisonContentFor(prn: String, fromDate: LocalDate?, toDate: LocalDate?): HmppsSubjectAccessRequestContent? {
    val from = fromDate ?: LocalDate.EPOCH
    val to = toDate ?: LocalDate.now()

    // Soft deleted restrictions are excluded by the entity's @SQLRestriction
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
