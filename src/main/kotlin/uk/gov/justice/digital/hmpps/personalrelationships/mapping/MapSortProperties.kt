package uk.gov.justice.digital.hmpps.personalrelationships.mapping

import jakarta.validation.ValidationException
import uk.gov.justice.digital.hmpps.personalrelationships.entity.ContactWithAddressEntity
import uk.gov.justice.digital.hmpps.personalrelationships.entity.PrisonerContactSummaryEntity
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.ContactSearchResultItem
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.PrisonerContactSummary

/**
 * These methods ensure search works as expected for clients of the API when there might be a difference in property names
 * on API types to the database entity mappings. There is a matching test that ensures everything on the relevant APIs
 * are mappable.
 */

fun mapSortPropertiesOfContactSearch(property: String): String = when (property) {
  ContactSearchResultItem::id.name -> ContactWithAddressEntity::contactId.name
  ContactSearchResultItem::lastName.name -> ContactWithAddressEntity::lastName.name
  ContactSearchResultItem::firstName.name -> ContactWithAddressEntity::firstName.name
  ContactSearchResultItem::middleNames.name -> ContactWithAddressEntity::middleNames.name
  ContactSearchResultItem::dateOfBirth.name -> ContactWithAddressEntity::dateOfBirth.name
  ContactSearchResultItem::deceasedDate.name -> ContactWithAddressEntity::deceasedDate.name
  ContactSearchResultItem::createdBy.name -> ContactWithAddressEntity::createdBy.name
  ContactSearchResultItem::createdTime.name -> ContactWithAddressEntity::createdTime.name
  ContactSearchResultItem::flat.name -> ContactWithAddressEntity::flat.name
  ContactSearchResultItem::property.name -> ContactWithAddressEntity::property.name
  ContactSearchResultItem::street.name -> ContactWithAddressEntity::street.name
  ContactSearchResultItem::area.name -> ContactWithAddressEntity::area.name
  ContactSearchResultItem::cityCode.name -> ContactWithAddressEntity::cityCode.name
  ContactSearchResultItem::cityDescription.name -> ContactWithAddressEntity::cityDescription.name
  ContactSearchResultItem::countyCode.name -> ContactWithAddressEntity::countyCode.name
  ContactSearchResultItem::countyDescription.name -> ContactWithAddressEntity::countyDescription.name
  ContactSearchResultItem::postcode.name -> ContactWithAddressEntity::postCode.name
  ContactSearchResultItem::countryCode.name -> ContactWithAddressEntity::countryCode.name
  ContactSearchResultItem::countryDescription.name -> ContactWithAddressEntity::countryDescription.name
  ContactSearchResultItem::mailAddress.name -> ContactWithAddressEntity::mailFlag.name
  ContactSearchResultItem::startDate.name -> ContactWithAddressEntity::startDate.name
  ContactSearchResultItem::endDate.name -> ContactWithAddressEntity::endDate.name
  ContactSearchResultItem::noFixedAddress.name -> ContactWithAddressEntity::noFixedAddress.name
  ContactSearchResultItem::comments.name -> ContactWithAddressEntity::comments.name
  else -> throw ValidationException("Unable to sort on $property")
}

fun mapSortPropertiesOfPrisonerContactSearch(property: String): String = when (property) {
  PrisonerContactSummary::prisonerContactId.name -> PrisonerContactSummaryEntity::prisonerContactId.name
  PrisonerContactSummary::contactId.name -> PrisonerContactSummaryEntity::contactId.name
  PrisonerContactSummary::prisonerNumber.name -> PrisonerContactSummaryEntity::prisonerNumber.name
  PrisonerContactSummary::titleCode.name -> PrisonerContactSummaryEntity::title.name
  PrisonerContactSummary::titleDescription.name -> PrisonerContactSummaryEntity::titleDescription.name
  PrisonerContactSummary::lastName.name -> PrisonerContactSummaryEntity::lastName.name
  PrisonerContactSummary::firstName.name -> PrisonerContactSummaryEntity::firstName.name
  PrisonerContactSummary::middleNames.name -> PrisonerContactSummaryEntity::middleNames.name
  PrisonerContactSummary::dateOfBirth.name -> PrisonerContactSummaryEntity::dateOfBirth.name
  PrisonerContactSummary::deceasedDate.name -> PrisonerContactSummaryEntity::deceasedDate.name
  PrisonerContactSummary::relationshipTypeCode.name -> PrisonerContactSummaryEntity::relationshipType.name
  PrisonerContactSummary::relationshipTypeDescription.name -> PrisonerContactSummaryEntity::relationshipTypeDescription.name
  PrisonerContactSummary::relationshipToPrisonerCode.name -> PrisonerContactSummaryEntity::relationshipToPrisoner.name
  PrisonerContactSummary::relationshipToPrisonerDescription.name -> PrisonerContactSummaryEntity::relationshipToPrisonerDescription.name
  PrisonerContactSummary::flat.name -> PrisonerContactSummaryEntity::flat.name
  PrisonerContactSummary::property.name -> PrisonerContactSummaryEntity::property.name
  PrisonerContactSummary::street.name -> PrisonerContactSummaryEntity::street.name
  PrisonerContactSummary::area.name -> PrisonerContactSummaryEntity::area.name
  PrisonerContactSummary::cityCode.name -> PrisonerContactSummaryEntity::cityCode.name
  PrisonerContactSummary::cityDescription.name -> PrisonerContactSummaryEntity::cityDescription.name
  PrisonerContactSummary::countyCode.name -> PrisonerContactSummaryEntity::countyCode.name
  PrisonerContactSummary::countyDescription.name -> PrisonerContactSummaryEntity::countyDescription.name
  PrisonerContactSummary::postcode.name -> PrisonerContactSummaryEntity::postCode.name
  PrisonerContactSummary::countryCode.name -> PrisonerContactSummaryEntity::countryCode.name
  PrisonerContactSummary::countryDescription.name -> PrisonerContactSummaryEntity::countryDescription.name
  PrisonerContactSummary::noFixedAddress.name -> PrisonerContactSummaryEntity::noFixedAddress.name
  PrisonerContactSummary::primaryAddress.name -> PrisonerContactSummaryEntity::primaryAddress.name
  PrisonerContactSummary::mailAddress.name -> PrisonerContactSummaryEntity::mailFlag.name
  PrisonerContactSummary::phoneType.name -> PrisonerContactSummaryEntity::phoneType.name
  PrisonerContactSummary::phoneTypeDescription.name -> PrisonerContactSummaryEntity::phoneTypeDescription.name
  PrisonerContactSummary::phoneNumber.name -> PrisonerContactSummaryEntity::phoneNumber.name
  PrisonerContactSummary::extNumber.name -> PrisonerContactSummaryEntity::extNumber.name
  PrisonerContactSummary::isApprovedVisitor.name -> PrisonerContactSummaryEntity::approvedVisitor.name
  PrisonerContactSummary::isNextOfKin.name -> PrisonerContactSummaryEntity::nextOfKin.name
  PrisonerContactSummary::isEmergencyContact.name -> PrisonerContactSummaryEntity::emergencyContact.name
  PrisonerContactSummary::isRelationshipActive.name -> PrisonerContactSummaryEntity::active.name
  PrisonerContactSummary::currentTerm.name -> PrisonerContactSummaryEntity::currentTerm.name
  PrisonerContactSummary::comments.name -> PrisonerContactSummaryEntity::comments.name
  PrisonerContactSummary::isStaff.name -> PrisonerContactSummaryEntity::staffFlag.name
  else -> throw ValidationException("Unable to sort on $property")
}
