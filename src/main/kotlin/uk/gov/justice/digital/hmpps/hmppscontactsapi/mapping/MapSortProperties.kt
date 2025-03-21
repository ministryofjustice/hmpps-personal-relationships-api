package uk.gov.justice.digital.hmpps.hmppscontactsapi.mapping

import jakarta.validation.ValidationException
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.ContactWithAddressEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.ContactSearchResultItem

/*
 * This ensures search works as expected for clients of the API when there might be a difference in property names
 * on API types to the database entity mappings. There is a matching test that ensures everything on the API is mappable.
 */
fun mapSortPropertiesOfContactSearch(property: String): String = when (property) {
  ContactSearchResultItem::id.name -> ContactWithAddressEntity::contactId.name
  ContactSearchResultItem::lastName.name -> ContactWithAddressEntity::lastName.name
  ContactSearchResultItem::firstName.name -> ContactWithAddressEntity::firstName.name
  ContactSearchResultItem::middleNames.name -> ContactWithAddressEntity::middleNames.name
  ContactSearchResultItem::dateOfBirth.name -> ContactWithAddressEntity::dateOfBirth.name
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
