package uk.gov.justice.digital.hmpps.hmppscontactsapi.resource.sync

import io.swagger.v3.oas.annotations.Operation
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
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncUpdatePrisonerNumberOfChildrenRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync.SyncPrisonerNumberOfChildrenResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.swagger.AuthApiResponses

@Tag(name = "Sync & Migrate")
@RestController
@RequestMapping(value = ["/sync"], produces = [MediaType.APPLICATION_JSON_VALUE])
class PrisonerNumberOfChildrenSyncController {
  @GetMapping(path = ["/{prisonerNumber}/number-of-children"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @Operation(
    summary = "Returns the number of children for a prisoner by prisonerNumber",
    description = """
      Requires role: PERSONAL_RELATIONSHIPS_MIGRATION.
      Used to get the number of children for one prisoner.
      """,
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Found the number of children",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = SyncPrisonerNumberOfChildrenResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "No number of children for that prisoner could be found",
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('PERSONAL_RELATIONSHIPS_MIGRATION')")
  fun syncGetNumberOfChildrenByPrisonerNumber(
    @PathVariable prisonerNumber: String,
  ): SyncPrisonerNumberOfChildrenResponse = SyncPrisonerNumberOfChildrenResponse(
    id = 1L,
  )

  /**
   * Creates a number of children record from NOMIS.
   * If a record already exists, it will be moved to history for auditability before creating the new record.
   *
   * Updates an existing number of children record from NOMIS.
   * The existing record will be moved to history for auditability before creating the new updated record.
   */

  @PutMapping(path = ["/{prisonerNumber}/number-of-children"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @ResponseBody
  @Operation(
    summary = "Create or Updates the number of children for a prisoner",
    description = """
      Requires role: PERSONAL_RELATIONSHIPS_MIGRATION.
      Used to update a prisoner's number of children.
      """,
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Successfully created/updated Prisoner's number of children",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = SyncPrisonerNumberOfChildrenResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Prisoner's number of children not found",
      ),
    ],
  )
  @AuthApiResponses
  @PreAuthorize("hasAnyRole('PERSONAL_RELATIONSHIPS_MIGRATION')")
  fun syncUpdateNumberOfChildren(
    @PathVariable prisonerNumber: String,
    @Valid @RequestBody request: SyncUpdatePrisonerNumberOfChildrenRequest,
  ): SyncPrisonerNumberOfChildrenResponse = SyncPrisonerNumberOfChildrenResponse(
    id = 1L,
  )

  /**
   * When deleting a record in NOMIS, the record will be moved to inactive number of children rather than being deleted.
   * This preserves the record history while marking it as no longer active.
   */

  @DeleteMapping(path = ["/{prisonerNumber}/number-of-children"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @Operation(
    summary = "Deletes an Prisoner's number of children",
    description = "Delete prisoner's number of children record by prisoner number.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "204",
        description = "Successfully deleted the number of children for the requested prisoner.",
      ),
      ApiResponse(
        responseCode = "404",
        description = "No active number of children found for the requested prisoner.",
      ),
    ],
  )
  @AuthApiResponses
  @PreAuthorize("hasAnyRole('PERSONAL_RELATIONSHIPS_MIGRATION')")
  fun syncDeleteNumberOfChildrenById(@PathVariable prisonerNumber: String) {
  }
}
