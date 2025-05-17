package uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request

import jakarta.validation.Validation
import jakarta.validation.ValidatorFactory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ContactSearchRequestTest {

  private val validatorFactory: ValidatorFactory = Validation.buildDefaultValidatorFactory()
  private val validator = validatorFactory.validator

  @Test
  fun `should fail validation if dateOfBirth is in the future`() {
    val futureDate = LocalDate.now().plusDays(1)
    val request = ContactSearchRequest(
      lastName = "Smith",
      firstName = null,
      middleNames = null,
      dateOfBirth = futureDate,
    )

    val violations = validator.validate(request)

    assertThat(violations).hasSize(1)
    assertThat(violations.first().message).isEqualTo("The date of birth must be in the past")
  }

  @Test
  fun `should fail validation if Last Name is not set`() {
    val request = ContactSearchRequest(
      lastName = "",
      firstName = null,
      middleNames = null,
      dateOfBirth = null,
    )

    val violations = validator.validate(request)

    assertThat(violations).hasSize(1)
    assertThat(violations.first().message).isEqualTo("must not be blank")
  }

  @Test
  fun `should pass validation if dateOfBirth is in the past`() {
    val pastDate = LocalDate.now().minusDays(1)
    val request = ContactSearchRequest(
      lastName = "Smith",
      firstName = null,
      middleNames = null,
      dateOfBirth = pastDate,
    )

    val violations = validator.validate(request)

    assertThat(violations).isEmpty()
  }
}
