package uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Size

data class AddContactRelationshipRequest(
  @Schema(description = "The id of the contact this relationship is for", example = "123456")
  val contactId: Long,

  @Schema(description = "A description of the contacts relationship to a prisoner", exampleClasses = [ContactRelationship::class])
  @field:Valid
  val relationship: ContactRelationship,

  @Schema(description = "The id of the user creating the contact", example = "JD000001", maxLength = 100)
  @field:Size(max = 100, message = "createdBy must be <= 100 characters")
  val createdBy: String,
)
