package uk.gov.justice.digital.hmpps.personalrelationships.model.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Describes limited details of a group of prisoner and contact relationships")
data class PrisonerContactRelationshipsResponse(
  val responses: List<PrisonerContactRelationship>,
)

data class PrisonerContactRelationship(
  @Schema(description = "Prisoner number (NOMS ID)", example = "A1234BC")
  val prisonerNumber: String,

  @Schema(description = "The unique identifier for the contact", example = "654321")
  val contactId: Long,

  val relationships: List<SummaryRelationship>,
)

data class SummaryRelationship(
  @Schema(description = "The prisoner contact ID", example = "1234")
  val prisonerContactId: Long,

  @Schema(description = "Coded value indicating either a social (S) or official contact (O)", example = "S")
  val relationshipTypeCode: String,

  @Schema(description = "The relationship with the prisoner coded value", example = "FRI")
  val relationshipToPrisonerCode: String,

  @Schema(description = "Is this a approved visitor for the prisoner?", example = "true")
  val isApprovedVisitor: Boolean,

  @Schema(description = "Is this prisoner's contact relationship active?", example = "true")
  val isRelationshipActive: Boolean,

  @Schema(description = "Is this prisoner's contact relationship active?", example = "true")
  val currentTerm: Boolean,
)
