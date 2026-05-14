package uk.gov.justice.digital.hmpps.personalrelationships.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personalrelationships.facade.ContactGlobalRestrictionsFacade
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.ContactIdsRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.ContactsRestrictionsResponse

@RestController
@RequestMapping(value = ["contacts/restrictions"], produces = [MediaType.APPLICATION_JSON_VALUE])
class ContactsGlobalRestrictionsController(
  val restrictionsFacade: ContactGlobalRestrictionsFacade,
) {
  @Operation(
    summary = "Get global restrictions for one or more contacts where matches are found",
    description = """
      Get the global restrictions that apply to the specified contacts.

      Global restrictions apply to all of a contact's relationships and are known as estate-wide restrictions in NOMIS.

      Prisoner-contact restrictions for specific prisoner relationships will not be returned.
    """,
  )
  @Tag(name = "Restrictions")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "The global restrictions for the specified contact(s)",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ContactsRestrictionsResponse::class),
          ),
        ],
      ),
    ],
  )
  @PostMapping
  @PreAuthorize("hasAnyRole('ROLE_CONTACTS_ADMIN', 'ROLE_CONTACTS__R', 'ROLE_CONTACTS__RW')")
  fun getContactGlobalRestrictionsByContactIds(
    @RequestBody
    @Parameter(description = "The ids of the contacts to search for", required = true)
    contactIdsRequest: ContactIdsRequest,
  ): ContactsRestrictionsResponse = restrictionsFacade.getGlobalRestrictionsForContacts(contactIdsRequest.contactIds.toSet())
}
