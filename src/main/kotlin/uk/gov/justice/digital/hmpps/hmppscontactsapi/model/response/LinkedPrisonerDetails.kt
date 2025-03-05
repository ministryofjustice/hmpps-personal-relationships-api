package uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "The details of a prisoner linked to a contact including one or more relationships")
data class LinkedPrisonerDetails(
  @Schema(description = "Prisoner number (NOMS ID)", example = "A1234BC")
  val prisonerNumber: String,

  @Schema(description = "The last name of the prisoner", example = "Doe")
  val lastName: String,

  @Schema(description = "The first name of the prisoner", example = "John")
  val firstName: String,

  @Schema(description = "The middle names of the prisoner, if any", example = "William", nullable = true)
  val middleNames: String? = null,

  @Schema(description = "The id of the prisoners current prison", example = "BXI", nullable = true)
  val prisonId: String? = null,

  @Schema(description = "The name of the prisoners current prison", example = "Brixton (HMP)", nullable = true)
  val prisonName: String? = null,

  @Schema(description = "All the relationships between the prisoner and contact. At least one will be present.")
  val relationships: List<LinkedPrisonerRelationshipDetails>,
)
