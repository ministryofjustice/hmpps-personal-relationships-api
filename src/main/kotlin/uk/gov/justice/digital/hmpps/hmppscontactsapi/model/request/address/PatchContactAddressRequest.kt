package uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.address
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import org.openapitools.jackson.nullable.JsonNullable
import java.time.LocalDate

@Schema(description = "Request to patch a contact address")
data class PatchContactAddressRequest(
  @Schema(
    description =
    """
    The type of address.
    This is a coded value (from the group code ADDRESS_TYPE in reference data).
    The known values are HOME, WORK or BUS (business address).
    """,
    example = "HOME",
    nullable = true,
    maxLength = 12,
    requiredMode = Schema.RequiredMode.NOT_REQUIRED,
  )
  @field:Size(max = 12, message = "addressType must be <= 12 characters")
  val addressType: JsonNullable<String?> = JsonNullable.undefined(),

  @Schema(description = "True if this is the primary address otherwise false", example = "true", nullable = false, type = "boolean", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  val primaryAddress: JsonNullable<Boolean> = JsonNullable.undefined(),

  @Schema(description = "Flat number or name", example = "Flat 2B", nullable = true, type = "string", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @field:Size(max = 30, message = "flat must be <= 30 characters")
  val flat: JsonNullable<String?> = JsonNullable.undefined(),

  @Schema(description = "Building or house number or name", example = "Mansion House", nullable = true, type = "string", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @field:Size(max = 50, message = "property must be <= 50 characters")
  val property: JsonNullable<String?> = JsonNullable.undefined(),

  @Schema(description = "Street or road name", example = "Acacia Avenue", nullable = true, type = "string", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @field:Size(max = 160, message = "street must be <= 160 characters")
  val street: JsonNullable<String?> = JsonNullable.undefined(),

  @Schema(description = "Area", example = "Morton Heights", nullable = true, type = "string", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @field:Size(max = 70, message = "area must be <= 70 characters")
  val area: JsonNullable<String?> = JsonNullable.undefined(),

  @Schema(description = "City code - from NOMIS reference data", example = "BIRM", nullable = true, type = "string", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @field:Size(max = 12, message = "cityCode must be <= 12 characters")
  val cityCode: JsonNullable<String?> = JsonNullable.undefined(),

  @Schema(description = "County code - from NOMIS reference data", example = "WMIDS", nullable = true, type = "string", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @field:Size(max = 12, message = "countyCode must be <= 12 characters")
  val countyCode: JsonNullable<String?> = JsonNullable.undefined(),

  @Schema(description = "Postcode", example = "S13 4FH", nullable = true, type = "string", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @field:Size(max = 12, message = "postcode must be <= 12 characters")
  val postcode: JsonNullable<String?> = JsonNullable.undefined(),

  @Schema(description = "Country code - from NOMIS reference data", example = "UK", type = "string", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @field:Size(max = 12, message = "countryCode must be <= 12 characters")
  @field:NotNull
  val countryCode: JsonNullable<String> = JsonNullable.undefined(),

  @Schema(description = "Whether the address has been verified by postcode lookup", example = "false", nullable = false, type = "boolean", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  val verified: JsonNullable<Boolean> = JsonNullable.undefined(),

  @Schema(description = "Whether the address can be used for mailing", example = "false", nullable = false, type = "boolean", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  val mailFlag: JsonNullable<Boolean> = JsonNullable.undefined(),

  @Schema(description = "The start date when this address can be considered active from", example = "2023-01-12", type = "string", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  val startDate: JsonNullable<LocalDate?> = JsonNullable.undefined(),

  @Schema(description = "The end date when this address can be considered active until", example = "2023-01-12", type = "string", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  val endDate: JsonNullable<LocalDate?> = JsonNullable.undefined(),

  @Schema(description = "Flag to indicate this address should be considered as no fixed address", example = "false", nullable = false, type = "boolean", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  val noFixedAddress: JsonNullable<Boolean> = JsonNullable.undefined(),

  @Schema(description = "Any additional information or comments about the address", example = "Some additional information", nullable = true, type = "string", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @field:Size(max = 240, message = "comments must be <= 240 characters")
  val comments: JsonNullable<String?> = JsonNullable.undefined(),

  @Schema(description = "The id of the user who updated the address", example = "JD000001", nullable = false, requiredMode = Schema.RequiredMode.REQUIRED)
  @field:Size(max = 100, message = "updatedBy must be <= 100 characters")
  val updatedBy: String,
)
