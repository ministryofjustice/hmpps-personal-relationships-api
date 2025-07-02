package uk.gov.justice.digital.hmpps.hmppscontactsapi.resource.sync

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppscontactsapi.facade.sync.PrisonerRestrictionsAdminFacade
import uk.gov.justice.digital.hmpps.hmppscontactsapi.swagger.AuthApiResponses

@Tag(name = "Offender restrictions admin")
@RestController
@RequestMapping(value = ["/prisoner-restrictions"], produces = [MediaType.APPLICATION_JSON_VALUE])
class PrisonerRestrictionsAdminController(val prisonerRestrictionsAdminFacade: PrisonerRestrictionsAdminFacade) {

  @PutMapping(path = ["/keep/{keepingPrisonerNumber}/remove/{removedPrisonerNumber}"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @ResponseBody
  @Operation(
    summary = "Handle the details of a prisoner when merging prisoner restrictions records",
    description = """
      Requires role: PERSONAL_RELATIONSHIPS_MIGRATION.
      Used to merge a prisoner's restrictions records with another prisoner's restrictions .
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
          ),
        ],
      ),
    ],
  )
  @AuthApiResponses
  @PreAuthorize("hasAnyRole('PERSONAL_RELATIONSHIPS_MIGRATION')")
  fun merge(
    @PathVariable keepingPrisonerNumber: String,
    @PathVariable removedPrisonerNumber: String,
  ) {
    prisonerRestrictionsAdminFacade.merge(keepingPrisonerNumber, removedPrisonerNumber)
  }

  @PostMapping(path = ["/reset/{prisonerNumber}"], produces = [MediaType.APPLICATION_JSON_VALUE])
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
          ),
        ],
      ),
    ],
  )
  @AuthApiResponses
  @PreAuthorize("hasAnyRole('PERSONAL_RELATIONSHIPS_MIGRATION')")
  fun resetPrisonerRestrictions(
    @PathVariable prisonerNumber: String,
  ) {
    prisonerRestrictionsAdminFacade.reset(prisonerNumber)
  }
}
