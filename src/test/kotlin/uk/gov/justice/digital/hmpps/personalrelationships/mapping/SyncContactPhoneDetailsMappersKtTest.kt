package uk.gov.justice.digital.hmpps.personalrelationships.mapping

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personalrelationships.entity.ContactPhoneDetailsEntity
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.ContactPhoneDetails
import java.time.LocalDateTime

class SyncContactPhoneDetailsMappersKtTest {

  @Test
  fun `should map all fields`() {
    assertThat(
      ContactPhoneDetailsEntity(
        contactPhoneId = 99,
        contactId = 45,
        phoneType = "HOME",
        phoneTypeDescription = "Home phone",
        phoneNumber = "123456789",
        extNumber = "987654321",
        createdBy = "CREATOR",
        createdTime = LocalDateTime.of(2024, 2, 3, 4, 5, 6),
        updatedBy = "AM",
        updatedTime = LocalDateTime.of(2026, 5, 4, 3, 2, 1),
      ).toModel(),
    ).isEqualTo(
      ContactPhoneDetails(
        contactPhoneId = 99,
        contactId = 45,
        phoneType = "HOME",
        phoneTypeDescription = "Home phone",
        phoneNumber = "123456789",
        extNumber = "987654321",
        createdBy = "CREATOR",
        createdTime = LocalDateTime.of(2024, 2, 3, 4, 5, 6),
        updatedBy = "AM",
        updatedTime = LocalDateTime.of(2026, 5, 4, 3, 2, 1),
      ),
    )
  }
}
