package uk.gov.justice.digital.hmpps.personalrelationships.model.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Restrictions related to specific contacts")
data class ContactsRestrictionsResponse(
  @Schema(description = "The list of contact global restrictions for each contact where found")
  val contactRestrictions: List<ContactRestrictions>,
)

data class ContactRestrictions(
  @Schema(description = "The unique identifier for the contact", example = "123456")
  val contactId: Long,

  @Schema(description = "Global (estate-wide) restrictions for the contact")
  val globalContactRestrictions: List<ContactRestrictionDetails>,
)
