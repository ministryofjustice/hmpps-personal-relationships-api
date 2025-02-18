package uk.gov.justice.digital.hmpps.hmppscontactsapi.resource

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
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppscontactsapi.facade.PrisonerDomesticStatusFacade
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.UpdatePrisonerDomesticStatusRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.PrisonerDomesticStatusResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.swagger.AuthApiResponses
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@Tag(name = "Prisoner Domestic Status")
@RestController
@RequestMapping(value = ["prisoner/{prisonerNumber}/domestic-status"], produces = [MediaType.APPLICATION_JSON_VALUE])
@AuthApiResponses
class PrisonerDomesticStatusController(
  private val prisonerDomesticStatusFacade: PrisonerDomesticStatusFacade,
) {

  @Operation(summary = "Get prisoner domestic status")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Prisoner domestic status retrieved successfully",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = PrisonerDomesticStatusResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Prisoner not found",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  @GetMapping
  @PreAuthorize("hasAnyRole('ROLE_CONTACTS_ADMIN', 'ROLE_CONTACTS__R', 'ROLE_CONTACTS__RW')")
  fun getDomesticStatus(
    @PathVariable prisonerNumber: String,
  ): PrisonerDomesticStatusResponse = prisonerDomesticStatusFacade.getDomesticStatus(prisonerNumber)

  @Operation(summary = "Create or update prisoner domestic status")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Prisoner domestic status created/updated successfully",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = PrisonerDomesticStatusResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Prisoner not found",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  @PutMapping
  @PreAuthorize("hasAnyRole('ROLE_CONTACTS_ADMIN', 'ROLE_CONTACTS__RW')")
  fun createOrUpdateDomesticStatus(
    @PathVariable prisonerNumber: String,
    @Valid @RequestBody request: UpdatePrisonerDomesticStatusRequest,
  ): PrisonerDomesticStatusResponse = prisonerDomesticStatusFacade.createOrUpdateDomesticStatus(prisonerNumber, request)
}
