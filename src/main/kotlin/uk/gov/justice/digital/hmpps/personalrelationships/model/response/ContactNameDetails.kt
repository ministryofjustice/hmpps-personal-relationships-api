package uk.gov.justice.digital.hmpps.personalrelationships.model.response

import io.swagger.v3.oas.annotations.media.Schema

data class ContactNameDetails(
  @Schema(
    description =
    """
      The title code for the contact.
      This is a coded value (from the group code TITLE in reference data).
      Known values are MR, MRS, MISS, DR, MS, REV, SIR, BR, SR.
      """,
    example = "MR",
    nullable = true,
  )
  val titleCode: String? = null,

  @Schema(
    description = "The description of the title code, if present",
    example = "Mr",
    nullable = true,
  )
  val titleDescription: String? = null,

  @Schema(description = "The last name of the contact", example = "Doe")
  val lastName: String,

  @Schema(description = "The first name of the contact", example = "John")
  val firstName: String,

  @Schema(description = "The middle name of the contact, if any", example = "William", nullable = true)
  val middleNames: String? = null,
)
