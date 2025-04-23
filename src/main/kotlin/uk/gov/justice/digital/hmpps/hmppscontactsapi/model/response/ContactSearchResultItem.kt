package uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.AddressLines
import java.time.LocalDate
import java.time.LocalDateTime

@Schema(description = "The details of a contact as an individual")
data class ContactSearchResultItem(

  @Schema(description = "The id of the contact", example = "123456")
  val id: Long,

  @Schema(description = "The last name of the contact", example = "Doe")
  val lastName: String,

  @Schema(description = "The first name of the contact", example = "John")
  val firstName: String,

  @Schema(description = "The middle name of the contact, if any", example = "William", nullable = true)
  val middleNames: String? = null,

  @Schema(description = "The date of birth of the contact, if known", example = "1980-01-01", nullable = true)
  val dateOfBirth: LocalDate? = null,

  @Schema(description = "The date the contact deceased, if known", example = "1980-01-01", nullable = true)
  val deceasedDate: LocalDate? = null,

  @Schema(description = "The id of the user who created the contact", example = "JD000001")
  val createdBy: String? = null,

  @Schema(description = "The timestamp of when the contact was created", example = "2024-01-01T00:00:00Z")
  val createdTime: LocalDateTime? = null,

  @Schema(description = "The flat of the contact address, if known", example = "01", nullable = true)
  override val flat: String? = null,

  @Schema(description = "The property of the contact address, if known", example = "01", nullable = true)
  override val property: String? = null,

  @Schema(description = "The street of the contact address, if known", example = "Bluebell Crescent", nullable = true)
  override val street: String? = null,

  @Schema(description = "The area of the contact address, if known", example = "Birmingham", nullable = true)
  override val area: String? = null,

  @Schema(description = "The city code of the contact address, if known", example = "25343", nullable = true)
  override val cityCode: String? = null,

  @Schema(description = "The description of city code, if known", example = "Sheffield", nullable = true)
  val cityDescription: String? = null,

  @Schema(description = "The county code of the contact address, if known", example = "S.YORKSHIRE", nullable = true)
  override val countyCode: String? = null,

  @Schema(description = "The description of county code, if known", example = "South Yorkshire", nullable = true)
  val countyDescription: String? = null,

  @Schema(description = "The postcode of the contact address, if known", example = "B42 2QJ", nullable = true)
  override val postcode: String? = null,

  @Schema(description = "The country code of the contact address, if known", example = "ENG", nullable = true)
  override val countryCode: String? = null,

  @Schema(description = "The description of country code, if known", example = "England", nullable = true)
  val countryDescription: String? = null,

  @Schema(description = "If true this address should be considered for sending mail to", nullable = true, example = "true")
  val mailAddress: Boolean?,

  @Schema(description = "The date from which this address can be considered active", example = "2022-10-01", nullable = true)
  val startDate: LocalDate? = null,

  @Schema(description = "The date after which this address should be considered inactive", example = "2023-10-02", nullable = true)
  val endDate: LocalDate? = null,

  @Schema(description = "A flag to indicate that this address is effectively no fixed address", example = "false", nullable = true)
  val noFixedAddress: Boolean? = false,

  @Schema(description = "Any additional information or comments about the address", example = "Some additional information", nullable = true)
  val comments: String?,

) : AddressLines<String?>
