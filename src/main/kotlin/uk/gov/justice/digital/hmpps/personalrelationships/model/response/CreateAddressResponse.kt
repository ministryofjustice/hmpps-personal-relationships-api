package uk.gov.justice.digital.hmpps.personalrelationships.model.response

data class CreateAddressResponse(val created: ContactAddressResponse, val otherUpdatedAddressIds: Set<Long>)
