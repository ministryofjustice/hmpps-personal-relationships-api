package uk.gov.justice.digital.hmpps.personalrelationships.model.response

/**
 * Response class for merged restrictions operations.
 *
 * @property hasChanged Indicates if any restrictions has changed
 */
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Response class for merged restrictions operations.")
data class MergedRestrictionsResponse(
  @Schema(description = "Indicates if any restrictions have changed", example = "true")
  val hasChanged: Boolean,
  @Schema(description = "List of IDs for created restrictions", example = "[101, 102]")
  val createdRestrictions: List<Long> = emptyList(),
  @Schema(description = "List of IDs for deleted restrictions", example = "[201, 202]")
  val deletedRestrictions: List<Long> = emptyList(),
)
