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
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncCreateContactPhoneRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncUpdateContactPhoneRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync.SyncContactPhone
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@Tag(name = "Sync & Migrate")
@RestController
@RequestMapping(value = ["/sync"], produces = [MediaType.APPLICATION_JSON_VALUE])
class ContactPhoneSyncController(
  val syncFacade: SyncFacade,
) {
  @GetMapping(path = ["/contact-phone/{contactPhoneId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @Operation(
    summary = "Returns the data for a contact phone by contactPhoneId",
    description = """
      Requires role: PERSONAL_RELATIONSHIPS_MIGRATION.
      Used to get the details for one contact phone.
      """,
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Found the contact phone",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = SyncContactPhone::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "No contact reference with that id could be found",
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('PERSONAL_RELATIONSHIPS_MIGRATION')")
  fun syncGetContactPhoneById(
    @Parameter(description = "The internal ID for a contact phone.", required = true)
    @PathVariable contactPhoneId: Long,
  ) = syncFacade.getContactPhoneById(contactPhoneId)

  @DeleteMapping(path = ["/contact-phone/{contactPhoneId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @Operation(
    summary = "Deletes one contact phone by internal ID",
    description = """
      Requires role: PERSONAL_RELATIONSHIPS_MIGRATION.
      Used to delete a contact phone.
      """,
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "204",
        description = "Successfully deleted contact phone",
      ),
      ApiResponse(
        responseCode = "404",
        description = "No contact phone with that id could be found",
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('PERSONAL_RELATIONSHIPS_MIGRATION')")
  fun syncDeleteContactPhoneById(
    @Parameter(description = "The internal ID for the contact phone.", required = true)
    @PathVariable contactPhoneId: Long,
  ) = syncFacade.deleteContactPhone(contactPhoneId)

  @PostMapping(path = ["/contact-phone"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @ResponseBody
  @Operation(
    summary = "Creates a new contact phone",
    description = """
      Requires role: PERSONAL_RELATIONSHIPS_MIGRATION.
      Used to create a contact phone and associate it with a contact.
      """,
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "201",
        description = "Successfully created contact phone",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = SyncContactPhone::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "400",
        description = "The request has invalid or missing fields",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('PERSONAL_RELATIONSHIPS_MIGRATION')")
  fun syncCreateContactPhone(
    @Valid @RequestBody request: SyncCreateContactPhoneRequest,
  ) = syncFacade.createContactPhone(request)

  @PutMapping(path = ["/contact-phone/{contactPhoneId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @ResponseBody
  @Operation(
    summary = "Updates a phone number for a contact",
    description = """
      Requires role: PERSONAL_RELATIONSHIPS_MIGRATION.
      Used to update a contact's phone number.
      """,
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Successfully updated contact phone",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = SyncContactPhone::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Contact phone not found",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Invalid input data",
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('PERSONAL_RELATIONSHIPS_MIGRATION')")
  fun syncUpdateContactPhone(
    @Parameter(description = "The internal ID for the contact phone.", required = true)
    @PathVariable contactPhoneId: Long,
    @Valid @RequestBody request: SyncUpdateContactPhoneRequest,
  ) = syncFacade.updateContactPhone(contactPhoneId, request)
}
