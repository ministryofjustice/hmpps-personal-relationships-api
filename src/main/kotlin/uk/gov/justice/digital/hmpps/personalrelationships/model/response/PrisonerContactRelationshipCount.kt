package uk.gov.justice.digital.hmpps.personalrelationships.model.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "A count of a prisoners contact relationships")
data class PrisonerContactRelationshipCount(
  @Schema(description = "The number of active social relationships")
  val social: Long,
  @Schema(description = "The number of active official relationships")
  val official: Long,
)
