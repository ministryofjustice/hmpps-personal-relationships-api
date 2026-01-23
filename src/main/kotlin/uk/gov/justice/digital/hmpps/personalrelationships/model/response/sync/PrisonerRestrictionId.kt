import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Response object for prisoner restriction reconciliation")
data class PrisonerRestrictionId(
  @Schema(description = "The ID for prisoner restriction", example = "111111")
  val prisonerRestrictionId: Long,
)
