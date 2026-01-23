package uk.gov.justice.digital.hmpps.personalrelationships.mapping

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personalrelationships.entity.ContactAddressDetailsEntity
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.ContactAddressDetails
import java.time.LocalDate
import java.time.LocalDateTime

class ContactAddressDetailsMappersKtTest {

  @Test
  fun `should convert entity to model with all fields`() {
    assertThat(
      ContactAddressDetailsEntity(
        contactAddressId = 0,
        contactId = 0,
        addressType = "HOME",
        addressTypeDescription = "Home address",
        primaryAddress = true,
        flat = "Flat",
        property = "Property",
        street = "Street",
        area = "Area",
        cityCode = "CIT",
        cityDescription = "City",
        countyCode = "COUNT",
        countyDescription = "County",
        postCode = "POST CODE",
        countryCode = "ENG",
        countryDescription = "England",
        verified = true,
        verifiedBy = "VERIFIED",
        verifiedTime = LocalDateTime.of(2021, 1, 1, 11, 15, 0),
        mailFlag = true,
        startDate = LocalDate.of(2020, 2, 3),
        endDate = LocalDate.of(2050, 4, 5),
        noFixedAddress = true,
        comments = "Some comments",
        createdBy = "USER1",
        createdTime = LocalDateTime.of(2023, 2, 3, 11, 15, 15),
        updatedBy = "AMEND_USER",
        updatedTime = LocalDateTime.of(2024, 5, 6, 12, 30, 30),
      ).toModel(emptyList()),
    ).isEqualTo(
      ContactAddressDetails(
        contactAddressId = 0,
        contactId = 0,
        addressType = "HOME",
        addressTypeDescription = "Home address",
        primaryAddress = true,
        flat = "Flat",
        property = "Property",
        street = "Street",
        area = "Area",
        cityCode = "CIT",
        cityDescription = "City",
        countyCode = "COUNT",
        countyDescription = "County",
        postcode = "POST CODE",
        countryCode = "ENG",
        countryDescription = "England",
        verified = true,
        verifiedBy = "VERIFIED",
        verifiedTime = LocalDateTime.of(2021, 1, 1, 11, 15, 0),
        mailFlag = true,
        startDate = LocalDate.of(2020, 2, 3),
        endDate = LocalDate.of(2050, 4, 5),
        noFixedAddress = true,
        comments = "Some comments",
        phoneNumbers = emptyList(),
        createdBy = "USER1",
        createdTime = LocalDateTime.of(2023, 2, 3, 11, 15, 15),
        updatedBy = "AMEND_USER",
        updatedTime = LocalDateTime.of(2024, 5, 6, 12, 30, 30),
      ),
    )
  }
}
