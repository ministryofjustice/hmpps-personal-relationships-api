package uk.gov.justice.digital.hmpps.personalrelationships.resource.sync

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personalrelationships.facade.sync.PrisonerMergeFacade
import uk.gov.justice.digital.hmpps.personalrelationships.swagger.AuthApiResponses

@Tag(name = "Offender merge")
@RestController
@RequestMapping(value = ["/merge"], produces = [MediaType.APPLICATION_JSON_VALUE])
class PrisonerMergeController(val prisonerMergeFacade: PrisonerMergeFacade) {

  @PutMapping(path = ["/keep/{keepingPrisonerNumber}/remove/{removedPrisonerNumber}"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @ResponseBody
  @Operation(
    summary = "Handle the details of a prisoner when merging prisoner records",
    description = """
      Requires role: PERSONAL_RELATIONSHIPS_MIGRATION.
      Used to merge a prisoner's records.
      """,
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Successfully merged Prisoner's records",
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
    prisonerMergeFacade.merge(keepingPrisonerNumber, removedPrisonerNumber)
  }
}
