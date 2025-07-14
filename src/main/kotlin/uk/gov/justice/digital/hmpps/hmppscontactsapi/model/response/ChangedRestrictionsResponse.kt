package uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response

import io.swagger.v3.oas.annotations.media.Schema

/**
 * Response class for changed restrictions operations.
 *
 * @property hasChanged Indicates if any restrictions has changed
 */
@Schema(description = "Response class for changed restrictions operations.")
data class ChangedRestrictionsResponse(
  @Schema(description = "Indicates if any restrictions have changed", example = "true")
  val hasChanged: Boolean,
  @Schema(description = "List of IDs for created restrictions", example = "[101, 102]")
  val createdRestrictions: List<Long>,
  @Schema(description = "List of IDs for deleted restrictions", example = "[201, 202]")
  val deletedRestrictions: List<Long>,
)
