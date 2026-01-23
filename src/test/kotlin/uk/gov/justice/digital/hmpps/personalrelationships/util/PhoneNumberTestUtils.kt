package uk.gov.justice.digital.hmpps.personalrelationships.util

import org.junit.jupiter.params.provider.Arguments

class PhoneNumberTestUtils {

  companion object {
    @JvmStatic
    fun invalidPhoneNumbers(): List<Arguments> = listOf(
      "!",
      "\"",
      "£",
      "$",
      "%",
      "^",
      "&",
      "*",
      "_",
      "-",
      "=",
      // + not allowed unless at start
      "0+",
      ":",
      ";",
      "[",
      "]",
      "{",
      "}",
      "@",
      "#",
      "~",
      "/",
      "\\",
      "'",
    ).map { Arguments.of(it) }
  }
}
