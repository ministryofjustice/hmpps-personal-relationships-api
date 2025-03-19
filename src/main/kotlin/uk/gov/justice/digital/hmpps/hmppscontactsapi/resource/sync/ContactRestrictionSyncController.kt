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
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncCreateContactRestrictionRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncUpdateContactRestrictionRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync.SyncContactRestriction
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@Tag(name = "Migrate and sync")
@RestController
@RequestMapping(value = ["/sync"], produces = [MediaType.APPLICATION_JSON_VALUE])
class ContactRestrictionSyncController(
  val syncFacade: SyncFacade,
) {
  @GetMapping(path = ["/contact-restriction/{contactRestrictionId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @Operation(
    summary = "Returns the data for a contact restriction by contactRestrictionId",
    description = """
      Requires role: PERSONAL_RELATIONSHIPS_MIGRATION.
      Used to get the details for one contact restriction.
      """,
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Found the contact restriction",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = SyncContactRestriction::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "No contact restriction reference with that id could be found",
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('PERSONAL_RELATIONSHIPS_MIGRATION')")
  fun syncGetContactRestrictionById(
    @Parameter(description = "The internal ID for a contact restriction.", required = true)
    @PathVariable contactRestrictionId: Long,
  ) = syncFacade.getContactRestrictionById(contactRestrictionId)

  @DeleteMapping(path = ["/contact-restriction/{contactRestrictionId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @Operation(
    summary = "Deletes one contact restriction by internal ID",
    description = """
      Requires role: PERSONAL_RELATIONSHIPS_MIGRATION.
      Used to delete a contact restriction.
      """,
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "204",
        description = "Successfully deleted contact restriction",
      ),
      ApiResponse(
        responseCode = "404",
        description = "No contact restriction reference with that id could be found",
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('PERSONAL_RELATIONSHIPS_MIGRATION')")
  fun syncDeleteContactRestrictionById(
    @Parameter(description = "The internal ID for the contact restriction.", required = true)
    @PathVariable contactRestrictionId: Long,
  ) = syncFacade.deleteContactRestriction(contactRestrictionId)

  @PostMapping(path = ["/contact-restriction"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @ResponseBody
  @Operation(
    summary = "Creates a new contact restriction",
    description = """
      Requires role: PERSONAL_RELATIONSHIPS_MIGRATION.
      Used to create a contact restriction and associate it with a contact.
      """,
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "201",
        description = "Successfully created contact restriction",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = SyncContactRestriction::class),
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
  fun syncCreateContactRestriction(
    @Valid @RequestBody request: SyncCreateContactRestrictionRequest,
  ) = syncFacade.createContactRestriction(request)

  @PutMapping(path = ["/contact-restriction/{contactRestrictionId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @ResponseBody
  @Operation(
    summary = "Updates a contact restriction with new or extra detail",
    description = """
      Requires role: PERSONAL_RELATIONSHIPS_MIGRATION.
      Used to update a contact restriction.
      """,
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Successfully updated contact restriction",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = SyncContactRestriction::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Contact restriction not found",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Invalid input data",
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('PERSONAL_RELATIONSHIPS_MIGRATION')")
  fun syncUpdateContactRestriction(
    @Parameter(description = "The internal ID for the contact restriction.", required = true)
    @PathVariable contactRestrictionId: Long,
    @Valid @RequestBody request: SyncUpdateContactRestrictionRequest,
  ) = syncFacade.updateContactRestriction(contactRestrictionId, request)
}
