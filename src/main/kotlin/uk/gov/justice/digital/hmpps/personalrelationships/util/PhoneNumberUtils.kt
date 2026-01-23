package uk.gov.justice.digital.hmpps.personalrelationships.util

import jakarta.validation.ValidationException

fun validatePhoneNumber(phoneNumber: String) {
  if (!phoneNumber.matches(Regex("\\+?[\\d\\s()]+"))) {
    throw ValidationException("Phone number invalid, it can only contain numbers, () and whitespace with an optional + at the start")
  }
}
