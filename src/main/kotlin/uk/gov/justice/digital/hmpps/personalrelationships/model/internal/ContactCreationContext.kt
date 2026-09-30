package uk.gov.justice.digital.hmpps.personalrelationships.model.internal

import uk.gov.justice.digital.hmpps.personalrelationships.client.organisationsapi.model.OrganisationSummary
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.ReferenceCode

data class ContactCreationContext(
  val title: ReferenceCode?,
  val gender: ReferenceCode?,
  val language: ReferenceCode?,
  val domesticStatus: ReferenceCode?,
  val relationship: ContactRelationshipReferenceData?,
  val organisations: Map<Long, OrganisationSummary>,
)

data class ContactRelationshipReferenceData(
  val relationshipType: ReferenceCode,
  val relationshipToPrisoner: ReferenceCode,
)
