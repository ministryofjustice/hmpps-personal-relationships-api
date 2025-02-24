package uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalDateTime

@Schema(description = "The details of a updated contact as an individual")
data class PatchContactResponse(

  @Schema(description = "The id of the contact", example = "123456")
  val id: Long,

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
  val middleNames: String? = null,

  @Schema(description = "The date of birth of the contact, if known", example = "1980-01-01", nullable = true)
  val dateOfBirth: LocalDate? = null,

  @Schema(description = "Whether the contact is a staff member", example = "false", nullable = false)
  var isStaff: Boolean = false,

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

  @Schema(description = "User who created the entry", example = "admin")
  val createdBy: String,

  @Schema(description = "Timestamp when the entry was created", example = "2023-09-23T10:15:30")
  val createdTime: LocalDateTime,

  @Schema(description = "User who updated the entry", example = "admin2")
  val updatedBy: String?,

  @Schema(description = "Timestamp when the entry was updated", example = "2023-09-24T12:00:00")
  val updatedTime: LocalDateTime?,
)
