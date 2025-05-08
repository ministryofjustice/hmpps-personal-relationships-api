package uk.gov.justice.digital.hmpps.hmppscontactsapi.service.migrate

class DuplicateRelationshipException(prisonerNumber: String, contactId: Long, relationshipToPrisonerCode: String) : RuntimeException("There is an existing relationship between prisoner $prisonerNumber and contact $contactId with type $relationshipToPrisonerCode")
