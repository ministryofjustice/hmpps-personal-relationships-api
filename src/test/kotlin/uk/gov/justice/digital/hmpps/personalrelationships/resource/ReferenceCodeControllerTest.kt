package uk.gov.justice.digital.hmpps.personalrelationships.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.Sort
import uk.gov.justice.digital.hmpps.personalrelationships.model.ReferenceCodeGroup
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.ReferenceCode
import uk.gov.justice.digital.hmpps.personalrelationships.service.ReferenceCodeService

class ReferenceCodeControllerTest {

  private val service: ReferenceCodeService = mock()
  private val controller = ReferenceCodeController(service)

  @ParameterizedTest
  @CsvSource(value = ["true", "false"])
  fun `should get the reference codes with supplied options`(activeOnly: Boolean) {
    val expected = listOf(
      ReferenceCode(1L, ReferenceCodeGroup.SOCIAL_RELATIONSHIP, "FRIEND", "Friend", 0, true),
      ReferenceCode(2L, ReferenceCodeGroup.SOCIAL_RELATIONSHIP, "MOTHER", "Mother", 1, true),
      ReferenceCode(3L, ReferenceCodeGroup.SOCIAL_RELATIONSHIP, "FATHER", "Father", 2, true),
    )
    whenever(service.getReferenceDataByGroup(ReferenceCodeGroup.SOCIAL_RELATIONSHIP, Sort.unsorted(), activeOnly)).thenReturn(expected)

    val response = controller.getReferenceDataByGroup(ReferenceCodeGroup.SOCIAL_RELATIONSHIP, Sort.unsorted(), activeOnly)

    assertThat(response).isEqualTo(expected)
    verify(service).getReferenceDataByGroup(ReferenceCodeGroup.SOCIAL_RELATIONSHIP, Sort.unsorted(), activeOnly)
  }

  @Test
  fun `should propagate exceptions`() {
    whenever(service.getReferenceDataByGroup(ReferenceCodeGroup.SOCIAL_RELATIONSHIP, Sort.unsorted(), true)).thenThrow(RuntimeException("Bang!"))

    assertThrows<RuntimeException>("Bang!") {
      controller.getReferenceDataByGroup(ReferenceCodeGroup.SOCIAL_RELATIONSHIP, Sort.unsorted(), true)
    }
  }
}
