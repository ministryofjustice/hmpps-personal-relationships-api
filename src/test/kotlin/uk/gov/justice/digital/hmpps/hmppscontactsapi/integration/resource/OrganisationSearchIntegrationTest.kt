package uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.resource

import org.apache.commons.lang3.RandomStringUtils
import org.apache.commons.lang3.RandomUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.SecureAPIIntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.OrganisationSearchRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.migrate.MigrateOrganisationAddress
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.migrate.MigrateOrganisationPhoneNumber
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.migrate.MigrateOrganisationRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.OrganisationSummary

class OrganisationSearchIntegrationTest : SecureAPIIntegrationTestBase() {

  override val allowedRoles: Set<String> = setOf("ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__RW", "ROLE_CONTACTS__R")

  @BeforeEach
  fun setUp() {
    createOrg(minimalRequest().copy(organisationName = "ZZZ No Match Company"))
  }

  override fun baseRequestBuilder(): WebTestClient.RequestHeadersSpec<*> = webTestClient.get()
    .uri("/organisation/search?name=foo")
    .accept(MediaType.APPLICATION_JSON)

  @Test
  fun `Should return bad request if no name is specified`() {
    webTestClient.get()
      .uri("/organisation/search")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CONTACTS_ADMIN")))
      .exchange()
      .expectStatus()
      .isBadRequest
  }

  @ParameterizedTest
  @ValueSource(strings = ["ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__R", "ROLE_CONTACTS__RW"])
  fun `should return empty list if no organisation found and work with all roles`(role: String) {
    val response = testAPIClient.searchOrganisations(OrganisationSearchRequest("ABC"))
    assertThat(response.empty).isTrue()
    assertThat(response.content).isEmpty()
  }

  @Test
  fun `should return matching organisations in name sort order by default`() {
    createOrg(minimalRequest().copy(organisationName = "John's Candlesticks"))
    createOrg(minimalRequest().copy(organisationName = "Another John's Taxis"))
    createOrg(minimalRequest().copy(organisationName = "John's Bakery"))

    val response = testAPIClient.searchOrganisations(OrganisationSearchRequest("John"))
    assertThat(response.content).hasSize(3)
    assertThat(response.content).extracting("organisationName").isEqualTo(
      listOf(
        "Another John's Taxis",
        "John's Bakery",
        "John's Candlesticks",
      ),
    )
  }

  @Test
  fun `should return matching organisations in requested sort order`() {
    createOrg(minimalRequest().copy(organisationName = "Lisa's Candlesticks", nomisCorporateId = 2000001))
    createOrg(minimalRequest().copy(organisationName = "Another Lisa's Taxis", nomisCorporateId = 2000002))
    createOrg(minimalRequest().copy(organisationName = "Lisa's Bakery", nomisCorporateId = 2000003))

    val response = testAPIClient.searchOrganisations(OrganisationSearchRequest("Lisa"), sort = listOf("organisationId"))
    assertThat(response.content).hasSize(3)
    assertThat(response.content).extracting("organisationName").isEqualTo(
      listOf(
        "Lisa's Candlesticks",
        "Another Lisa's Taxis",
        "Lisa's Bakery",
      ),
    )
  }

  @Test
  fun `should return organisation pages`() {
    createOrg(minimalRequest().copy(organisationName = "Jane's Candlesticks"))
    createOrg(minimalRequest().copy(organisationName = "Another Jane's Taxis"))
    createOrg(minimalRequest().copy(organisationName = "Jane's Bakery"))

    assertThat(
      testAPIClient.searchOrganisations(OrganisationSearchRequest("Jane"), page = 0, size = 1).content[0].organisationName,
    ).isEqualTo("Another Jane's Taxis")
    assertThat(
      testAPIClient.searchOrganisations(OrganisationSearchRequest("Jane"), page = 1, size = 1).content[0].organisationName,
    ).isEqualTo("Jane's Bakery")
    assertThat(
      testAPIClient.searchOrganisations(OrganisationSearchRequest("Jane"), page = 2, size = 1).content[0].organisationName,
    ).isEqualTo("Jane's Candlesticks")
  }

  @Test
  fun `should return primary address if there is one`() {
    val org = minimalRequest().copy(
      addresses = listOf(
        MigrateOrganisationAddress(
          nomisAddressId = RandomUtils.secure().randomLong(),
          type = "HOME",
          flat = "F",
          premise = "10",
          street = "Dublin Road",
          locality = "locality",
          postCode = "D1 1DN",
          city = "25343",
          county = "S.YORKSHIRE",
          country = "ENG",
          primaryAddress = true,
        ),
        MigrateOrganisationAddress(
          nomisAddressId = RandomUtils.secure().randomLong(),
          type = "BUS",
          flat = "G",
          premise = "11",
          street = "Someplace",
          locality = "locality",
          postCode = "D1 1DN",
          city = "25343",
          county = "S.YORKSHIRE",
          country = "ENG",
          primaryAddress = false,
        ),
      ),
    )
    createOrg(org)

    val response = testAPIClient.searchOrganisations(OrganisationSearchRequest(org.organisationName))
    assertThat(response.content).hasSize(1)
    assertThat(response.content[0]).isEqualTo(
      OrganisationSummary(
        organisationId = org.nomisCorporateId,
        organisationName = org.organisationName,
        organisationActive = true,
        flat = "F",
        property = "10",
        street = "Dublin Road",
        area = "locality",
        postcode = "D1 1DN",
        cityCode = "25343",
        cityDescription = "Sheffield",
        countyCode = "S.YORKSHIRE",
        countyDescription = "South Yorkshire",
        countryCode = "ENG",
        countryDescription = "England",
        businessPhoneNumber = null,
        businessPhoneNumberExtension = null,
      ),
    )
  }

  @Test
  fun `should not return address if there is only a non-primary one`() {
    val org = minimalRequest().copy(
      addresses = listOf(
        MigrateOrganisationAddress(
          nomisAddressId = RandomUtils.secure().randomLong(),
          type = "HOME",
          flat = "F",
          premise = "10",
          street = "Dublin Road",
          locality = "locality",
          postCode = "D1 1DN",
          city = "25343",
          county = "S.YORKSHIRE",
          country = "ENG",
          primaryAddress = false,
        ),
      ),
    )
    createOrg(org)

    val response = testAPIClient.searchOrganisations(OrganisationSearchRequest(org.organisationName))
    assertThat(response.content).hasSize(1)
    assertThat(response.content[0]).isEqualTo(
      OrganisationSummary(
        organisationId = org.nomisCorporateId,
        organisationName = org.organisationName,
        organisationActive = true,
        flat = null,
        property = null,
        street = null,
        area = null,
        postcode = null,
        cityCode = null,
        cityDescription = null,
        countyCode = null,
        countyDescription = null,
        countryCode = null,
        countryDescription = null,
        businessPhoneNumber = null,
        businessPhoneNumberExtension = null,
      ),
    )
  }

  @Test
  fun `should return primary address business phone number if there is one`() {
    val org = minimalRequest().copy(
      addresses = listOf(
        MigrateOrganisationAddress(
          nomisAddressId = RandomUtils.secure().randomLong(),
          type = "HOME",
          flat = "F",
          premise = "10",
          street = "Dublin Road",
          locality = "locality",
          postCode = "D1 1DN",
          city = "25343",
          county = "S.YORKSHIRE",
          country = "ENG",
          primaryAddress = true,
          phoneNumbers = listOf(
            MigrateOrganisationPhoneNumber(
              nomisPhoneId = RandomUtils.secure().randomLong(),
              type = "BUS",
              number = "123",
              extension = "321",
            ),
            MigrateOrganisationPhoneNumber(
              nomisPhoneId = RandomUtils.secure().randomLong(),
              type = "MOB",
              number = "999",
              extension = "666",
            ),
          ),
        ),
      ),
    )
    createOrg(org)

    val response = testAPIClient.searchOrganisations(OrganisationSearchRequest(org.organisationName))
    assertThat(response.content).hasSize(1)
    assertThat(response.content[0]).isEqualTo(
      OrganisationSummary(
        organisationId = org.nomisCorporateId,
        organisationName = org.organisationName,
        organisationActive = true,
        flat = "F",
        property = "10",
        street = "Dublin Road",
        area = "locality",
        postcode = "D1 1DN",
        cityCode = "25343",
        cityDescription = "Sheffield",
        countyCode = "S.YORKSHIRE",
        countyDescription = "South Yorkshire",
        countryCode = "ENG",
        countryDescription = "England",
        businessPhoneNumber = "123",
        businessPhoneNumberExtension = "321",
      ),
    )
  }

  @Test
  fun `should return primary address with no phone number if there is no business one`() {
    val org = minimalRequest().copy(
      addresses = listOf(
        MigrateOrganisationAddress(
          nomisAddressId = RandomUtils.secure().randomLong(),
          type = "HOME",
          flat = "F",
          premise = "10",
          street = "Dublin Road",
          locality = "locality",
          postCode = "D1 1DN",
          city = "25343",
          county = "S.YORKSHIRE",
          country = "ENG",
          primaryAddress = true,
          phoneNumbers = listOf(
            MigrateOrganisationPhoneNumber(
              nomisPhoneId = RandomUtils.secure().randomLong(),
              type = "MOB",
              number = "999",
              extension = "666",
            ),
          ),
        ),
      ),
    )
    createOrg(org)

    val response = testAPIClient.searchOrganisations(OrganisationSearchRequest(org.organisationName))
    assertThat(response.content).hasSize(1)
    assertThat(response.content[0]).isEqualTo(
      OrganisationSummary(
        organisationId = org.nomisCorporateId,
        organisationName = org.organisationName,
        organisationActive = true,
        flat = "F",
        property = "10",
        street = "Dublin Road",
        area = "locality",
        postcode = "D1 1DN",
        cityCode = "25343",
        cityDescription = "Sheffield",
        countyCode = "S.YORKSHIRE",
        countyDescription = "South Yorkshire",
        countryCode = "ENG",
        countryDescription = "England",
        businessPhoneNumber = null,
        businessPhoneNumberExtension = null,
      ),
    )
  }

  private fun minimalRequest() = MigrateOrganisationRequest(
    nomisCorporateId = RandomUtils.secure().randomLong(10000, 99999),
    organisationName = RandomStringUtils.secure().nextAlphabetic(25),
    programmeNumber = null,
    vatNumber = null,
    caseloadId = null,
    comments = null,
    active = true,
    deactivatedDate = null,
    organisationTypes = emptyList(),
    phoneNumbers = emptyList(),
    emailAddresses = emptyList(),
    webAddresses = emptyList(),
    addresses = emptyList(),
  )

  private fun createOrg(request: MigrateOrganisationRequest) {
    testAPIClient.migrateAnOrganisation(request)
  }
}
