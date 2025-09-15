package uk.gov.justice.digital.hmpps.hmppscontactsapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springdoc.core.converters.models.PageableAsQueryParam
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedModel
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppscontactsapi.facade.PrisonerRestrictionsFacade
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.PrisonerRestrictionDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.swagger.AuthApiResponses
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@Tag(name = "Prisoner Restrictions")
@RestController
@RequestMapping(value = ["prisoner-restrictions"], produces = [MediaType.APPLICATION_JSON_VALUE])
@AuthApiResponses
class PrisonerRestrictionsController(
  private val prisonerRestrictionsFacade: PrisonerRestrictionsFacade,
) {
  @GetMapping("/{prisonerNumber}")
  @Operation(
    summary = "Get all restrictions for a prisoner",
    description = "Returns all restrictions for a prisoner, optionally filtering by current term only, with paging.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Found restrictions",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Invalid request",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('ROLE_CONTACTS_ADMIN', 'ROLE_CONTACTS__R', 'ROLE_CONTACTS__RW')")
  @PageableAsQueryParam
  fun getPrisonerRestrictions(
    @PathVariable("prisonerNumber") @Parameter(
      name = "prisonerNumber",
      description = "The prisoner number",
      example = "A1234BC",
    )
    @Parameter(`in` = ParameterIn.PATH, description = "The prisoner number", example = "A1234BC", required = true)
    prisonerNumber: String,
    @RequestParam(name = "currentTermOnly", required = false, defaultValue = "false")
    @Parameter(`in` = ParameterIn.QUERY, description = "filter results by current terms", example = "true", required = false)
    currentTermOnly: Boolean = false,
    @RequestParam(name = "paged", required = false, defaultValue = "true")
    @Parameter(`in` = ParameterIn.QUERY, description = "return paged results (default true); if false returns all records", example = "true", required = false)
    paged: Boolean = true,
    @Parameter(hidden = true)
    pageable: Pageable,
  ): PagedModel<PrisonerRestrictionDetails> = prisonerRestrictionsFacade.getPrisonerRestrictions(prisonerNumber, currentTermOnly, pageable, paged)
}
