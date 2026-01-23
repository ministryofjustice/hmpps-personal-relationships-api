package uk.gov.justice.digital.hmpps.personalrelationships.resource.sync

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
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
import uk.gov.justice.digital.hmpps.personalrelationships.facade.sync.PrisonerRestrictionSyncFacade
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.sync.SyncCreatePrisonerRestrictionRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.sync.SyncUpdatePrisonerRestrictionRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.sync.SyncPrisonerRestriction
import uk.gov.justice.digital.hmpps.personalrelationships.swagger.AuthApiResponses
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@Tag(name = "Migrate and sync")
@RestController
@RequestMapping(value = ["/sync"], produces = [MediaType.APPLICATION_JSON_VALUE])
@AuthApiResponses
class PrisonerRestrictionsSyncController(
  val syncFacade: PrisonerRestrictionSyncFacade,
) {
  @GetMapping(
    path = ["/prisoner-restriction/{prisonerRestrictionId}"],
    produces = [MediaType.APPLICATION_JSON_VALUE],
  )
  @Operation(
    summary = "Returns the data for a prisoner restriction by id",
    description = """
      Requires role: PERSONAL_RELATIONSHIPS_MIGRATION.
      Used to get the details for one prisoner restriction.
      """,
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Found the prisoner restriction",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = SyncPrisonerRestriction::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "No prisoner restriction reference with that id could be found",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('PERSONAL_RELATIONSHIPS_MIGRATION')")
  fun syncGetPrisonerRestrictionById(
    @Parameter(description = "The internal ID for a prisoner restriction.", required = true)
    @PathVariable prisonerRestrictionId: Long,
  ) = syncFacade.getPrisonerRestrictionById(prisonerRestrictionId)

  @DeleteMapping(
    path = ["/prisoner-restriction/{prisonerRestrictionId}"],
    produces = [MediaType.APPLICATION_JSON_VALUE],
  )
  @Operation(
    summary = "Deletes one prisoner restriction by internal ID",
    description = """
      Requires role: PERSONAL_RELATIONSHIPS_MIGRATION.
      Used to delete a prisoner restriction.
      """,
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "204",
        description = "Successfully deleted prisoner restriction",
      ),
      ApiResponse(
        responseCode = "404",
        description = "No prisoner restriction reference with that id could be found",
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('PERSONAL_RELATIONSHIPS_MIGRATION')")
  fun syncDeletePrisonerRestrictionById(
    @Parameter(description = "The internal ID for the prisoner restriction.", required = true)
    @PathVariable prisonerRestrictionId: Long,
  ): ResponseEntity<Void> {
    syncFacade.deletePrisonerRestriction(prisonerRestrictionId)
    return ResponseEntity.noContent().build()
  }

  @PostMapping(path = ["/prisoner-restriction"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @ResponseBody
  @Operation(
    summary = "Creates a new prisoner restriction",
    description = """
      Requires role: PERSONAL_RELATIONSHIPS_MIGRATION.
      Used to create a prisoner restriction.
      """,
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "201",
        description = "Successfully created prisoner restriction",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = SyncPrisonerRestriction::class),
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
  fun syncCreatePrisonerRestriction(
    @Valid @RequestBody createPrisonerRestrictionRequest: SyncCreatePrisonerRestrictionRequest,
  ): ResponseEntity<SyncPrisonerRestriction> {
    val created = syncFacade.createPrisonerRestriction(createPrisonerRestrictionRequest)
    return ResponseEntity.status(HttpStatus.CREATED).body(created)
  }

  @PutMapping(
    path = ["/prisoner-restriction/{prisonerRestrictionId}"],
    produces = [MediaType.APPLICATION_JSON_VALUE],
  )
  @Operation(
    summary = "Updates an existing prisoner restriction by internal ID",
    description = """
      Requires role: PERSONAL_RELATIONSHIPS_MIGRATION.
      Used to update a prisoner restriction.
      """,
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Successfully updated prisoner restriction",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = SyncPrisonerRestriction::class),
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
        description = "No prisoner restriction reference with that id could be found",
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('PERSONAL_RELATIONSHIPS_MIGRATION')")
  fun syncUpdatePrisonerRestriction(
    @Parameter(description = "The internal ID for the prisoner restriction.", required = true)
    @PathVariable prisonerRestrictionId: Long,
    @Valid @RequestBody updatePrisonerRestrictionRequest: SyncUpdatePrisonerRestrictionRequest,
  ) = syncFacade.updatePrisonerRestriction(prisonerRestrictionId, updatePrisonerRestrictionRequest)
}
