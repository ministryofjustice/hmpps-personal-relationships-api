package uk.gov.justice.digital.hmpps.hmppscontactsapi.model.internal

/**
 * Response class for changed restrictions operations.
 *
 * @property hasChanged Indicates if any restrictions has changed
 */
data class ChangedRestrictionsResponse(
  val hasChanged: Boolean,
  val createdRestrictions: List<Long>,
  val deletedRestrictions: List<Long>,
)
