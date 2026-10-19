package uk.gov.justice.digital.hmpps.personalrelationships.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.test.context.jdbc.Sql
import org.springframework.web.util.UriComponentsBuilder
import uk.gov.justice.digital.hmpps.personalrelationships.integration.PostgresIntegrationTestBase
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.ContactSearchResultItem
import uk.gov.justice.digital.hmpps.personalrelationships.util.StubUser

@Sql("classpath:search-cap.tests/data-for-search-cap-test.sql")
@Sql(
  scripts = ["classpath:search-cap.tests/cleanup-search-cap-test.sql"],
  executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD,
)
class SearchContactsCapIntegrationTest : PostgresIntegrationTestBase() {

  @BeforeEach
  fun setUp() {
    setCurrentUser(StubUser.READ_ONLY_USER)
  }

  @ParameterizedTest
  @ValueSource(booleans = [true, false])
  fun `should cap current partial name matches and paginate within the cap`(explicitPreviousNames: Boolean) {
    val contacts = (0..3).flatMap { pageNumber ->
      val uri = searchUrl(previousNames = if (explicitPreviousNames) false else null)
        .queryParam("page", pageNumber)
        .queryParam("size", "500")
        .queryParam("sort", "firstName,desc")
        .build()
        .toUri()

      val body = testAPIClient.getSearchContactResults(uri)!!

      assertThat(body.page.totalElements).isEqualTo(2000)
      assertThat(body.page.totalPages).isEqualTo(4)
      assertThat(body.page.number).isEqualTo(pageNumber.toLong())
      assertThat(body.content).hasSize(500)
      body.content
    }

    assertThat(contacts.map { it.id }).doesNotHaveDuplicates()
    assertThat(contacts).isSortedAccordingTo(compareByDescending<ContactSearchResultItem> { it.firstName }.thenBy { it.id })

    val beyondCap = searchUrl()
      .queryParam("page", "4")
      .queryParam("size", "500")
      .build()
      .toUri()
    assertThat(testAPIClient.getSearchContactResults(beyondCap)!!.content).isEmpty()
  }

  @Test
  fun `should cap current partial name matches with the default sort`() {
    val body = testAPIClient.getSearchContactResults(searchUrl().build().toUri())!!

    assertThat(body.page.totalElements).isEqualTo(2000)
    assertThat(body.content).hasSize(10)
    assertThat(body.content.map { it.id }).isSorted()
  }

  @ParameterizedTest
  @CsvSource("eXa,DdLeX,334", ",DdLeX,667", "eXa,,1002")
  fun `should apply all supplied name filters before limiting matches`(firstName: String?, middleNames: String?, expectedCount: Long) {
    val uri = searchUrl()
      .apply {
        firstName?.let { queryParam("firstName", it) }
        middleNames?.let { queryParam("middleNames", it) }
      }
      .queryParam("sort", "lastName,asc")
      .build()
      .toUri()

    val body = testAPIClient.getSearchContactResults(uri)!!

    assertThat(body.page.totalElements).isEqualTo(expectedCount)
    assertThat(body.content).hasSize(10)
    body.content.forEach { contact ->
      firstName?.let { assertThat(contact.firstName).containsIgnoringCase(it) }
      middleNames?.let { assertThat(contact.middleNames).containsIgnoringCase(it) }
    }
  }

  @Test
  fun `should not cap partial name searches narrowed by date of birth`() {
    val uri = searchUrl()
      .queryParam("dateOfBirth", "12/04/1980")
      .build()
      .toUri()

    val body = testAPIClient.getSearchContactResults(uri)!!

    assertThat(body.page.totalElements).isEqualTo(2003)
    assertThat(body.content).hasSize(10)
  }

  @Test
  fun `should not cap exact current name searches`() {
    val body = testAPIClient.getSearchContactResults(searchUrl(searchType = "EXACT").build().toUri())!!

    assertThat(body.page.totalElements).isEqualTo(2003)
    assertThat(body.content).hasSize(10)
  }

  private fun searchUrl(previousNames: Boolean? = false, searchType: String = "PARTIAL"): UriComponentsBuilder = UriComponentsBuilder.fromPath("contact/search")
    .queryParam("searchType", searchType)
    .queryParam("lastName", "cApSeArCh")
    .apply { previousNames?.let { queryParam("previousNames", it) } }
}
