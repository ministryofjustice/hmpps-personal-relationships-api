package uk.gov.justice.digital.hmpps.personalrelationships.model.internal

import uk.gov.justice.digital.hmpps.personalrelationships.model.response.EmploymentDetails

data class PatchEmploymentResult(
  val createdIds: List<Long>,
  val updatedIds: List<Long>,
  val deletedIds: List<Long>,
  val employmentsAfterUpdate: List<EmploymentDetails>,
)
