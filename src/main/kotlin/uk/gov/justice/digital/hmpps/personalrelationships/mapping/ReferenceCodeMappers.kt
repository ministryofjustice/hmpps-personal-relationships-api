package uk.gov.justice.digital.hmpps.personalrelationships.mapping

import uk.gov.justice.digital.hmpps.personalrelationships.entity.ReferenceCodeEntity
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.ReferenceCode

fun ReferenceCodeEntity.toModel() = ReferenceCode(
  referenceCodeId = referenceCodeId,
  groupCode = groupCode,
  code = code,
  description = description,
  displayOrder = displayOrder,
  isActive = isActive,
)

fun List<ReferenceCodeEntity>.toModel() = map { it.toModel() }
