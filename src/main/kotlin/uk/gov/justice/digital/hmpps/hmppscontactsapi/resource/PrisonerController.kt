package uk.gov.justice.digital.hmpps.hmppscontactsapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
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
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.internal.PrisonerContactSearchParams
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.PrisonerContactSummary
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
  @PageableAsQueryParam
  fun getAllContacts(
    @PathVariable("prisonNumber") @PrisonNumberDoc prisonerNumber: String,
    @Parameter(
      `in` = ParameterIn.QUERY,
      description = "If specified and true then only relationships that are active will be returned, or if false only inactive ones. If omitted, then all relationships will be returned.",
      required = false,
    )
    active: Boolean? = null,
    @Parameter(
      `in` = ParameterIn.QUERY,
      description = "If specified then only relationships of that type will be returned. If omitted, then all relationship types will be returned. Use \"S\" for Social and \"O\" for Official relationships.",
      examples = [ExampleObject("S"), ExampleObject("O")],
      schema = Schema(allowableValues = ["S", "O"]),
      required = false,
    )
    relationshipType: String? = null,
    @Parameter(
      `in` = ParameterIn.QUERY,
      description = "If specified and true only returns results where the contact is listed as an emergency contact for the prisoner or is not an emergency contact if false",
      required = false,
    )
    emergencyContact: Boolean? = null,
    @Parameter(
      `in` = ParameterIn.QUERY,
      description = "If specified and true only returns results where the contact is listed as next of kin for the prisoner or is not next of kin if false",
      required = false,
    )
    nextOfKin: Boolean? = null,
    @Parameter(
      `in` = ParameterIn.QUERY,
      description = "If specified and true only includes results that are listed as either an emergency contact or next of kin for the prisoner. If false then only returns contacts that are neither next of kin or emergency contact.",
      required = false,
    )
    emergencyContactOrNextOfKin: Boolean? = null,
    @Parameter(hidden = true) pageable: Pageable,
  ): PagedModel<PrisonerContactSummary> {
    val params = PrisonerContactSearchParams(
      prisonerNumber = prisonerNumber,
      active = active,
      relationshipType = relationshipType,
      emergencyContact = emergencyContact,
      nextOfKin = nextOfKin,
      emergencyContactOrNextOfKin = emergencyContactOrNextOfKin,
      pageable = pageable,
    )
    return prisonerContactService.getAllContacts(params)
  }
}
