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
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppscontactsapi.facade.SyncFacade
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.MergePrisonerContactRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.ResetPrisonerContactRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync.MergePrisonerContactResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync.ResetPrisonerContactResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.swagger.AuthApiResponses
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@Tag(name = "Sync admin")
@RestController
@RequestMapping(value = ["/sync"], produces = [MediaType.APPLICATION_JSON_VALUE])
@AuthApiResponses
data class SyncAdminController(val syncFacade: SyncFacade) {

  @PostMapping(path = ["/admin/merge"], consumes = [MediaType.APPLICATION_JSON_VALUE])
  @Operation(
    summary = "Removes and recreates relationships and restrictions after a prisoner merge in NOMIS",
    description = "Relationships are removed for the old and new numbers, and then recreated for new number only",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "The relationships and restrictions were successfully replaced",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = MergePrisonerContactResponse::class),
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
  @PreAuthorize("hasAnyRole('PERSONAL_RELATIONSHIPS_MIGRATION')")
  fun mergePrisonerContacts(
    @Valid @RequestBody mergePrisonerContactRequest: MergePrisonerContactRequest,
  ) = syncFacade.mergePrisonerContacts(mergePrisonerContactRequest)

  @PostMapping(path = ["/admin/reset"], consumes = [MediaType.APPLICATION_JSON_VALUE])
  @Operation(
    summary = "Removes and recreates relationships and restrictions for one prisoner to reset them to match what exists in NOMIS.",
    description = "Similar to a merge but for one prisoner, catering for events like booking moves, new bookings, and reinstated bookings",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "The relationships and restrictions were successfully replaced",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ResetPrisonerContactResponse::class),
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
  @PreAuthorize("hasAnyRole('PERSONAL_RELATIONSHIPS_MIGRATION')")
  fun resetPrisonerContacts(
    @Valid @RequestBody request: ResetPrisonerContactRequest,
  ) = syncFacade.resetPrisonerContacts(request)
}
