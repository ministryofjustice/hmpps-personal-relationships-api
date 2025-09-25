package uk.gov.justice.digital.hmpps.hmppscontactsapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppscontactsapi.facade.ContactFacade
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.RelationshipsApprovedResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.UpdateInternalOfficialDobResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.swagger.ProtectedByIngress

/**
 * These endpoints are secured in the ingress rather than the app so that they can be called from
 * within the namespace without requiring authentication
 */

@Tag(name = "Utility Controller")
@RestController
@ProtectedByIngress
@RequestMapping(value = ["utility"], produces = [MediaType.APPLICATION_JSON_VALUE])
class UtilityController(val contactFacade: ContactFacade) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Operation(summary = "Endpoint to remove the date of birth from contacts in solely internal official relationships")
  @PutMapping(path = ["/remove-internal-official-dob"])
  @ResponseStatus(HttpStatus.OK)
  fun removeDateOfBirth(): UpdateInternalOfficialDobResponse = run {
    log.info("UTILITY: Remove internal official contacts dates of birth started")
    val result = contactFacade.removeInternalOfficialDateOfBirth()
    log.info("UTILITY: Remove internal official dates of birth completed - updated ${result.size} contacts")
    return UpdateInternalOfficialDobResponse(contactsUpdated = result)
  }

  @Operation(summary = "Endpoint to trigger the approval of a specific set of contact relationships")
  @PutMapping(path = ["/approve-contacts"])
  @ResponseStatus(HttpStatus.OK)
  fun approveContacts(
    @Parameter(`in` = ParameterIn.QUERY, description = "Provides usernames of people who created relationships (multiple values treated as a list)", example = "XYZ", required = true)
    createdBy: List<String> = emptyList(),
  ): RelationshipsApprovedResponse = run {
    log.info("UTILITY: Approve relationships to visit")
    val result = contactFacade.approveRelationships(createdBy)
    log.info("UTILITY: Approved relationships to visit - approved count ${result.size}")
    return RelationshipsApprovedResponse(relationships = result)
  }
}
