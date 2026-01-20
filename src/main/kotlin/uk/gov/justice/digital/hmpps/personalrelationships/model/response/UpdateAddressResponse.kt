package uk.gov.justice.digital.hmpps.personalrelationships.model.response

data class UpdateAddressResponse(val updated: ContactAddressResponse, val otherUpdatedAddressIds: Set<Long>)
