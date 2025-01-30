package uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.resource

import org.apache.commons.lang3.RandomUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.SecureAPIIntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.migrate.AbstractAuditable
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.migrate.MigrateOrganisationAddress
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.migrate.MigrateOrganisationEmailAddress
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.migrate.MigrateOrganisationPhoneNumber
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.migrate.MigrateOrganisationRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.migrate.MigrateOrganisationType
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.migrate.MigrateOrganisationWebAddress
import java.time.LocalDate
import java.time.LocalDateTime

@Nested
class GetOrganisationByOrganisationIdIntegrationTest : SecureAPIIntegrationTestBase() {

  override val allowedRoles: Set<String> = setOf("ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__RW", "ROLE_CONTACTS__R")

  override fun baseRequestBuilder(): WebTestClient.RequestHeadersSpec<*> = webTestClient.get()
    .uri("/organisation/001")

  @ParameterizedTest
  @ValueSource(strings = ["ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__R", "ROLE_CONTACTS__RW"])
  fun `should return not found if no organisation found`(role: String) {
    webTestClient.get()
      .uri("/organisation/9999")
      .headers(setAuthorisation(roles = listOf(role)))
      .exchange()
      .expectStatus()
      .isNotFound
  }

  @Test
  fun `should return full organisation data when using a valid organisation id`() {
    val organisationId = RandomUtils.secure().randomLong()
    val migrate = MigrateOrganisationRequest(
      nomisCorporateId = organisationId,
      organisationName = "Basic Org",
      programmeNumber = "Programme Number",
      vatNumber = "VAT Number",
      caseloadId = "CL",
      comments = "Some comments",
      active = false,
      deactivatedDate = LocalDate.of(2025, 1, 2),
      organisationTypes = listOf(MigrateOrganisationType("BSKILLS").setCreatedAndModified()),
      phoneNumbers = listOf(
        MigrateOrganisationPhoneNumber(
          nomisPhoneId = RandomUtils.secure().randomLong(),
          type = "MOB",
          number = "123",
          extension = "321",
        ).setCreatedAndModified(),
      ),
      emailAddresses = listOf(
        MigrateOrganisationEmailAddress(
          nomisEmailAddressId = RandomUtils.secure().randomLong(),
          email = "test@example.com",
        ).setCreatedAndModified(),
      ),
      webAddresses = listOf(
        MigrateOrganisationWebAddress(
          nomisWebAddressId = RandomUtils.secure().randomLong(),
          webAddress = "www.example.com",
        ).setCreatedAndModified(),
      ),
      addresses = listOf(
        MigrateOrganisationAddress(
          nomisAddressId = RandomUtils.secure().randomLong(),
          type = "BUS",
          primaryAddress = true,
          mailAddress = true,
          serviceAddress = true,
          noFixedAddress = false,
          flat = "F",
          premise = "10",
          street = "Dublin Road",
          locality = "locality",
          city = "25343",
          county = "S.YORKSHIRE",
          country = "ENG",
          postCode = "D1 1DN",
          specialNeedsCode = "DEAF",
          contactPersonName = "Jeff",
          businessHours = "9-5",
          comment = "Comments",
          startDate = LocalDate.of(2020, 2, 3),
          endDate = LocalDate.of(2021, 3, 4),
          phoneNumbers = listOf(
            MigrateOrganisationPhoneNumber(
              nomisPhoneId = RandomUtils.secure().randomLong(),
              type = "BUS",
              number = "9123",
              extension = "321",
            ).setCreatedAndModified(),
          ),
        ).setCreatedAndModified(),
      ),
    ).setCreatedAndModified()

    testAPIClient.migrateAnOrganisation(migrate)

    val organisation = testAPIClient.getOrganisation(organisationId)

    with(organisation) {
      assertThat(this.organisationId).isEqualTo(organisationId)
      assertThat(organisationName).isEqualTo("Basic Org")
      assertThat(programmeNumber).isEqualTo("Programme Number")
      assertThat(vatNumber).isEqualTo("VAT Number")
      assertThat(caseloadId).isEqualTo("CL")
      assertThat(comments).isEqualTo("Some comments")
      assertThat(active).isEqualTo(false)
      assertThat(deactivatedDate).isEqualTo(LocalDate.of(2025, 1, 2))
      assertThat(createdBy).isEqualTo("CREATED")
      assertThat(createdTime).isEqualTo(LocalDateTime.of(2020, 2, 3, 10, 30))
      assertThat(updatedBy).isEqualTo("MODIFIED")
      assertThat(updatedTime).isEqualTo(LocalDateTime.of(2020, 3, 4, 11, 45))

      assertThat(organisationTypes).hasSize(1)
      with(organisationTypes[0]) {
        assertThat(organisationType).isEqualTo("BSKILLS")
        assertThat(organisationTypeDescription).isEqualTo("Basic Skills Provider")
        assertThat(createdBy).isEqualTo("CREATED")
        assertThat(createdTime).isEqualTo(LocalDateTime.of(2020, 2, 3, 10, 30))
        assertThat(updatedBy).isEqualTo("MODIFIED")
        assertThat(updatedTime).isEqualTo(LocalDateTime.of(2020, 3, 4, 11, 45))
      }
      assertThat(phoneNumbers).hasSize(1)
      with(phoneNumbers[0]) {
        assertThat(phoneType).isEqualTo("MOB")
        assertThat(phoneTypeDescription).isEqualTo("Mobile")
        assertThat(phoneNumber).isEqualTo("123")
        assertThat(extNumber).isEqualTo("321")
        assertThat(createdBy).isEqualTo("CREATED")
        assertThat(createdTime).isEqualTo(LocalDateTime.of(2020, 2, 3, 10, 30))
        assertThat(updatedBy).isEqualTo("MODIFIED")
        assertThat(updatedTime).isEqualTo(LocalDateTime.of(2020, 3, 4, 11, 45))
      }
      assertThat(emailAddresses).hasSize(1)
      with(emailAddresses[0]) {
        assertThat(emailAddress).isEqualTo("test@example.com")
        assertThat(createdBy).isEqualTo("CREATED")
        assertThat(createdTime).isEqualTo(LocalDateTime.of(2020, 2, 3, 10, 30))
        assertThat(updatedBy).isEqualTo("MODIFIED")
        assertThat(updatedTime).isEqualTo(LocalDateTime.of(2020, 3, 4, 11, 45))
      }
      assertThat(webAddresses).hasSize(1)
      with(webAddresses[0]) {
        assertThat(webAddress).isEqualTo("www.example.com")
        assertThat(createdBy).isEqualTo("CREATED")
        assertThat(createdTime).isEqualTo(LocalDateTime.of(2020, 2, 3, 10, 30))
        assertThat(updatedBy).isEqualTo("MODIFIED")
        assertThat(updatedTime).isEqualTo(LocalDateTime.of(2020, 3, 4, 11, 45))
      }
      assertThat(addresses).hasSize(1)
      with(addresses[0]) {
        assertThat(addressType).isEqualTo("BUS")
        assertThat(addressTypeDescription).isEqualTo("Business address")
        assertThat(flat).isEqualTo("F")
        assertThat(property).isEqualTo("10")
        assertThat(street).isEqualTo("Dublin Road")
        assertThat(area).isEqualTo("locality")
        assertThat(postcode).isEqualTo("D1 1DN")
        assertThat(cityCode).isEqualTo("25343")
        assertThat(cityDescription).isEqualTo("Sheffield")
        assertThat(countyCode).isEqualTo("S.YORKSHIRE")
        assertThat(countyDescription).isEqualTo("South Yorkshire")
        assertThat(countryCode).isEqualTo("ENG")
        assertThat(countryDescription).isEqualTo("England")
        assertThat(specialNeedsCode).isEqualTo("DEAF")
        assertThat(specialNeedsCodeDescription).isEqualTo("Hearing Impaired Translation")
        assertThat(contactPersonName).isEqualTo("Jeff")
        assertThat(businessHours).isEqualTo("9-5")
        assertThat(comments).isEqualTo("Comments")
        assertThat(startDate).isEqualTo(LocalDate.of(2020, 2, 3))
        assertThat(endDate).isEqualTo(LocalDate.of(2021, 3, 4))
        assertThat(serviceAddress).isTrue()
        assertThat(mailAddress).isTrue()
        assertThat(primaryAddress).isTrue()
        assertThat(noFixedAddress).isFalse()
        assertThat(createdBy).isEqualTo("CREATED")
        assertThat(createdTime).isEqualTo(LocalDateTime.of(2020, 2, 3, 10, 30))
        assertThat(updatedBy).isEqualTo("MODIFIED")
        assertThat(updatedTime).isEqualTo(LocalDateTime.of(2020, 3, 4, 11, 45))
        assertThat(phoneNumbers).hasSize(1)
        with(phoneNumbers[0]) {
          assertThat(phoneType).isEqualTo("BUS")
          assertThat(phoneTypeDescription).isEqualTo("Business")
          assertThat(phoneNumber).isEqualTo("9123")
          assertThat(extNumber).isEqualTo("321")
          assertThat(createdBy).isEqualTo("CREATED")
          assertThat(createdTime).isEqualTo(LocalDateTime.of(2020, 2, 3, 10, 30))
          assertThat(updatedBy).isEqualTo("MODIFIED")
          assertThat(updatedTime).isEqualTo(LocalDateTime.of(2020, 3, 4, 11, 45))
        }
      }
    }
  }

  private fun <T : AbstractAuditable> T.setCreatedAndModified(): T = apply {
    createDateTime = LocalDateTime.of(2020, 2, 3, 10, 30)
    createUsername = "CREATED"
    modifyDateTime = LocalDateTime.of(2020, 3, 4, 11, 45)
    modifyUsername = "MODIFIED"
  }
}
