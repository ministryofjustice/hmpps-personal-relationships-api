package uk.gov.justice.digital.hmpps.hmppscontactsapi.mapping.sync

import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.PrisonerRestriction
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.PrisonerRestrictionDetailsEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncCreatePrisonerRestrictionRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.PrisonerRestrictionDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync.SyncPrisonerRestriction

fun PrisonerRestriction.mapEntityToSyncResponse(): SyncPrisonerRestriction = SyncPrisonerRestriction(
  prisonerRestrictionId = this.prisonerRestrictionId,
  prisonerNumber = this.prisonerNumber,
  restrictionType = this.restrictionType,
  effectiveDate = this.effectiveDate,
  expiryDate = this.expiryDate,
  commentText = this.commentText,
  authorisedUsername = this.authorisedUsername,
  currentTerm = this.currentTerm,
  createdBy = this.createdBy,
  createdTime = this.createdTime,
  updatedBy = this.updatedBy,
  updatedTime = this.updatedTime,
)

fun SyncCreatePrisonerRestrictionRequest.mapSyncRequestToEntity() = PrisonerRestriction(
  prisonerRestrictionId = 0L,
  prisonerNumber = this.prisonerNumber,
  restrictionType = this.restrictionType,
  effectiveDate = this.effectiveDate,
  expiryDate = this.expiryDate,
  commentText = this.commentText,
  authorisedUsername = this.authorisedUsername,
  currentTerm = this.currentTerm,
  createdBy = this.createdBy,
  createdTime = this.createdTime,
)

fun Iterable<Pair<PrisonerRestrictionDetailsEntity, String>>.mapRestrictionsWithEnteredBy(
  prisonerNumber: String,
  enteredByMap: Map<String, String>,
): List<PrisonerRestrictionDetails> = this.map { (entity, authorisedUsername) ->
  PrisonerRestrictionDetails(
    prisonerRestrictionId = entity.prisonerRestrictionId,
    prisonerNumber = prisonerNumber,
    restrictionType = entity.restrictionType,
    restrictionTypeDescription = entity.restrictionTypeDescription,
    effectiveDate = entity.effectiveDate,
    expiryDate = entity.expiryDate,
    commentText = entity.commentText,
    authorisedUsername = authorisedUsername,
    authorisedByDisplayName = enteredByMap[authorisedUsername] ?: authorisedUsername,
    createdBy = entity.createdBy,
    createdTime = entity.createdTime,
    updatedBy = entity.updatedBy,
    currentTerm = entity.currentTerm,
    updatedTime = entity.updatedTime,
  )
}
