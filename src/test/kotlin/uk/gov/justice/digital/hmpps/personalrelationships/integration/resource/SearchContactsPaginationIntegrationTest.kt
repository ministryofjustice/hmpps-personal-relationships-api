package uk.gov.justice.digital.hmpps.personalrelationships.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.util.UriComponentsBuilder
import uk.gov.justice.digital.hmpps.personalrelationships.integration.SecureAPIIntegrationTestBase
import uk.gov.justice.digital.hmpps.personalrelationships.util.StubUser
import java.net.URI
import kotlin.collections.toList

class SearchContactsPaginationIntegrationTest : SecureAPIIntegrationTestBase() {

  @BeforeEach
  fun setUp() {
    setCurrentUser(StubUser.READ_ONLY_USER)
  }

  override val allowedRoles: Set<String> = setOf("ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__RW", "ROLE_CONTACTS__R")

  override fun baseRequestBuilder(): WebTestClient.RequestHeadersSpec<*> = webTestClient.get()
    .uri(CONTACT_SEARCH_URL.toString())
    .accept(MediaType.APPLICATION_JSON)

  @Test
  fun `when contacts search is done then proper search results are returned irrespective of sort order`() {
    // test to check fix for VB-6479
    // 50 records have been added with lastName starting with ABCD and firstName as Test to replicate live scenario
    // starting from contact ID 2001 to 2050
    // the asserts check that all Ids starting from 2001 to 2050 are being returned which was not the case before the fix
    var sortValues = listOf("lastName,asc")
    assertPagedData(sortValues)

    sortValues = listOf("lastName,desc")
    assertPagedData(sortValues)

    sortValues = listOf("dateOfBirth,asc")
    assertPagedData(sortValues)

    sortValues = listOf("dateOfBirth,desc")
    assertPagedData(sortValues)
  }

  private fun getContactSearchUrl(pageNumber: Int? = 0, sortValues: List<String>): URI {
    val uri = UriComponentsBuilder.fromPath("contact/search")
      .queryParam("searchType", "PARTIAL")
      .queryParam("lastName", "ABCD")
      .queryParam("firstName", "Test")
      .queryParam("page", pageNumber)
      .queryParam("sort", sortValues.joinToString(","))
      .build()
      .toUri()

    return uri
  }

  private fun assertPagedData(
    sortValues: List<String>,
  ) {
    fun expectedIds(start: Long, end: Long) = (start..end).toList()
    var uri = getContactSearchUrl(0, sortValues)
    val contactIds = mutableListOf<Long>()
    var body = testAPIClient.getSearchContactResults(uri)

    // assert that 50 records are returned - split into 5 pages
    with(body!!) {
      assertThat(content).isNotEmpty()
      assertThat(content.size).isEqualTo(10)
      assertThat(page.totalElements).isEqualTo(50)
      assertThat(page.totalPages).isEqualTo(5)
      assertThat(page.size).isEqualTo(10)
    }

    // iterate over the 5 pages and collate all results into a list
    for (pageNumber in 0..4) {
      uri = getContactSearchUrl(pageNumber, sortValues)
      body = testAPIClient.getSearchContactResults(uri)
      contactIds.addAll(body!!.content.map { it.id })
    }

    assertThat(contactIds.distinct()).hasSize(50)
    // all contactIds from 2001 to 2050 should be present in the list
    assertThat(contactIds).containsAll(expectedIds(2001, 2050))
    assertThat(contactIds.min()).isEqualTo(2001)
    assertThat(contactIds.max()).isEqualTo(2050)
  }

  companion object {
    private val CONTACT_SEARCH_URL = UriComponentsBuilder.fromPath("contact/search")
      .queryParam("searchType", "PARTIAL")
      .queryParam("lastName", "ABCD")
      .queryParam("firstName", "Test")
      .build()
      .toUri()
  }
}
