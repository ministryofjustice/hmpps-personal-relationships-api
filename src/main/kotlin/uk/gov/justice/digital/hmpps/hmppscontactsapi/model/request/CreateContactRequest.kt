package uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Size
import org.springframework.format.annotation.DateTimeFormat
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.address.Address
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.email.EmailAddress
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.employment.Employment
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.identity.IdentityDocument
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.phone.PhoneNumber
import java.time.LocalDate

@Schema(description = "Request to create a new contact")
data class CreateContactRequest(

  @Schema(
    description =
    """
      The title code for the contact.
      This is a coded value (from the group code TITLE in reference data).
      Known values are MR, MRS, MISS, DR, MS, REV, SIR, BR, SR.
      """,
    example = "MR",
    nullable = true,
    maxLength = 12,
  )
  @field:Size(max = 12, message = "titleCode must be <= 12 characters")
  val titleCode: String? = null,

  @Schema(description = "The last name of the contact", example = "Doe", maxLength = 35)
  @field:Size(max = 35, message = "lastName must be <= 35 characters")
  val lastName: String,

  @Schema(description = "The first name of the contact", example = "John", maxLength = 35)
  @field:Size(max = 35, message = "firstName must be <= 35 characters")
  val firstName: String,

  @Schema(description = "The middle names of the contact, if any", example = "William", nullable = true, maxLength = 35)
  @field:Size(max = 35, message = "middleNames must be <= 35 characters")
  val middleNames: String? = null,

  @Schema(description = "The date of birth of the contact, if known", example = "1980-01-01", nullable = true)
  @field:DateTimeFormat(pattern = "yyyy-MM-dd")
  val dateOfBirth: LocalDate? = null,

  @Schema(description = "Whether the contact is a staff member", example = "false", nullable = false)
  @JsonProperty(required = true)
  var isStaff: Boolean = false,

  @Schema(
    description = """
      The primary language of the contact.
      This is a coded value (from the group code LANGUAGE in reference data).
      """,
    example = "ENG",
    nullable = true,
  )
  @field:Size(max = 12, message = "languageCode must be <= 12 characters")
  val languageCode: String? = null,

  @Schema(description = "Whether an interpreter is required for this contact", example = "true", nullable = true)
  @JsonProperty(required = true)
  val interpreterRequired: Boolean = false,

  @Schema(
    description = """
      The domestic status of the contact.
      This is a coded value (from the group code DOMESTIC_STS in reference data).
      """,
    example = "S",
    nullable = true,
  )
  @field:Size(max = 12, message = "domesticStatusCode must be <= 12 characters")
  val domesticStatusCode: String? = null,

  @Schema(
    description = """
      The domestic status of the contact.
      This is a coded value (from the group code GENDER in reference data).
      """,
    examples = ["M", "F"],
    nullable = true,
  )
  @field:Size(max = 12, message = "genderCode must be <= 12 characters")
  val genderCode: String? = null,

  @Schema(description = "A description of the relationship if the contact should be linked to a prisoner", nullable = true, exampleClasses = [ContactRelationship::class])
  @field:Valid
  val relationship: ContactRelationship? = null,

  @Schema(description = "Identity documents", required = false)
  @field:Valid
  val identities: List<IdentityDocument> = emptyList(),

  @Schema(description = "Addresses", required = false)
  @field:Valid
  val addresses: List<Address> = emptyList(),

  @Schema(description = "Phone numbers", required = false)
  @field:Valid
  val phoneNumbers: List<PhoneNumber> = emptyList(),

  @Schema(description = "Email addresses", required = false)
  @field:Valid
  val emailAddresses: List<EmailAddress> = emptyList(),

  @Schema(description = "Employments", required = false)
  @field:Valid
  val employments: List<Employment> = emptyList(),

  @Schema(description = "The id of the user creating the contact", example = "JD000001", maxLength = 100)
  @field:Size(max = 100, message = "createdBy must be <= 100 characters")
  val createdBy: String,
)
