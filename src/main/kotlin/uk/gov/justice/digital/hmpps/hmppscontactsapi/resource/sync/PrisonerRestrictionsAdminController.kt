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
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppscontactsapi.facade.sync.PrisonerRestrictionsAdminFacade
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.MergePrisonerRestrictionsRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.ResetPrisonerRestrictionsRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.ChangedRestrictionsResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync.PrisonerRestrictionIdsResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.swagger.AuthApiResponses
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@Tag(name = "Prisoner restrictions admin")
@RestController
@RequestMapping(value = ["/prisoner-restrictions"], produces = [MediaType.APPLICATION_JSON_VALUE])
class PrisonerRestrictionsAdminController(val prisonerRestrictionsAdminFacade: PrisonerRestrictionsAdminFacade) {

  @PostMapping(path = ["/merge"], consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
  @ResponseBody
  @Operation(
    summary = "Merge prisoner restrictions from one prisoner to another, deleting all from the removed prisoner and resetting the list for the retained prisoner.",
    description = """
      Requires role: PERSONAL_RELATIONSHIPS_MIGRATION.\n" +
      "Used to merge a prisoner's restrictions records with another prisoner's restrictions.\n" +
      "Deletes all restrictions from the removed prisoner and resets the retained prisoner's restrictions to those in the request."
     """,
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Successfully merged Prisoner's restrictions records",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ChangedRestrictionsResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "400",
        description = "The request failed validation with invalid or missing data supplied",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @AuthApiResponses
  @PreAuthorize("hasAnyRole('PERSONAL_RELATIONSHIPS_MIGRATION')")
  fun mergePrisonerRestrictions(
    @Valid @RequestBody request: MergePrisonerRestrictionsRequest,
  ): ChangedRestrictionsResponse = prisonerRestrictionsAdminFacade.merge(request)

  @PostMapping(path = ["/reset"], consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
  @ResponseBody
  @Operation(
    summary = "Reset a prisoner's restrictions to match what exists in NOMIS",
    description = """
      Requires role: PERSONAL_RELATIONSHIPS_MIGRATION.
      Used to reset a prisoner's restrictions to match what exists in NOMIS.
      This is used for scenarios like booking moves, new bookings, and reinstated bookings.
      """,
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Successfully reset Prisoner's restrictions records",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ChangedRestrictionsResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "400",
        description = "The request failed validation with invalid or missing data supplied",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @AuthApiResponses
  @PreAuthorize("hasAnyRole('PERSONAL_RELATIONSHIPS_MIGRATION')")
  fun resetPrisonerRestrictions(
    @Valid @RequestBody request: ResetPrisonerRestrictionsRequest,
  ): ChangedRestrictionsResponse = prisonerRestrictionsAdminFacade.reset(request)

  @GetMapping(path = ["/{prisonerNumber}/reconcile"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @Operation(
    summary = "Reconciliation endpoint for a single prisoner by prisoner number (restriction IDs only)",
    description = "Get a list of restriction IDs for a given prisoner number",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "List of restriction IDs for the prisoner",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = PrisonerRestrictionIdsResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "No restrictions found for that prisoner number",
      ),
    ],
  )
  @AuthApiResponses
  @PreAuthorize("hasAnyRole('PERSONAL_RELATIONSHIPS_MIGRATION')")
  fun reconcilePrisonerRestrictionIds(
    @PathVariable prisonerNumber: String,
  ): PrisonerRestrictionIdsResponse = prisonerRestrictionsAdminFacade.getRestrictionIdsForPrisoner(prisonerNumber)
}
