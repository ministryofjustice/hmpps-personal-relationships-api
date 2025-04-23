package uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.AddressLines
import java.time.LocalDate
import java.time.LocalDateTime

@Schema(description = "An address related to a contact with descriptions of all reference data")
data class ContactAddressDetails(

  @Schema(description = "The id of the contact address", example = "123456")
  val contactAddressId: Long,

  @Schema(description = "The id of the contact", example = "123456")
  val contactId: Long,

  @Schema(
    description =
    """
      The type of address (optional).
      This is a coded value (from the group code ADDRESS_TYPE in reference data).
      The known values are HOME, WORK or BUS (business address).
    """,
    example = "HOME",
    nullable = true,
  )
  val addressType: String?,

  @Schema(description = "The description of the address type", example = "HOME", nullable = true)
  val addressTypeDescription: String?,

  @Schema(description = "True if this is the primary address otherwise false", example = "true")
  val primaryAddress: Boolean,

  @Schema(description = "Flat number or name", example = "Flat 2B", nullable = true)
  override val flat: String?,

  @Schema(description = "Building or house number or name", example = "Mansion House", nullable = true)
  override val property: String?,

  @Schema(description = "Street or road name", example = "Acacia Avenue", nullable = true)
  override val street: String?,

  @Schema(description = "Area", example = "Morton Heights", nullable = true)
  override val area: String?,

  @Schema(description = "City code", example = "25343", nullable = true)
  override val cityCode: String?,

  @Schema(description = "The description of city code", example = "Sheffield", nullable = true)
  val cityDescription: String?,

  @Schema(description = "County code", example = "S.YORKSHIRE", nullable = true)
  override val countyCode: String?,

  @Schema(description = "The description of county code", example = "South Yorkshire", nullable = true)
  val countyDescription: String?,

  @Schema(description = "Postcode", example = "S13 4FH", nullable = true)
  override val postcode: String?,

  @Schema(description = "Country code", example = "ENG", nullable = true)
  override val countryCode: String?,

  @Schema(description = "The description of country code", example = "England", nullable = true)
  val countryDescription: String?,

  @Schema(description = "Whether the address has been verified by postcode lookup", example = "false")
  val verified: Boolean,

  @Schema(description = "Which username ran the postcode lookup check", example = "NJKG44D", nullable = true)
  val verifiedBy: String?,

  @Schema(description = "The timestamp of when the postcode lookup was done", example = "2024-01-01T00:00:00Z")
  val verifiedTime: LocalDateTime?,

  @Schema(description = "Flag to indicate whether mail is allowed to be sent to this address", example = "false")
  val mailFlag: Boolean,

  @Schema(description = "The start date when this address is to be considered active from", example = "2024-01-01", nullable = true)
  val startDate: LocalDate?,

  @Schema(description = "The end date when this address is to be considered no longer active", example = "2024-01-01", nullable = true)
  val endDate: LocalDate?,

  @Schema(description = "Flag to indicate whether this address indicates no fixed address", example = "false")
  val noFixedAddress: Boolean,

  @Schema(description = "Any additional information or comments about the address", example = "Some additional information", nullable = true)
  val comments: String?,

  @Schema(description = "Phone numbers that are related to this address")
  val phoneNumbers: List<ContactAddressPhoneDetails>,

  @Schema(description = "The id of the user who created the contact", example = "JD000001")
  val createdBy: String,

  @Schema(description = "The timestamp of when the contact was created", example = "2024-01-01T00:00:00Z")
  val createdTime: LocalDateTime,

  @Schema(description = "The id of the user who last updated the contact address", example = "JD000001", nullable = true)
  val updatedBy: String?,

  @Schema(description = "The timestamp of when the contact address was last updated", example = "2024-01-01T00:00:00Z", nullable = true)
  val updatedTime: LocalDateTime?,
) : AddressLines<String?>
