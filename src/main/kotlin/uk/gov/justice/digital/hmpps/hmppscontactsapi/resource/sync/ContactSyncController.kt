package uk.gov.justice.digital.hmpps.hmppscontactsapi.resource.sync

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springdoc.core.converters.models.PageableAsQueryParam
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort.Direction
import org.springframework.data.web.PageableDefault
import org.springframework.data.web.PagedModel
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppscontactsapi.facade.SyncFacade
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncCreateContactRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncUpdateContactRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync.SyncContact
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync.SyncContactId
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync.SyncContactReconcile
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync.SyncPrisonerReconcile
import uk.gov.justice.digital.hmpps.hmppscontactsapi.swagger.AuthApiResponses
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@Tag(name = "Migrate and sync")
@RestController
@RequestMapping(value = ["/sync"], produces = [MediaType.APPLICATION_JSON_VALUE])
@AuthApiResponses
class ContactSyncController(
  val syncFacade: SyncFacade,
) {
  @GetMapping(path = ["/contact/{contactId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @Operation(
    summary = "Returns the data for a contact by contactId",
    description = """
      Requires role: PERSONAL_RELATIONSHIPS_MIGRATION.
      Used to get the details for one contact.
      """,
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Found the contact",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = SyncContact::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "No contact reference with that id could be found",
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('PERSONAL_RELATIONSHIPS_MIGRATION')")
  fun syncGetContactById(
    @Parameter(description = "The internal ID for a contact.", required = true)
    @PathVariable contactId: Long,
  ) = syncFacade.getContactById(contactId)

  @DeleteMapping(path = ["/contact/{contactId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @Operation(
    summary = "Deletes one contact by internal ID",
    description = """
      Requires role: PERSONAL_RELATIONSHIPS_MIGRATION.
      Used to delete a contact.
      """,
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "204",
        description = "Successfully deleted contact",
      ),
      ApiResponse(
        responseCode = "404",
        description = "No contact reference with that id could be found",
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('PERSONAL_RELATIONSHIPS_MIGRATION')")
  fun syncDeleteContactById(
    @Parameter(description = "The internal ID for the contact.", required = true)
    @PathVariable contactId: Long,
  ) = syncFacade.deleteContact(contactId)

  @PostMapping(path = ["/contact"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @ResponseBody
  @Operation(
    summary = "Creates a new contact",
    description = """
      Requires role: PERSONAL_RELATIONSHIPS_MIGRATION.
      Used to create a contact and associate it with a contact.
      """,
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "201",
        description = "Successfully created contact",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = SyncContact::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "400",
        description = "The request has invalid or missing fields",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "409",
        description = "Conflict. The personId provided in the request already exists as a contact",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('PERSONAL_RELATIONSHIPS_MIGRATION')")
  fun syncCreateContact(
    @Valid @RequestBody createContactRequest: SyncCreateContactRequest,
  ) = syncFacade.createContact(createContactRequest)

  @PutMapping(path = ["/contact/{contactId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @ResponseBody
  @Operation(
    summary = "Updates a contact with new or extra detail",
    description = """
      Requires role: PERSONAL_RELATIONSHIPS_MIGRATION.
      Used to update a contact.
      """,
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Successfully updated contact",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = SyncContact::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Contact not found",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Invalid input data",
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('PERSONAL_RELATIONSHIPS_MIGRATION')")
  fun syncUpdateContact(
    @Parameter(description = "The internal ID for the contact.", required = true)
    @PathVariable contactId: Long,
    @Valid @RequestBody updateContactRequest: SyncUpdateContactRequest,
  ) = syncFacade.updateContact(contactId, updateContactRequest)

  @GetMapping("/contact/reconcile")
  @Operation(
    summary = "Reconciliation endpoint",
    description = "Get a paged list of existing contact IDs to reconcile against",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Pageable contact IDs returned",
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('PERSONAL_RELATIONSHIPS_MIGRATION')")
  @PageableAsQueryParam
  fun reconcileContacts(
    @Parameter(hidden = true)
    @PageableDefault(sort = ["contactId"], size = 100, direction = Direction.ASC)
    pageable: Pageable,
  ): PagedModel<SyncContactId> = syncFacade.getContactIds(pageable)

  @GetMapping("/contact/{contactId}/reconcile")
  @Operation(
    summary = "Reconciliation endpoint for a single contact by ID",
    description = "Get a minimal version of a contact and its main sub-entities to reconcile against",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = SyncContactReconcile::class),
          ),
        ],
        description = "Reconciliation object for one contact",
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('PERSONAL_RELATIONSHIPS_MIGRATION')")
  fun reconcileSingleContact(
    @Parameter(description = "The internal ID for the contact.", required = true)
    @PathVariable contactId: Long,
    @RequestParam(name = "currentTermOnly", required = false, defaultValue = "true")
    @Parameter(`in` = ParameterIn.QUERY, description = "filter results by current terms", example = "true", required = false)
    currentTermOnly: Boolean = true,
  ) = syncFacade.reconcileSingleContact(contactId, currentTermOnly)

  @GetMapping("/prisoner/{prisonerNumber}/reconcile")
  @Operation(
    summary = "Reconciliation endpoint for a single prisoner by prisoner number",
    description = "Get all the relationships, active and inactive, and their restrictions for one prisoner",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = SyncPrisonerReconcile::class),
          ),
        ],
        description = "Reconciliation object for one contact",
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('PERSONAL_RELATIONSHIPS_MIGRATION')")
  fun reconcileSinglePrisoner(
    @Parameter(description = "The internal ID for the contact.", required = true)
    @PathVariable prisonerNumber: String,
  ) = syncFacade.reconcileSinglePrisoner(prisonerNumber)
}
