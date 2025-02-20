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
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncUpdatePrisonerDependentsRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync.SyncPrisonerDependentsResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.swagger.AuthApiResponses

@Tag(name = "Sync & Migrate")
@RestController
@RequestMapping(value = ["/sync"], produces = [MediaType.APPLICATION_JSON_VALUE])
class PrisonerDependentsSyncController(
  // private val prisonerDependentsSyncFacade: PrisonerDependentsSyncFacade,
) {
  @GetMapping(path = ["/{prisonerNumber}/dependents"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @Operation(
    summary = "Returns the dependents for a prisoner by prisonerNumber",
    description = """
      Requires role: PERSONAL_RELATIONSHIPS_MIGRATION.
      Used to get the dependents for one prisoner.
      """,
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Found the dependents",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = SyncPrisonerDependentsResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "No dependents for that prisoner could be found",
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('PERSONAL_RELATIONSHIPS_MIGRATION')")
  fun syncGetDependentsByPrisonerNumber(
    @PathVariable prisonerNumber: String,
  ): SyncPrisonerDependentsResponse = SyncPrisonerDependentsResponse(
    id = 1L,
    prisonerNumber = prisonerNumber,
    dependentsCount = "1",
    createdBy = "User",
    active = true,
  )

  /**
   * Creates a dependents record from NOMIS.
   * If a record already exists, it will be moved to history for auditability before creating the new record.
   *
   * Updates an existing dependents record from NOMIS.
   * The existing record will be moved to history for auditability before creating the new updated record.
   */

  @PutMapping(path = ["/{prisonerNumber}/dependents"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @ResponseBody
  @Operation(
    summary = "Create or Updates the dependents for a prisoner",
    description = """
      Requires role: PERSONAL_RELATIONSHIPS_MIGRATION.
      Used to update a prisoner's dependents.
      """,
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Successfully created/updated Prisoner's dependents",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = SyncPrisonerDependentsResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Prisoner's dependents not found",
      ),
    ],
  )
  @AuthApiResponses
  @PreAuthorize("hasAnyRole('PERSONAL_RELATIONSHIPS_MIGRATION')")
  fun syncUpdateDependents(
    @PathVariable prisonerNumber: String,
    @Valid @RequestBody request: SyncUpdatePrisonerDependentsRequest,
  ): SyncPrisonerDependentsResponse = SyncPrisonerDependentsResponse(
    id = 1L,
    prisonerNumber = prisonerNumber,
    dependentsCount = request.dependentsCount,
    createdBy = "User",
    active = true,
  )

  /**
   * When deleting a record in NOMIS, the record will be moved to inactive dependentsCount rather than being deleted.
   * This preserves the record history while marking it as no longer active.
   */

  @DeleteMapping(path = ["/{prisonerNumber}/dependents"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @Operation(
    summary = "Deletes an Prisoner's dependents",
    description = "Delete prisoner's dependents record by prisoner number.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "204",
        description = "Successfully deleted the dependents for the requested prisoner.",
      ),
      ApiResponse(
        responseCode = "404",
        description = "No active dependents found for the requested prisoner.",
      ),
    ],
  )
  @AuthApiResponses
  @PreAuthorize("hasAnyRole('PERSONAL_RELATIONSHIPS_MIGRATION')")
  fun syncDeleteDependentsById(@PathVariable prisonerNumber: String) {
    // prisonerDependentsSyncFacade.deleteDependents(prisonerNumber)
  }
}
