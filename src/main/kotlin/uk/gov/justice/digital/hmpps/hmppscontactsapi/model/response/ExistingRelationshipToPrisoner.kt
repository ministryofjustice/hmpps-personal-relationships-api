package uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response

import io.swagger.v3.oas.annotations.media.Schema

data class ExistingRelationshipToPrisoner(
  @Schema(description = "The unique identifier for the prisoner contact", example = "123456")
  val prisonerContactId: Long,

  @Schema(
    description =
    """
      Coded value indicating either a social or official contact (mandatory).
      This is a coded value from the group code CONTACT_TYPE in reference data.
      Known values are (S) Social or (O) official.
      """,
    example = "S",
  )
  val relationshipTypeCode: String,

  @Schema(description = "The description of the contact relationship type. Description from reference data Official or Social", example = "Official")
  val relationshipTypeDescription: String,

  @Schema(description = "The relationship to the prisoner. A code from SOCIAL_RELATIONSHIP or OFFICIAL_RELATIONSHIP reference data groups depending on the relationship type.", example = "FRI")
  val relationshipToPrisonerCode: String,

  @Schema(description = "The description of the relationship", example = "Friend", nullable = true)
  val relationshipToPrisonerDescription: String?,

  @Schema(description = "Is this prisoner's contact relationship active?", example = "true")
  val isRelationshipActive: Boolean,
)
