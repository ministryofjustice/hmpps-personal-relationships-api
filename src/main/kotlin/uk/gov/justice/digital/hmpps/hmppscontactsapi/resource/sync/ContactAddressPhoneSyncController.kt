package uk.gov.justice.digital.hmpps.hmppscontactsapi.resource.sync

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppscontactsapi.facade.SyncFacade
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncCreateContactAddressPhoneRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncUpdateContactAddressPhoneRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync.SyncContactAddressPhone
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@Tag(name = "Migrate and sync")
@RestController
@RequestMapping(value = ["/sync"], produces = [MediaType.APPLICATION_JSON_VALUE])
class ContactAddressPhoneSyncController(
  val syncFacade: SyncFacade,
) {
  @GetMapping(path = ["/contact-address-phone/{contactAddressPhoneId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @Operation(
    summary = "Returns the data for an address-soecific phone number by contactAddressPhoneId",
    description = """
      Requires role: PERSONAL_RELATIONSHIPS_MIGRATION.
      Used to get the details for one address-specific phone number.
      """,
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "The details of the address-specific phone number",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = SyncContactAddressPhone::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "No address-specific phone number with this ID could be found",
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('PERSONAL_RELATIONSHIPS_MIGRATION')")
  fun syncGetContactPhoneById(
    @Parameter(description = "The internal ID for an address-specific phone number", required = true)
    @PathVariable contactAddressPhoneId: Long,
  ) = syncFacade.getContactAddressPhoneById(contactAddressPhoneId)

  @DeleteMapping(path = ["/contact-address-phone/{contactAddressPhoneId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @Operation(
    summary = "Deletes an addres-specific phone number by internal ID",
    description = """
      Requires role: PERSONAL_RELATIONSHIPS_MIGRATION.
      Delete an address-specific phone number by internal ID.
      """,
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "204",
        description = "Successfully deleted an address-specific phone number",
      ),
      ApiResponse(
        responseCode = "404",
        description = "No address-specific phone number with this ID could be found",
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('PERSONAL_RELATIONSHIPS_MIGRATION')")
  fun syncDeleteContactAddressPhoneById(
    @Parameter(description = "The internal ID for the address-specific phone number", required = true)
    @PathVariable contactAddressPhoneId: Long,
  ) = syncFacade.deleteContactAddressPhone(contactAddressPhoneId)

  @PostMapping(path = ["/contact-address-phone"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @ResponseBody
  @Operation(
    summary = "Creates a new address-specific phone number",
    description = """
      Requires role: PERSONAL_RELATIONSHIPS_MIGRATION.
      Used to create an address-specific phone number.
      """,
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "201",
        description = "Successfully created an address-specific phone number",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = SyncContactAddressPhone::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "400",
        description = "The request has invalid or missing fields",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "The contact address phone number was not found for the provided ID",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('PERSONAL_RELATIONSHIPS_MIGRATION')")
  fun syncCreateContactAddressPhone(
    @Valid @RequestBody request: SyncCreateContactAddressPhoneRequest,
  ) = syncFacade.createContactAddressPhone(request)

  @PutMapping(path = ["/contact-address-phone/{contactAddressPhoneId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @ResponseBody
  @Operation(
    summary = "Updates an address-specific phone number",
    description = """
      Requires role: PERSONAL_RELATIONSHIPS_MIGRATION.
      Used to update an address-specific phone number for a contact.
      """,
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Successfully updated an address-specific phone",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = SyncContactAddressPhone::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "The address-specific phone number was not found",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Invalid data provided in the request",
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('PERSONAL_RELATIONSHIPS_MIGRATION')")
  fun syncUpdateContactAddressPhone(
    @Parameter(description = "The internal ID for an address-specific phone number", required = true)
    @PathVariable contactAddressPhoneId: Long,
    @Valid @RequestBody request: SyncUpdateContactAddressPhoneRequest,
  ) = syncFacade.updateContactAddressPhone(contactAddressPhoneId, request)
}
