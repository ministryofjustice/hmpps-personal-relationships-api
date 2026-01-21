package uk.gov.justice.digital.hmpps.personalrelationships.exception

class RelationshipCannotBeRemovedDueToDependencyException(prisonerContactId: Long) : RuntimeException("Cannot delete relationship ($prisonerContactId) as there are dependent entities")
