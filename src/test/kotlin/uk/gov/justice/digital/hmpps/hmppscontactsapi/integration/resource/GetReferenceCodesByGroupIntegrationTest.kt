package uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Sort
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.SecureAPIIntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.helper.hasSize
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.ReferenceCodeGroup
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.ReferenceCodeRepository
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

class GetReferenceCodesByGroupIntegrationTest : SecureAPIIntegrationTestBase() {
  @Autowired
  private lateinit var referenceCodeRepository: ReferenceCodeRepository

  override val allowedRoles: Set<String> = setOf("ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__RW", "ROLE_CONTACTS__R")

  override fun baseRequestBuilder(): WebTestClient.RequestHeadersSpec<*> = webTestClient.get()
    .uri("/reference-codes/group/PHONE_TYPE")

  @Test
  fun `should return bad request if no matching code found`() {
    val error = webTestClient.get()
      .uri("/reference-codes/group/FOO")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CONTACTS_ADMIN")))
      .exchange()
      .expectStatus().isBadRequest
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody!!

    assertThat(error.developerMessage).startsWith(""""FOO" is not a valid reference code group. Valid groups are DOMESTIC_STS, OFFICIAL_RELATIONSHIP""")
  }

  @ParameterizedTest
  @ValueSource(strings = ["ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__R", "ROLE_CONTACTS__RW"])
  fun `should return a list of relationship type reference codes`(role: String) {
    val groupCode = ReferenceCodeGroup.SOCIAL_RELATIONSHIP
    referenceCodeRepository.findAllByGroupCodeEquals(groupCode, Sort.unsorted()) hasSize 36

    val listOfCodes = testAPIClient.getReferenceCodes(groupCode, role = role)

    assertThat(listOfCodes).hasSize(36)
    assertThat(listOfCodes)
      .extracting("code")
      .containsAll(listOf("MOT", "FA"))
    assertThat(listOfCodes).extracting("code").doesNotContainAnyElementsOf(listOf("WORK", "MOB"))
  }

  @Test
  fun `should return a list of phone type reference codes`() {
    val groupCode = ReferenceCodeGroup.PHONE_TYPE
    referenceCodeRepository.findAllByGroupCodeEquals(groupCode, Sort.unsorted()) hasSize 7

    val listOfCodes = testAPIClient.getReferenceCodes(groupCode, role = "ROLE_CONTACTS_ADMIN")

    assertThat(listOfCodes).extracting("code").containsExactlyInAnyOrder(
      "HOME",
      "BUS",
      "FAX",
      "ALTB",
      "ALTH",
      "MOB",
      "VISIT",
    )
  }

  @Test
  fun `should return a list of domestic status type reference codes`() {
    val groupCode = ReferenceCodeGroup.DOMESTIC_STS
    referenceCodeRepository.findAllByGroupCodeEquals(groupCode, Sort.unsorted()) hasSize 7

    val listOfCodes = testAPIClient.getReferenceCodes(groupCode, role = "ROLE_CONTACTS_ADMIN")

    assertThat(listOfCodes).hasSize(7)
    assertThat(listOfCodes)
      .extracting("code")
      .containsAll(listOf("S", "C", "M", "D", "P", "W", "N"))
  }

  @Test
  fun `should be able to sort reference codes`() {
    val groupCode = ReferenceCodeGroup.DOMESTIC_STS
    val listOfCodesInDisplayOrder = testAPIClient.getReferenceCodes(
      groupCode,
      "displayOrder",
      role = "ROLE_CONTACTS_ADMIN",
    )
    assertThat(listOfCodesInDisplayOrder)
      .extracting("code")
      .isEqualTo(listOf("S", "C", "M", "D", "P", "W", "N"))

    val listOfCodesInCodeOrder = testAPIClient.getReferenceCodes(groupCode, "code", role = "ROLE_CONTACTS_ADMIN")
    assertThat(listOfCodesInCodeOrder)
      .extracting("code")
      .isEqualTo(listOf("C", "D", "M", "N", "P", "S", "W"))
  }

  @Test
  fun `should not return inactive codes by default`() {
    val groupCode = ReferenceCodeGroup.TEST_TYPE
    assertThat(testAPIClient.getReferenceCodes(groupCode, activeOnly = null, role = "ROLE_CONTACTS_ADMIN"))
      .extracting("code")
      .isEqualTo(listOf("ACTIVE"))
  }

  @Test
  fun `should not return inactive codes if specifically request not to`() {
    val groupCode = ReferenceCodeGroup.TEST_TYPE
    assertThat(testAPIClient.getReferenceCodes(groupCode, activeOnly = true, role = "ROLE_CONTACTS_ADMIN"))
      .extracting("code")
      .isEqualTo(listOf("ACTIVE"))
  }

  @Test
  fun `should return inactive codes if requested`() {
    val groupCode = ReferenceCodeGroup.TEST_TYPE
    assertThat(testAPIClient.getReferenceCodes(groupCode, activeOnly = false, role = "ROLE_CONTACTS_ADMIN"))
      .extracting("code")
      .isEqualTo(listOf("ACTIVE", "INACTIVE"))
  }
}
