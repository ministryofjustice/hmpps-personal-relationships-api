package uk.gov.justice.digital.hmpps.personalrelationships.service

import jakarta.validation.ValidationException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations.openMocks
import org.mockito.kotlin.whenever
import org.springframework.data.domain.Sort
import uk.gov.justice.digital.hmpps.personalrelationships.entity.ReferenceCodeEntity
import uk.gov.justice.digital.hmpps.personalrelationships.mapping.toModel
import uk.gov.justice.digital.hmpps.personalrelationships.model.ReferenceCodeGroup
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.ReferenceCode
import uk.gov.justice.digital.hmpps.personalrelationships.repository.ReferenceCodeRepository

class ReferenceCodeServiceTest {
  private val referenceCodeRepository: ReferenceCodeRepository = mock()
  private val service = ReferenceCodeService(referenceCodeRepository)

  @BeforeEach
  fun setUp() {
    openMocks(this)
  }

  @Test
  fun `Should return a list of references codes with active only`() {
    val groupCode = ReferenceCodeGroup.SOCIAL_RELATIONSHIP
    val listOfCodes = listOf(
      ReferenceCodeEntity(1L, groupCode, "FRIEND", "Friend", 0, true, "name"),
      ReferenceCodeEntity(2L, groupCode, "MOTHER", "Mother", 1, true, "name"),
      ReferenceCodeEntity(3L, groupCode, "FATHER", "Father", 2, true, "name"),
    )

    whenever(referenceCodeRepository.findAllByGroupCodeAndIsActiveEquals(groupCode, true, Sort.unsorted())).thenReturn(listOfCodes)

    assertThat(service.getReferenceDataByGroup(groupCode, Sort.unsorted(), true)).isEqualTo(listOfCodes.toModel())

    verify(referenceCodeRepository).findAllByGroupCodeAndIsActiveEquals(groupCode, true, Sort.unsorted())
  }

  @Test
  fun `Should return a list of references codes for all is active status`() {
    val groupCode = ReferenceCodeGroup.SOCIAL_RELATIONSHIP
    val listOfCodes = listOf(
      ReferenceCodeEntity(1L, groupCode, "FRIEND", "Friend", 0, true, "name"),
      ReferenceCodeEntity(2L, groupCode, "MOTHER", "Mother", 1, true, "name"),
      ReferenceCodeEntity(3L, groupCode, "FATHER", "Father", 2, true, "name"),
    )

    whenever(referenceCodeRepository.findAllByGroupCodeAndIsActiveEquals(groupCode, true, Sort.unsorted())).thenReturn(listOfCodes)

    assertThat(service.getReferenceDataByGroup(groupCode, Sort.unsorted(), true)).isEqualTo(listOfCodes.toModel())

    verify(referenceCodeRepository).findAllByGroupCodeAndIsActiveEquals(groupCode, true, Sort.unsorted())
  }

  @Test
  fun `Should return an empty list when no reference codes are matched active only`() {
    val groupCode = ReferenceCodeGroup.TITLE
    whenever(referenceCodeRepository.findAllByGroupCodeEquals(groupCode, Sort.unsorted())).thenReturn(emptyList())
    assertThat(service.getReferenceDataByGroup(groupCode, Sort.unsorted(), false)).isEmpty()
    verify(referenceCodeRepository).findAllByGroupCodeEquals(groupCode, Sort.unsorted())
  }

  @Test
  fun `Should return an empty list when no reference codes are matched inactive included`() {
    val groupCode = ReferenceCodeGroup.TITLE
    whenever(referenceCodeRepository.findAllByGroupCodeEquals(groupCode, Sort.unsorted())).thenReturn(emptyList())
    assertThat(service.getReferenceDataByGroup(groupCode, Sort.unsorted(), false)).isEmpty()
    verify(referenceCodeRepository).findAllByGroupCodeEquals(groupCode, Sort.unsorted())
  }

  @Test
  fun `should return reference code if valid`() {
    val entity = ReferenceCodeEntity(1L, ReferenceCodeGroup.SOCIAL_RELATIONSHIP, "FRIEND", "Friend", 0, true, "name")
    whenever(referenceCodeRepository.findByGroupCodeAndCode(ReferenceCodeGroup.SOCIAL_RELATIONSHIP, "FRIEND")).thenReturn(entity)

    val code = service.validateReferenceCode(ReferenceCodeGroup.SOCIAL_RELATIONSHIP, "FRIEND", allowInactive = false)

    assertThat(code).isEqualTo(
      ReferenceCode(1L, ReferenceCodeGroup.SOCIAL_RELATIONSHIP, "FRIEND", "Friend", 0, true),
    )
  }

  @Test
  fun `should return reference code if inactive but inactive are allowed`() {
    val entity = ReferenceCodeEntity(1L, ReferenceCodeGroup.SOCIAL_RELATIONSHIP, "FRIEND", "Friend", 0, false, "name")
    whenever(referenceCodeRepository.findByGroupCodeAndCode(ReferenceCodeGroup.SOCIAL_RELATIONSHIP, "FRIEND")).thenReturn(entity)

    val code = service.validateReferenceCode(ReferenceCodeGroup.SOCIAL_RELATIONSHIP, "FRIEND", allowInactive = true)

    assertThat(code).isEqualTo(
      ReferenceCode(1L, ReferenceCodeGroup.SOCIAL_RELATIONSHIP, "FRIEND", "Friend", 0, false),
    )
  }

  @Test
  fun `should throw exception if reference code is inactive and it's not allowed`() {
    val entity = ReferenceCodeEntity(1L, ReferenceCodeGroup.SOCIAL_RELATIONSHIP, "FRIEND", "Friend", 0, false, "name")
    whenever(referenceCodeRepository.findByGroupCodeAndCode(ReferenceCodeGroup.SOCIAL_RELATIONSHIP, "FRIEND")).thenReturn(entity)

    val exception = assertThrows<ValidationException> {
      service.validateReferenceCode(ReferenceCodeGroup.SOCIAL_RELATIONSHIP, "FRIEND", allowInactive = false)
    }

    assertThat(exception.message).isEqualTo("Unsupported social relationship (FRIEND). This code is no longer active.")
  }

  @Test
  fun `should throw exception if reference code is not found`() {
    whenever(referenceCodeRepository.findByGroupCodeAndCode(ReferenceCodeGroup.SOCIAL_RELATIONSHIP, "FRIEND")).thenReturn(null)

    val exception = assertThrows<ValidationException> {
      service.validateReferenceCode(ReferenceCodeGroup.SOCIAL_RELATIONSHIP, "FRIEND", allowInactive = true)
    }

    assertThat(exception.message).isEqualTo("Unsupported social relationship (FRIEND)")
  }
}
