package uk.gov.justice.digital.hmpps.hmppscontactsapi.exception

class RelationshipCannotBeRemovedDueToDependencyException(prisonerContactId: Long) : RuntimeException("Cannot delete relationship ($prisonerContactId) as there are dependent entities")
