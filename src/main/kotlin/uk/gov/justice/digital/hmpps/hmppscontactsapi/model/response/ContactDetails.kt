package uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalDateTime

@Schema(description = "The details of a contact as an individual")
data class ContactDetails(

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

  @Schema(description = "The date of birth of the contact, if known", example = "1980-01-01", nullable = true)
  val dateOfBirth: LocalDate? = null,

  @Schema(description = "Whether the contact is a staff member", example = "false", nullable = false)
  var isStaff: Boolean = false,

  @Schema(description = "The date the contact deceased, if known", example = "1980-01-01")
  val isDeceased: Boolean,

  @Schema(description = "The date the contact deceased, if known", example = "1980-01-01", nullable = true)
  val deceasedDate: LocalDate? = null,

  @Schema(description = "The NOMIS code for the contacts language", example = "ENG", nullable = true)
  val languageCode: String?,

  @Schema(description = "The description of the language code", example = "English", nullable = true)
  val languageDescription: String?,

  @Schema(description = "Whether an interpreter is required for this contact", example = "true", nullable = true)
  val interpreterRequired: Boolean,

  @Schema(description = "All addresses for the contact")
  val addresses: List<ContactAddressDetails>,

  @Schema(description = "All phone numbers for the contact")
  val phoneNumbers: List<ContactPhoneDetails>,

  @Schema(description = "All email addresses for the contact")
  val emailAddresses: List<ContactEmailDetails>,

  @Schema(description = "All identities for the contact")
  val identities: List<ContactIdentityDetails>,

  @Schema(description = "All employments for the contact")
  val employments: List<EmploymentDetails>,

  @Schema(description = "The NOMIS code for the contacts domestic status", example = "S", nullable = true)
  val domesticStatusCode: String?,

  @Schema(description = "The description of the domestic status code", example = "Single", nullable = true)
  val domesticStatusDescription: String?,

  @Schema(description = "The NOMIS code for the contacts gender. See reference data with group code 'GENDER'", examples = ["M", "F"], nullable = true)
  val gender: String?,

  @Schema(description = "The description of gender code. See reference data with group code 'GENDER'", examples = ["Male", "Female"], nullable = true)
  val genderDescription: String?,

  @Schema(description = "The id of the user who created the contact", example = "JD000001")
  val createdBy: String,

  @Schema(description = "The timestamp of when the contact was created", example = "2024-01-01T00:00:00Z")
  val createdTime: LocalDateTime,
)
