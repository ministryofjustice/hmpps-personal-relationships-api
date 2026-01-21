package uk.gov.justice.digital.hmpps.personalrelationships.model.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "The result of creating a contact and optionally a new relationship to a prisoner")
data class ContactCreationResult(
  @Schema(description = "The details of a contact as an individual")
  val createdContact: ContactDetails,
  @Schema(description = "Describes the prisoner contact relationship if one was created with the contact", nullable = true)
  val createdRelationship: PrisonerContactRelationshipDetails?,
)
