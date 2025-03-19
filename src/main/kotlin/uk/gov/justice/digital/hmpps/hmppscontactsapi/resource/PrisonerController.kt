package uk.gov.justice.digital.hmpps.hmppscontactsapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.PrisonerContactSummary
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.PrisonerContactSummaryPage
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.PrisonerContactService
import uk.gov.justice.digital.hmpps.hmppscontactsapi.swagger.AuthApiResponses
import uk.gov.justice.digital.hmpps.hmppscontactsapi.swagger.PrisonNumberDoc
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@Tag(name = "Prisoner relationships")
@RestController
@RequestMapping(value = ["prisoner"], produces = [MediaType.APPLICATION_JSON_VALUE])
@AuthApiResponses
class PrisonerController(private val prisonerContactService: PrisonerContactService) {

  @Operation(summary = "Endpoint to fetch all contacts for a specific prisoner by prisoner number and active status")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "List of all contacts for the prisoner",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = PrisonerContactSummaryPage::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "The Prisoner was not found.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  @GetMapping(value = ["/{prisonNumber}/contact"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @PreAuthorize("hasAnyRole('ROLE_CONTACTS_ADMIN', 'ROLE_CONTACTS__R', 'ROLE_CONTACTS__RW')")
  fun getAllContacts(
    @PathVariable("prisonNumber") @PrisonNumberDoc prisonerNumber: String,
    @RequestParam(name = "active", defaultValue = "true") @Parameter(
      name = "active",
      description = "Whether to include only active (true) or inactive (false) contacts",
    ) active: Boolean,
    @Parameter(description = "Pageable configurations", required = false)
    pageable: Pageable,
  ): Page<PrisonerContactSummary> = prisonerContactService.getAllContacts(prisonerNumber, active, pageable)
}
