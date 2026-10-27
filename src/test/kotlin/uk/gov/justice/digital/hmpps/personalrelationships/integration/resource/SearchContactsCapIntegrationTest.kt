package uk.gov.justice.digital.hmpps.personalrelationships.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.jpa.repository.Query
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.test.context.jdbc.Sql
import org.springframework.web.util.UriComponentsBuilder
import uk.gov.justice.digital.hmpps.personalrelationships.integration.PostgresIntegrationTestBase
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.ContactSearchResultItem
import uk.gov.justice.digital.hmpps.personalrelationships.repository.ContactSearchRepository
import uk.gov.justice.digital.hmpps.personalrelationships.util.StubUser
import java.sql.Types

@Sql("classpath:search-cap.tests/data-for-search-cap-test.sql")
@Sql(
  scripts = ["classpath:search-cap.tests/cleanup-search-cap-test.sql"],
  executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD,
)
class SearchContactsCapIntegrationTest : PostgresIntegrationTestBase() {

  @Autowired
  private lateinit var jdbcTemplate: NamedParameterJdbcTemplate

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

  @ParameterizedTest
  @CsvSource(
    "lastName,asc",
    "lastName,desc",
    "firstName,asc",
    "firstName,desc",
    "middleNames,asc",
    "middleNames,desc",
    "dateOfBirth,asc",
    "dateOfBirth,desc",
    "id,asc",
    "id,desc",
  )
  fun `should preserve all sounds-like matches and stable sorting across pages`(property: String, direction: String) {
    val contacts = (0..4).flatMap { pageNumber ->
      val uri = searchUrl(searchType = "SOUNDS_LIKE")
        .queryParam("page", pageNumber)
        .queryParam("size", 500)
        .queryParam("sort", "$property,$direction")
        .build()
        .toUri()

      val body = testAPIClient.getSearchContactResults(uri)!!

      assertThat(body.page.totalElements).isEqualTo(2003)
      assertThat(body.page.totalPages).isEqualTo(5)
      assertThat(body.page.number).isEqualTo(pageNumber.toLong())
      assertThat(body.content).hasSize(if (pageNumber == 4) 3 else 500)
      body.content
    }

    val comparator = when (property) {
      "firstName" -> compareBy<Long> { if (it % 2 == 0L) "Lex" else "Lexa" }
      "middleNames" -> compareBy<Long> { if (it % 3 == 0L) "Middlex" else "Tess" }
      "id" -> naturalOrder()
      else -> compareBy<Long> { 0 }
    }
    val requestedOrder = if (direction == "desc") comparator.reversed() else comparator
    val expectedIds = (9000001L..9002003L).sortedWith(requestedOrder.thenBy { it })

    assertThat(contacts.map { it.id }).containsExactlyElementsOf(expectedIds)

    val beyondLastPage = searchUrl(searchType = "SOUNDS_LIKE")
      .queryParam("page", 5)
      .queryParam("size", 500)
      .queryParam("sort", "$property,$direction")
      .build()
      .toUri()
    assertThat(testAPIClient.getSearchContactResults(beyondLastPage)!!.content).isEmpty()
  }

  @ParameterizedTest
  @CsvSource("Lex,,2003", ",Middlex,667", "Lex,Middlex,667", ",Tess,1336", "Other,,0")
  fun `should apply every supplied sounds-like name filter to results and counts`(firstName: String?, middleNames: String?, expectedCount: Long) {
    val uri = searchUrl(searchType = "SOUNDS_LIKE")
      .apply {
        firstName?.let { queryParam("firstName", it) }
        middleNames?.let { queryParam("middleNames", it) }
      }
      .queryParam("size", 10)
      .queryParam("sort", "lastName,asc")
      .build()
      .toUri()

    val body = testAPIClient.getSearchContactResults(uri)!!

    assertThat(body.page.totalElements).isEqualTo(expectedCount)
    assertThat(body.content).hasSize(if (expectedCount == 0L) 0 else 10)
    middleNames?.let { expectedMiddleNames ->
      assertThat(body.content).allSatisfy { assertThat(it.middleNames).isEqualTo(expectedMiddleNames) }
    }
  }

  @ParameterizedTest
  @CsvSource("firstName,Lex,2003", "middleNames,Middlex,667")
  fun `should find sounds-like matches without a surname`(property: String, name: String, expectedCount: Long) {
    val uri = searchUrl(searchType = "SOUNDS_LIKE")
      .replaceQueryParam("lastName")
      .queryParam(property, name)
      .queryParam("sort", "firstName,asc")
      .build()
      .toUri()

    val body = testAPIClient.getSearchContactResults(uri)!!

    assertThat(body.page.totalElements).isEqualTo(expectedCount)
    assertThat(body.content).hasSize(10)
  }

  @Test
  fun `should materialize sounds-like matches before sorting and limiting`() {
    val query = ContactSearchRepository::class.java.methods
      .single { it.name == "findAllByNamesSoundLike" }
      .getAnnotation(Query::class.java)
      .value
    val parameters = MapSqlParameterSource()
      .addValue("lastName", "Capsearch", Types.VARCHAR)
      .addValue("firstName", null, Types.VARCHAR)
      .addValue("middleNames", null, Types.VARCHAR)

    val plan = jdbcTemplate.queryForList(
      "explain $query order by c.last_name, c.contact_id limit 10",
      parameters,
      String::class.java,
    ).joinToString("\n")

    assertThat(plan).contains("CTE matching_contacts", "CTE Scan on matching_contacts", "Sort Key:")
  }

  private fun searchUrl(previousNames: Boolean? = false, searchType: String = "PARTIAL"): UriComponentsBuilder = UriComponentsBuilder.fromPath("contact/search")
    .queryParam("searchType", searchType)
    .queryParam("lastName", "cApSeArCh")
    .apply { previousNames?.let { queryParam("previousNames", it) } }
}
