package uk.gov.justice.digital.hmpps.hmppscontactsapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springdoc.core.converters.models.PageableAsQueryParam
import org.springframework.data.domain.Pageable
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.LinkedPrisonerPage
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.LinkedPrisonersService
import uk.gov.justice.digital.hmpps.hmppscontactsapi.swagger.AuthApiResponses
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@Tag(name = "Contacts")
@RestController
@RequestMapping(value = ["contact/{contactId}/linked-prisoners"], produces = [MediaType.APPLICATION_JSON_VALUE])
@AuthApiResponses
class ContactLinkedPrisonersController(private val linkedPrisonersService: LinkedPrisonersService) {

  @GetMapping
  @Operation(
    summary = "Get linked prisoners",
    description = "Gets a list of prisoners that have an active relationship with the contact",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Found the the linked prisoners successfully. Can be an empty list.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = LinkedPrisonerPage::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Could not find the the contact",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('ROLE_CONTACTS_ADMIN', 'ROLE_CONTACTS__R', 'ROLE_CONTACTS__RW')")
  @PageableAsQueryParam
  fun getContactLinkedPrisoners(
    @PathVariable("contactId") @Parameter(
      name = "contactId",
      description = "The id of the contact",
      example = "123456",
    ) contactId: Long,
    @Parameter(description = "Pageable configurations", required = false)
    pageable: Pageable,
  ) = linkedPrisonersService.getLinkedPrisoners(contactId, pageable)
}
