package uk.gov.justice.digital.hmpps.personalrelationships.model.response.migrate

import io.swagger.v3.oas.annotations.media.Schema

/**
 * Class to group together a type and the NOMIS / DPS IDs for it.
 */
data class IdPair(
  @Schema(description = "The category of information returned", example = "PHONE")
  val elementType: ElementType,

  @Schema(description = "The unique ID for this piece of data provided in the request", example = "123435")
  val nomisId: Long,

  @Schema(description = "The unique ID created in the DPS contacts service", example = "1234")
  val dpsId: Long,
)
