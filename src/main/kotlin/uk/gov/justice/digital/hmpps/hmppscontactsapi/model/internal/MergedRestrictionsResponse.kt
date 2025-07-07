package uk.gov.justice.digital.hmpps.hmppscontactsapi.model.internal

/**
 * Response class for merged restrictions operations.
 *
 * @property hasChanged Indicates if any restrictions has changed
 */
data class MergedRestrictionsResponse(
  val hasChanged: Boolean,
  val createdRestrictions: List<Long> = emptyList(),
  val deletedRestrictions: List<Long> = emptyList(),
)
