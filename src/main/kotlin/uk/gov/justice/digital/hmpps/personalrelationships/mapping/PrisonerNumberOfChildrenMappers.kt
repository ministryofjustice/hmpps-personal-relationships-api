package uk.gov.justice.digital.hmpps.personalrelationships.mapping

import uk.gov.justice.digital.hmpps.personalrelationships.entity.PrisonerNumberOfChildren
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.PrisonerNumberOfChildrenResponse

fun PrisonerNumberOfChildren.toModel(): PrisonerNumberOfChildrenResponse = PrisonerNumberOfChildrenResponse(
  id = prisonerNumberOfChildrenId,
  numberOfChildren = numberOfChildren,
  active = active,
  createdTime = createdTime,
  createdBy = createdBy,
)
