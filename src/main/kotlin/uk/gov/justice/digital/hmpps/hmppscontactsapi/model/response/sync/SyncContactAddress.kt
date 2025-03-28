package uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalDateTime

@Schema(description = "An address related to a contact")
data class SyncContactAddress(

  @Schema(description = "The id of the contact address", example = "123456")
  val contactAddressId: Long = 0,

  @Schema(description = "The id of the contact", example = "123456")
  val contactId: Long,

  @Schema(
    description =
    """
    The type of address.
    This is a coded value (from the group code ADDRESS_TYPE in reference data).
    The known values are HOME, WORK or BUS (business address).
    """,
    example = "HOME",
    nullable = true,
  )
  val addressType: String? = null,

  @Schema(description = "True if this is the primary address otherwise false", example = "true")
  val primaryAddress: Boolean,

  @Schema(description = "Flat number or name", example = "Flat 2B", nullable = true)
  val flat: String? = null,

  @Schema(description = "Building or house number or name", example = "Mansion House", nullable = true)
  val property: String? = null,

  @Schema(description = "Street or road name", example = "Acacia Avenue", nullable = true)
  val street: String? = null,

  @Schema(description = "Area", example = "Morton Heights", nullable = true)
  val area: String? = null,

  @Schema(description = "City code - from NOMIS", example = "BIRM", nullable = true)
  val cityCode: String? = null,

  @Schema(description = "County code - from NOMIS", example = "WMIDS", nullable = true)
  val countyCode: String? = null,

  @Schema(description = "Postcode", example = "S13 4FH", nullable = true)
  val postcode: String? = null,

  @Schema(description = "Country code - from NOMIS", example = "UK", nullable = true)
  val countryCode: String? = null,

  @Schema(description = "Whether the address has been verified by postcode lookup", example = "false")
  val verified: Boolean = false,

  @Schema(description = "Which username ran the postcode lookup check", example = "NJKG44D")
  val verifiedBy: String? = null,

  @Schema(description = "The timestamp of when the postcode lookup was done", example = "2024-01-01T00:00:00Z")
  val verifiedTime: LocalDateTime? = null,

  @Schema(description = "Flag to indicate whether mail is allowed to be sent to this address", example = "false")
  val mailFlag: Boolean = false,

  @Schema(description = "The start date when this address is to be considered active from", example = "2024-01-01")
  val startDate: LocalDate? = null,

  @Schema(description = "The end date when this address is to be considered no longer active", example = "2024-01-01")
  val endDate: LocalDate? = null,

  @Schema(description = "Flag to indicate whether this address indicates no fixed address", example = "false")
  val noFixedAddress: Boolean = false,

  @Schema(description = "Any additional information or comments about the address", example = "Some additional information", nullable = true)
  val comments: String? = null,

  @Schema(description = "The id of the user who created the contact", example = "JD000001")
  val createdBy: String,

  @Schema(description = "The timestamp of when the contact was created", example = "2024-01-01T00:00:00Z")
  val createdTime: LocalDateTime,

  @Schema(description = "The id of the user who last updated the contact address", example = "JD000001")
  val updatedBy: String? = null,

  @Schema(description = "The timestamp of when the contact address was last updated", example = "2024-01-01T00:00:00Z")
  val updatedTime: LocalDateTime? = null,
)
