package uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.PostgresIntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.Language
import uk.gov.justice.digital.hmpps.hmppscontactsapi.util.StubUser

class LanguageIntegrationTest : PostgresIntegrationTestBase() {

  companion object {
    private const val GET_LANGUAGE_REFERENCE_DATA = "/language-reference"
  }

  @BeforeEach
  fun setUp() {
    setCurrentUser(StubUser.READ_ONLY_USER)
  }

  @Nested
  inner class GetLanguageByLanguageId {

    @Test
    fun `should return unauthorized if no token`() {
      webTestClient.get()
        .uri("/language-reference/001")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `should return forbidden if no role`() {
      setCurrentUser(StubUser.USER_WITH_NO_ROLES)
      webTestClient.get()
        .uri("$GET_LANGUAGE_REFERENCE_DATA/001")
        .headers(setAuthorisationUsingCurrentUser())
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return forbidden if wrong role`() {
      setCurrentUser(StubUser.USER_WITH_WRONG_ROLES)
      webTestClient.get()
        .uri("$GET_LANGUAGE_REFERENCE_DATA/001")
        .headers(setAuthorisationUsingCurrentUser())
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return not found if no language found`() {
      webTestClient.get()
        .uri("$GET_LANGUAGE_REFERENCE_DATA/999")
        .headers(setAuthorisationUsingCurrentUser())
        .exchange()
        .expectStatus()
        .isNotFound
    }

    @Test
    fun `should return language reference data when using the id`() {
      val languageReferences = webTestClient.getLanguageReferenceData(
        "$GET_LANGUAGE_REFERENCE_DATA/226",
      )

      assertThat(languageReferences).extracting("nomisDescription").contains("Zhuang; Chuang")
      assertThat(languageReferences).extracting("nomisCode").contains("ZHA")
      assertThat(languageReferences).hasSize(1)
    }

    private fun WebTestClient.getLanguageReferenceData(url: String): MutableList<Language> = get()
      .uri(url)
      .headers(setAuthorisationUsingCurrentUser())
      .exchange()
      .expectStatus()
      .isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBodyList(Language::class.java)
      .returnResult().responseBody!!
  }

  @Nested
  inner class GetLanguageByNomisCode {

    @Test
    fun `should return unauthorized if no token`() {
      webTestClient.get()
        .uri("/language-reference/nomis-code/YU")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `should return forbidden if no role`() {
      setCurrentUser(StubUser.USER_WITH_NO_ROLES)
      webTestClient.get()
        .uri("$GET_LANGUAGE_REFERENCE_DATA/nomis-code/YU")
        .headers(setAuthorisationUsingCurrentUser())
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return forbidden if wrong role`() {
      setCurrentUser(StubUser.USER_WITH_WRONG_ROLES)
      webTestClient.get()
        .uri("$GET_LANGUAGE_REFERENCE_DATA/nomis-code/YU")
        .headers(setAuthorisationUsingCurrentUser())
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return not found if no language found`() {
      webTestClient.get()
        .uri("$GET_LANGUAGE_REFERENCE_DATA/nomis-code/YYWS")
        .headers(setAuthorisationUsingCurrentUser())
        .exchange()
        .expectStatus()
        .isNotFound
    }

    @Test
    fun `should return language reference data when using the nomis code`() {
      val languageReferences = webTestClient.getLanguageReferenceData(
        "$GET_LANGUAGE_REFERENCE_DATA/nomis-code/CHU",
      )

      assertThat(languageReferences).extracting("nomisDescription").contains("Church Slavic; Old Slavonic; Church Slavonic; Old Bulgarian; Old Church Slavonic")
      assertThat(languageReferences).extracting("nomisCode").contains("CHU")
      assertThat(languageReferences).hasSize(1)
    }

    private fun WebTestClient.getLanguageReferenceData(url: String): MutableList<Language> = get()
      .uri(url)
      .headers(setAuthorisationUsingCurrentUser())
      .exchange()
      .expectStatus()
      .isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBodyList(Language::class.java)
      .returnResult().responseBody!!
  }

  @Nested
  inner class GetLanguageByIsoAlpha2 {

    @Test
    fun `should return unauthorized if no token`() {
      webTestClient.get()
        .uri("/language-reference/iso-alpha2/b6")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `should return forbidden if no role`() {
      setCurrentUser(StubUser.USER_WITH_NO_ROLES)
      webTestClient.get()
        .uri("$GET_LANGUAGE_REFERENCE_DATA/iso-alpha2/b6")
        .headers(setAuthorisationUsingCurrentUser())
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return forbidden if wrong role`() {
      setCurrentUser(StubUser.USER_WITH_WRONG_ROLES)
      webTestClient.get()
        .uri("$GET_LANGUAGE_REFERENCE_DATA/iso-alpha2/b6")
        .headers(setAuthorisationUsingCurrentUser())
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return not found if no language found`() {
      webTestClient.get()
        .uri("$GET_LANGUAGE_REFERENCE_DATA/iso-alpha2/z6j")
        .headers(setAuthorisationUsingCurrentUser())
        .exchange()
        .expectStatus()
        .isNotFound
    }

    @Test
    fun `should return language reference data when using the iso alpha code 2`() {
      val languageReferences = webTestClient.getLanguageReferenceData(
        "$GET_LANGUAGE_REFERENCE_DATA/iso-alpha2/za",
      )

      assertThat(languageReferences).extracting("nomisDescription").contains("Zhuang; Chuang")
      assertThat(languageReferences).extracting("nomisCode").contains("ZHA")
      assertThat(languageReferences).hasSize(1)
    }

    private fun WebTestClient.getLanguageReferenceData(url: String): MutableList<Language> = get()
      .uri(url)
      .headers(setAuthorisationUsingCurrentUser())
      .exchange()
      .expectStatus()
      .isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBodyList(Language::class.java)
      .returnResult().responseBody!!
  }

  @Nested
  inner class GetLanguageByIsoAlpha3 {

    @Test
    fun `should return unauthorized if no token`() {
      webTestClient.get()
        .uri("/language-reference/iso-alpha3/bn6")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `should return forbidden if no role`() {
      setCurrentUser(StubUser.USER_WITH_NO_ROLES)
      webTestClient.get()
        .uri("$GET_LANGUAGE_REFERENCE_DATA/iso-alpha3/bn6")
        .headers(setAuthorisationUsingCurrentUser())
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return forbidden if wrong role`() {
      setCurrentUser(StubUser.USER_WITH_WRONG_ROLES)
      webTestClient.get()
        .uri("$GET_LANGUAGE_REFERENCE_DATA/iso-alpha3/bn6")
        .headers(setAuthorisationUsingCurrentUser())
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return not found if no language found`() {
      webTestClient.get()
        .uri("$GET_LANGUAGE_REFERENCE_DATA/iso-alpha3/z6y")
        .headers(setAuthorisationUsingCurrentUser())
        .exchange()
        .expectStatus()
        .isNotFound
    }

    @Test
    fun `should return language reference data when using the iso alpha code 3`() {
      val languageReferences = webTestClient.getLanguageReferenceData(
        "$GET_LANGUAGE_REFERENCE_DATA/iso-alpha3/zha",
      )

      assertThat(languageReferences).extracting("nomisDescription").contains("Zhuang; Chuang")
      assertThat(languageReferences).extracting("nomisCode").contains("ZHA")
      assertThat(languageReferences).hasSize(1)
    }

    private fun WebTestClient.getLanguageReferenceData(url: String): MutableList<Language> = get()
      .uri(url)
      .headers(setAuthorisationUsingCurrentUser())
      .exchange()
      .expectStatus()
      .isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBodyList(Language::class.java)
      .returnResult().responseBody!!
  }

  @Nested
  inner class GetAllLanguages {

    @Test
    fun `should return unauthorized if no token`() {
      webTestClient.get()
        .uri(GET_LANGUAGE_REFERENCE_DATA)
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `should return forbidden if no role`() {
      setCurrentUser(StubUser.USER_WITH_NO_ROLES)
      webTestClient.get()
        .uri(GET_LANGUAGE_REFERENCE_DATA)
        .headers(setAuthorisationUsingCurrentUser())
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return forbidden if wrong role`() {
      setCurrentUser(StubUser.USER_WITH_WRONG_ROLES)
      webTestClient.get()
        .uri(GET_LANGUAGE_REFERENCE_DATA)
        .headers(setAuthorisationUsingCurrentUser())
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return not found if no language found`() {
      webTestClient.get()
        .uri("$GET_LANGUAGE_REFERENCE_DATA/2999")
        .headers(setAuthorisationUsingCurrentUser())
        .exchange()
        .expectStatus()
        .isNotFound
    }

    @Test
    fun `should return language reference data when get all countries`() {
      val languageReferences = webTestClient.getLanguageReferenceData(GET_LANGUAGE_REFERENCE_DATA)
      val language = Language(
        languageId = 226,
        nomisCode = "ZHA",
        nomisDescription = "Zhuang; Chuang",
        isoAlpha2 = "za",
        isoAlpha3 = "zha",
        isoLanguageDesc = "Zhuang; Chuang",
        displaySequence = 99,
      )
      assertThat(languageReferences.contains(language))
      assertThat(languageReferences).hasSizeGreaterThan(10)
    }

    private fun WebTestClient.getLanguageReferenceData(url: String): MutableList<Language> = get()
      .uri(url)
      .headers(setAuthorisationUsingCurrentUser())
      .exchange()
      .expectStatus()
      .isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBodyList(Language::class.java)
      .returnResult().responseBody!!
  }
}
