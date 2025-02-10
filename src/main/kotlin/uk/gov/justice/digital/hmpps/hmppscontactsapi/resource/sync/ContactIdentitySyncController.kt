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
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncCreateContactIdentityRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncUpdateContactIdentityRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync.SyncContactIdentity
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@Tag(name = "Sync & Migrate")
@RestController
@RequestMapping(value = ["/sync"], produces = [MediaType.APPLICATION_JSON_VALUE])
class ContactIdentitySyncController(
  val syncFacade: SyncFacade,
) {
  @GetMapping(path = ["/contact-identity/{contactIdentityId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @Operation(
    summary = "Returns the data for a contact identity by contactIdentityId",
    description = """
      Requires role: PERSONAL_RELATIONSHIPS_MIGRATION.
      Used to get the details for one contact identity.
      """,
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Found the contact identity",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = SyncContactIdentity::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "No contact identity with that id could be found",
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('PERSONAL_RELATIONSHIPS_MIGRATION')")
  fun syncGetContactIdentityById(
    @Parameter(description = "The internal ID for a contact identity.", required = true)
    @PathVariable contactIdentityId: Long,
  ) = syncFacade.getContactIdentityById(contactIdentityId)

  @DeleteMapping(path = ["/contact-identity/{contactIdentityId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @Operation(
    summary = "Deletes one contact identity by internal ID",
    description = """
      Requires role: PERSONAL_RELATIONSHIPS_MIGRATION.
      Used to delete a contact identity.
      """,
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "204",
        description = "Successfully deleted contact identity",
      ),
      ApiResponse(
        responseCode = "404",
        description = "No contact identity with that id could be found",
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('PERSONAL_RELATIONSHIPS_MIGRATION')")
  fun syncDeleteContactIdentityById(
    @Parameter(description = "The internal ID for the contact identity.", required = true)
    @PathVariable contactIdentityId: Long,
  ) = syncFacade.deleteContactIdentity(contactIdentityId)

  @PostMapping(path = ["/contact-identity"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @ResponseBody
  @Operation(
    summary = "Creates a new contact identity",
    description = """
      Requires role: PERSONAL_RELATIONSHIPS_MIGRATION.
      Used to create a contact identity and associate it with a contact.
      """,
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "201",
        description = "Successfully created contact identity",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = SyncContactIdentity::class),
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
  fun syncCreateContactIdentity(
    @Valid @RequestBody createContactIdentityRequest: SyncCreateContactIdentityRequest,
  ) = syncFacade.createContactIdentity(createContactIdentityRequest)

  @PutMapping(path = ["/contact-identity/{contactIdentityId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @ResponseBody
  @Operation(
    summary = "Updates a contact identity with new or extra detail",
    description = """
      Requires role: PERSONAL_RELATIONSHIPS_MIGRATION.
      Used to update a contact identity.
      """,
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Successfully updated contact identity",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = SyncContactIdentity::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Contact identity not found",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Invalid input data",
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('PERSONAL_RELATIONSHIPS_MIGRATION')")
  fun syncUpdateContactIdentity(
    @Parameter(description = "The internal ID for the contact identity.", required = true)
    @PathVariable contactIdentityId: Long,
    @Valid @RequestBody updateContactIdentityRequest: SyncUpdateContactIdentityRequest,
  ) = syncFacade.updateContactIdentity(contactIdentityId, updateContactIdentityRequest)
}
