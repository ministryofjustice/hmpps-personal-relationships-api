package uk.gov.justice.digital.hmpps.personalrelationships.resource.sync

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
import uk.gov.justice.digital.hmpps.personalrelationships.facade.SyncFacade
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.sync.SyncCreatePrisonerContactRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.sync.SyncUpdatePrisonerContactRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.sync.SyncPrisonerContact
import uk.gov.justice.digital.hmpps.personalrelationships.swagger.AuthApiResponses
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@Tag(name = "Migrate and sync")
@RestController
@RequestMapping(value = ["/sync"], produces = [MediaType.APPLICATION_JSON_VALUE])
@AuthApiResponses
class PrisonerContactSyncController(
  val syncFacade: SyncFacade,
) {
  @GetMapping(path = ["/prisoner-contact/{prisonerContactId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @Operation(
    summary = "Returns the data for a prisoner contact by prisonerContactId",
    description = """
      Requires role: PERSONAL_RELATIONSHIPS_MIGRATION.
      Used to get the details for one prisoner contact.
      """,
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Found the prisoner contact",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = SyncPrisonerContact::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "No prisoner contact reference with that id could be found",
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('PERSONAL_RELATIONSHIPS_MIGRATION')")
  fun syncGetPrisonerContactById(
    @Parameter(description = "The internal ID for a prisoner contact.", required = true)
    @PathVariable prisonerContactId: Long,
  ) = syncFacade.getPrisonerContactById(prisonerContactId)

  @DeleteMapping(path = ["/prisoner-contact/{prisonerContactId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @Operation(
    summary = "Deletes one prisoner contact by internal ID",
    description = """
      Requires role: PERSONAL_RELATIONSHIPS_MIGRATION.
      Used to delete a prisoner contact.
      """,
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "204",
        description = "Successfully deleted prisoner contact",
      ),
      ApiResponse(
        responseCode = "404",
        description = "No prisoner contact reference with that id could be found",
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('PERSONAL_RELATIONSHIPS_MIGRATION')")
  fun syncDeletePrisonerContactById(
    @Parameter(description = "The internal ID for the prisoner contact.", required = true)
    @PathVariable prisonerContactId: Long,
  ) = syncFacade.deletePrisonerContact(prisonerContactId)

  @PostMapping(path = ["/prisoner-contact"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @ResponseBody
  @Operation(
    summary = "Creates a new prisoner contact",
    description = """
      Requires role: PERSONAL_RELATIONSHIPS_MIGRATION.
      Used to create a prisoner contact.
      """,
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "201",
        description = "Successfully created prisoner contact",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = SyncPrisonerContact::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "400",
        description = "The request has invalid or missing fields",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "409",
        description = "The requested combination of prisoner, contact and relationship to prisoner already exists.",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('PERSONAL_RELATIONSHIPS_MIGRATION')")
  fun syncCreatePrisonerContact(
    @Valid @RequestBody createPrisonerContactRequest: SyncCreatePrisonerContactRequest,
  ) = syncFacade.createPrisonerContact(createPrisonerContactRequest)

  @PutMapping(path = ["/prisoner-contact/{prisonerContactId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @ResponseBody
  @Operation(
    summary = "Updates a prisoner contact with new or extra detail",
    description = """
      Requires role: PERSONAL_RELATIONSHIPS_MIGRATION.
      Used to update a prisoner contact.
      """,
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Successfully updated prisoner contact",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = SyncPrisonerContact::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Prisoner contact not found",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Invalid input data",
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('PERSONAL_RELATIONSHIPS_MIGRATION')")
  fun syncUpdatePrisonerContact(
    @Parameter(description = "The internal ID for the prisoner contact.", required = true)
    @PathVariable prisonerContactId: Long,
    @Valid @RequestBody updatePrisonerContactRequest: SyncUpdatePrisonerContactRequest,
  ) = syncFacade.updatePrisonerContact(prisonerContactId, updatePrisonerContactRequest)
}
