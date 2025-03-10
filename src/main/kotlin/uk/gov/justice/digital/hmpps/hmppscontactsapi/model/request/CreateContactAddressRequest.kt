package uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Size
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.phone.PhoneNumber
import java.time.LocalDate

@Schema(description = "Request to create a new contact address")
data class CreateContactAddressRequest(
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
  @field:Size(max = 12, message = "addressType must be <= 12 characters")
  val addressType: String? = null,

  @Schema(description = "True if this is the primary address otherwise false", example = "true")
  val primaryAddress: Boolean = false,

  @Schema(description = "Flat number or name", example = "Flat 2B", nullable = true)
  @field:Size(max = 30, message = "flat must be <= 30 characters")
  val flat: String? = null,

  @Schema(description = "Building or house number or name", example = "Mansion House", nullable = true)
  @field:Size(max = 50, message = "property must be <= 50 characters")
  val property: String? = null,

  @Schema(description = "Street or road name", example = "Acacia Avenue", nullable = true)
  @field:Size(max = 160, message = "street must be <= 160 characters")
  val street: String? = null,

  @Schema(description = "Area", example = "Morton Heights", nullable = true)
  @field:Size(max = 70, message = "area must be <= 70 characters")
  val area: String? = null,

  @Schema(description = "City code - from NOMIS", example = "13232", nullable = true)
  @field:Size(max = 12, message = "cityCode must be <= 12 characters")
  val cityCode: String? = null,

  @Schema(description = "County code - from NOMIS", example = "WMIDS", nullable = true)
  @field:Size(max = 12, message = "countyCode must be <= 12 characters")
  val countyCode: String? = null,

  @Schema(description = "Postcode", example = "S13 4FH", nullable = true)
  @field:Size(max = 12, message = "postcode must be <= 12 characters")
  val postcode: String? = null,

  @Schema(description = "Country code - from NOMIS", example = "UK", nullable = true)
  @field:Size(max = 12, message = "countryCode must be <= 12 characters")
  val countryCode: String? = null,

  @Schema(description = "Whether the address has been verified by postcode lookup", example = "false")
  val verified: Boolean? = false,

  @Schema(description = "Whether the address can be used for mailing", example = "false")
  val mailFlag: Boolean? = false,

  @Schema(description = "The start date when this address can be considered active from", example = "2023-01-12")
  val startDate: LocalDate? = null,

  @Schema(description = "The end date when this address can be considered active until", example = "2023-01-12")
  val endDate: LocalDate? = null,

  @Schema(description = "Flag to indicate this address should be considered as no fixed address", example = "false")
  val noFixedAddress: Boolean? = false,

  @Schema(description = "List of new address-specific phone numbers to create")
  @field:Valid
  @field:Size(min = 1, message = "phoneNumbers must have at least 1 item")
  val phoneNumbers: List<PhoneNumber>,

  @Schema(description = "Any additional information or comments about the address", example = "Some additional information", nullable = true)
  @field:Size(max = 240, message = "comments must be <= 240 characters")
  val comments: String? = null,

  @Schema(description = "The id of the user who created the contact", example = "JD000001")
  @field:Size(max = 100, message = "createdBy must be <= 100 characters")
  val createdBy: String,
)
