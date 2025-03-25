package uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "The details of a single relationship between a prisoner and a contact")
data class LinkedPrisonerDetails(
  @Schema(description = "Prisoner number (NOMS ID)", example = "A1234BC")
  val prisonerNumber: String,

  @Schema(description = "The unique identifier for the prisoner contact relationship", example = "123456")
  val prisonerContactId: Long,

  @Schema(description = "The last name of the prisoner. Nullable in the case the prisoners details are unavailable.", example = "Doe", nullable = true)
  val lastName: String?,

  @Schema(description = "The first name of the prisoner. Nullable in the case the prisoners details are unavailable.", example = "John", nullable = true)
  val firstName: String?,

  @Schema(description = "The middle names of the prisoner, if any", example = "William", nullable = true)
  val middleNames: String? = null,

  @Schema(description = "The id of the prisoners current prison", example = "BXI", nullable = true)
  val prisonId: String? = null,

  @Schema(description = "The name of the prisoners current prison", example = "Brixton (HMP)", nullable = true)
  val prisonName: String? = null,

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
