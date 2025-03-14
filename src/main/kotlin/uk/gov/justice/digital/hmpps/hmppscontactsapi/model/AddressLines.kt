package uk.gov.justice.digital.hmpps.hmppscontactsapi.model

interface AddressLines {
  val flat: String?
  val property: String?
  val street: String?
  val area: String?
  val cityCode: String?
  val countyCode: String?
  val postcode: String?
  val countryCode: String?
}
