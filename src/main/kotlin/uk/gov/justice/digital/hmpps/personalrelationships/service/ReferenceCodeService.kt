package uk.gov.justice.digital.hmpps.personalrelationships.service

import jakarta.validation.ValidationException
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.personalrelationships.mapping.toModel
import uk.gov.justice.digital.hmpps.personalrelationships.model.ReferenceCodeGroup
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.ReferenceCode
import uk.gov.justice.digital.hmpps.personalrelationships.repository.ReferenceCodeRepository

@Service
class ReferenceCodeService(private val referenceCodeRepository: ReferenceCodeRepository) {
  fun getReferenceDataByGroup(groupCode: ReferenceCodeGroup, sort: Sort, activeOnly: Boolean): List<ReferenceCode> = if (activeOnly) {
    referenceCodeRepository.findAllByGroupCodeAndIsActiveEquals(groupCode, true, sort).toModel()
  } else {
    referenceCodeRepository.findAllByGroupCodeEquals(groupCode, sort).toModel()
  }

  fun getReferenceDataByGroupAndCode(groupCode: ReferenceCodeGroup, code: String): ReferenceCode? = referenceCodeRepository.findByGroupCodeAndCode(groupCode, code)?.toModel()

  /**
   * Validates a reference code for the supplied group and returns the details if it is. You should allow inactive reference codes
   * when updating an existing entity so we do not block important updates on existing or imported data.
   */
  fun validateReferenceCode(groupCode: ReferenceCodeGroup, code: String, allowInactive: Boolean): ReferenceCode {
    val referenceCode = getReferenceDataByGroupAndCode(groupCode, code) ?: throw ValidationException("Unsupported ${groupCode.displayName} ($code)")
    if (!allowInactive && !referenceCode.isActive) {
      throw ValidationException("Unsupported ${groupCode.displayName} ($code). This code is no longer active.")
    }
    return referenceCode
  }
}
