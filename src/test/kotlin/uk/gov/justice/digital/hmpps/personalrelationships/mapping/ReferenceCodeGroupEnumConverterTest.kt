package uk.gov.justice.digital.hmpps.personalrelationships.mapping

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import uk.gov.justice.digital.hmpps.personalrelationships.exception.InvalidReferenceCodeGroupException
import uk.gov.justice.digital.hmpps.personalrelationships.model.ReferenceCodeGroup

class ReferenceCodeGroupEnumConverterTest {

  @ParameterizedTest
  @EnumSource(ReferenceCodeGroup::class)
  fun `should parse valid types`(code: ReferenceCodeGroup) {
    assertThat(ReferenceCodeGroupEnumConverter().convert(code.name)).isEqualTo(code)
  }

  @Test
  fun `should throw custom exception on invalid type`() {
    val exception = assertThrows<InvalidReferenceCodeGroupException> {
      ReferenceCodeGroupEnumConverter().convert("FOO")
    }
    assertThat(exception.message).startsWith("\"FOO\" is not a valid reference code group. Valid groups are DOMESTIC_STS, OFFICIAL_RELATIONSHIP")
    assertThat(exception.message).doesNotContain(ReferenceCodeGroup.TEST_TYPE.name)
  }
}
