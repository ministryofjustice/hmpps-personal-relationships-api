package uk.gov.justice.digital.hmpps.personalrelationships.model.request.sync

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalDateTime

@Schema(description = "Request to update a new contact ")
data class SyncUpdateContactRequest(

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
  val title: String? = null,

  @Schema(description = "The last name of the contact", example = "Doe")
  val lastName: String,

  @Schema(description = "The first name of the contact", example = "John")
  val firstName: String,

  @Schema(description = "The middle name of the contact, if any", example = "William", nullable = true)
  val middleName: String? = null,

  @Schema(description = "The date of birth of the contact, if known", example = "1980-01-01", nullable = true)
  val dateOfBirth: LocalDate? = null,

  @Schema(description = "Whether the contact is a staff member", example = "false", nullable = false)
  var isStaff: Boolean = false,

  @Schema(description = "Whether the contact is a remitter", example = "false", nullable = false)
  var remitter: Boolean = false,

  @Schema(description = "Whether the contact is deceased", example = "false", nullable = true)
  var deceasedFlag: Boolean? = false,

  @Schema(description = "The date the contact was deceased, if applicable", example = "2023-05-01", nullable = true)
  var deceasedDate: LocalDate? = null,

  @Schema(
    description =
    """
    The gender code for the contact.
    This is a coded value (from the group code GENDER in reference data).
    Known values are (M) Male, (F) Female, (NK) Not Known, (NS) Not Specified.
    """,
    example = "M",
    nullable = true,
  )
  var gender: String? = null,

  @Schema(description = "The domestic status code of the contact", example = "S", nullable = true)
  var domesticStatus: String? = null,

  @Schema(description = "The language code of the contact", example = "EN", nullable = true)
  var languageCode: String? = null,

  @Schema(description = "Whether an interpreter is required", example = "false", nullable = true)
  var interpreterRequired: Boolean? = false,

  @Schema(description = "The id of the user who updated the contact", example = "JD000001")
  val updatedBy: String,

  @Schema(description = "The timestamp of when the contact was changed", example = "2024-01-01T00:00:00Z")
  val updatedTime: LocalDateTime,
)
