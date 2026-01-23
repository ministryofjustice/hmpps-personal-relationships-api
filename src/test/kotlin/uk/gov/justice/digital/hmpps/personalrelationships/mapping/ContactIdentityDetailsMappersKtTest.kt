package uk.gov.justice.digital.hmpps.personalrelationships.mapping

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personalrelationships.entity.ContactIdentityDetailsEntity
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.ContactIdentityDetails
import java.time.LocalDateTime

class ContactIdentityDetailsMappersKtTest {
  @Test
  fun `should map from entity to domain`() {
    assertThat(
      ContactIdentityDetailsEntity(
        contactIdentityId = 1,
        contactId = 1,
        identityType = "PASS",
        identityTypeDescription = "Passport",
        identityTypeIsActive = true,
        identityValue = "132456789",
        issuingAuthority = "UK",
        createdBy = "CRE",
        createdTime = LocalDateTime.of(2024, 2, 2, 2, 2, 2),
        updatedBy = "AMD",
        updatedTime = LocalDateTime.of(2024, 3, 3, 3, 3, 3),
      ).toModel(),
    ).isEqualTo(
      ContactIdentityDetails(
        contactIdentityId = 1,
        contactId = 1,
        identityType = "PASS",
        identityTypeDescription = "Passport",
        identityTypeIsActive = true,
        identityValue = "132456789",
        issuingAuthority = "UK",
        createdBy = "CRE",
        createdTime = LocalDateTime.of(2024, 2, 2, 2, 2, 2),
        updatedBy = "AMD",
        updatedTime = LocalDateTime.of(2024, 3, 3, 3, 3, 3),
      ),
    )
  }
}
