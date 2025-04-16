package uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid

data class AddContactRelationshipRequest(
  @Schema(description = "The id of the contact this relationship is for", example = "123456")
  val contactId: Long,

  @Schema(description = "A description of the contacts relationship to a prisoner", exampleClasses = [ContactRelationship::class])
  @field:Valid
  val relationship: ContactRelationship,
)
