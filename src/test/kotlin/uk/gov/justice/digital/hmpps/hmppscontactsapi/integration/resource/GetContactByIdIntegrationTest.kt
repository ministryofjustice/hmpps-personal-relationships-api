package uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.resource

import org.apache.commons.lang3.RandomStringUtils
import org.apache.commons.lang3.RandomUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.EmploymentEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.SecureAPIIntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.CreateContactRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.migrate.MigrateOrganisationAddress
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.migrate.MigrateOrganisationPhoneNumber
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.migrate.MigrateOrganisationRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.EmploymentRepository
import java.time.LocalDate
import java.time.LocalDateTime

class GetContactByIdIntegrationTest : SecureAPIIntegrationTestBase() {

  @Autowired
  private lateinit var employmentRepository: EmploymentRepository

  override val allowedRoles: Set<String> = setOf("ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__RW", "ROLE_CONTACTS__R")

  override fun baseRequestBuilder(): WebTestClient.RequestHeadersSpec<*> = webTestClient.get()
    .uri("/contact/123456")
    .accept(MediaType.APPLICATION_JSON)

  @Test
  fun `should return 404 if the contact doesn't exist`() {
    webTestClient.get()
      .uri("/contact/123456")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CONTACTS_ADMIN")))
      .exchange()
      .expectStatus()
      .isNotFound
  }

  @Test
  fun `should get the contact with all fields`() {
    val contact = testAPIClient.getContact(1)

    with(contact) {
      assertThat(id).isEqualTo(1)
      assertThat(title).isEqualTo("MR")
      assertThat(titleDescription).isEqualTo("Mr")
      assertThat(lastName).isEqualTo("Last")
      assertThat(firstName).isEqualTo("Jack")
      assertThat(middleNames).isEqualTo("Middle")
      assertThat(dateOfBirth).isEqualTo(LocalDate.of(2000, 11, 21))
      assertThat(isDeceased).isFalse()
      assertThat(deceasedDate).isNull()
      assertThat(createdBy).isEqualTo("TIM")
      assertThat(createdTime).isNotNull()
      assertThat(contact.addresses).hasSize(2)
    }
  }

  @Test
  fun `should get the contact with addresses`() {
    val contact = testAPIClient.getContact(1)

    with(contact) {
      assertThat(id).isEqualTo(1)
      assertThat(id).isEqualTo(1)

      with(contact.addresses[0]) {
        assertThat(contactAddressId).isEqualTo(1)
        assertThat(contactId).isEqualTo(1)
        assertThat(addressType).isEqualTo("HOME")
        assertThat(addressTypeDescription).isEqualTo("Home address")
        assertThat(primaryAddress).isEqualTo(true)
        assertThat(flat).isNull()
        assertThat(property).isEqualTo("24")
        assertThat(street).isEqualTo("Acacia Avenue")
        assertThat(area).isEqualTo("Bunting")
        assertThat(cityCode).isEqualTo("25343")
        assertThat(cityDescription).isEqualTo("Sheffield")
        assertThat(countyCode).isEqualTo("S.YORKSHIRE")
        assertThat(countyDescription).isEqualTo("South Yorkshire")
        assertThat(postcode).isEqualTo("S2 3LK")
        assertThat(countryCode).isEqualTo("ENG")
        assertThat(countryDescription).isEqualTo("England")
        assertThat(mailFlag).isFalse()
        assertThat(noFixedAddress).isFalse()
        assertThat(comments).isEqualTo("Some comments")
        assertThat(createdBy).isEqualTo("TIM")
        assertThat(createdTime).isNotNull()
      }
      with(contact.addresses[1]) {
        assertThat(contactAddressId).isEqualTo(2)
        assertThat(contactId).isEqualTo(1)
        assertThat(addressType).isEqualTo("WORK")
        assertThat(addressTypeDescription).isEqualTo("Work address")
        assertThat(primaryAddress).isEqualTo(false)
        assertThat(flat).isEqualTo("Flat 1")
        assertThat(verified).isTrue()
        assertThat(verifiedBy).isEqualTo("BOB")
        assertThat(verifiedTime).isEqualTo(LocalDateTime.of(2020, 1, 1, 10, 30, 0))
        assertThat(mailFlag).isTrue()
        assertThat(noFixedAddress).isTrue()
        assertThat(startDate).isEqualTo(LocalDate.of(2020, 1, 2))
        assertThat(endDate).isEqualTo(LocalDate.of(2029, 3, 4))
      }
    }
  }

  @Test
  fun `should get the contact with phone numbers`() {
    val contact = testAPIClient.getContact(1)

    with(contact) {
      assertThat(id).isEqualTo(1)

      // Phone numbers should exclude any address-specific numbers
      assertThat(contact.phoneNumbers).hasSize(1)
      with(contact.phoneNumbers[0]) {
        assertThat(contactPhoneId).isEqualTo(1)
        assertThat(contactId).isEqualTo(1)
        assertThat(phoneType).isEqualTo("MOB")
        assertThat(phoneTypeDescription).isEqualTo("Mobile")
        assertThat(phoneNumber).isEqualTo("07878 111111")
        assertThat(extNumber).isNull()
        assertThat(createdBy).isEqualTo("TIM")
      }

      assertThat(contact.addresses).hasSize(2)
      assertThat(contact.addresses[0].phoneNumbers).hasSize(1)
      assertThat(contact.addresses[0].phoneNumbers[0].contactPhoneId).isEqualTo(2)
      assertThat(contact.addresses[0].phoneNumbers[0].contactAddressPhoneId).isEqualTo(1)
      assertThat(contact.addresses[0].phoneNumbers[0].contactAddressId).isEqualTo(1)
      assertThat(contact.addresses[1].phoneNumbers).isEmpty()
    }
  }

  @Test
  fun `should get the contact with emails`() {
    val contact = testAPIClient.getContact(3)

    with(contact) {
      assertThat(id).isEqualTo(3)
      assertThat(contact.emailAddresses).hasSize(2)
      with(contact.emailAddresses[0]) {
        assertThat(contactEmailId).isEqualTo(3)
        assertThat(contactId).isEqualTo(3)
        assertThat(emailAddress).isEqualTo("mrs.last@example.com")
        assertThat(createdBy).isEqualTo("TIM")
      }
      with(contact.emailAddresses[1]) {
        assertThat(contactEmailId).isEqualTo(4)
        assertThat(contactId).isEqualTo(3)
        assertThat(emailAddress).isEqualTo("work@example.com")
        assertThat(createdBy).isEqualTo("JAMES")
      }
    }
  }

  @Test
  fun `should get the contact with identities`() {
    val contact = testAPIClient.getContact(1)

    with(contact) {
      assertThat(id).isEqualTo(1)
      assertThat(contact.identities).hasSize(1)
      with(contact.identities[0]) {
        assertThat(contactIdentityId).isEqualTo(1)
        assertThat(contactId).isEqualTo(1)
        assertThat(identityType).isEqualTo("DL")
        assertThat(identityTypeDescription).isEqualTo("Driving Licence")
        assertThat(identityValue).isEqualTo("LAST-87736799M")
        assertThat(issuingAuthority).isEqualTo("DVLA")
        assertThat(createdBy).isEqualTo("TIM")
        assertThat(createdTime).isNotNull()
      }
    }
  }

  @Test
  fun `should get deceased contacts`() {
    val contact = testAPIClient.getContact(19)

    with(contact) {
      assertThat(id).isEqualTo(19)
      assertThat(title).isNull()
      assertThat(titleDescription).isNull()
      assertThat(lastName).isEqualTo("Dead")
      assertThat(firstName).isEqualTo("Currently")
      assertThat(middleNames).isNull()
      assertThat(dateOfBirth).isEqualTo(LocalDate.of(1980, 1, 1))
      assertThat(isDeceased).isTrue()
      assertThat(deceasedDate).isEqualTo(LocalDate.of(2000, 1, 1))
      assertThat(createdBy).isEqualTo("TIM")
      assertThat(createdTime).isNotNull()
    }
  }

  @Test
  fun `should get contacts with language details`() {
    val contact = testAPIClient.getContact(20)

    with(contact) {
      assertThat(id).isEqualTo(20)
      assertThat(languageCode).isEqualTo("FRE-FRA")
      assertThat(languageDescription).isEqualTo("French")
      assertThat(interpreterRequired).isTrue()
    }
  }

  @Test
  fun `should get contacts with gender details`() {
    val contact = testAPIClient.getContact(16)

    with(contact) {
      assertThat(id).isEqualTo(16)
      assertThat(gender).isEqualTo("F")
      assertThat(genderDescription).isEqualTo("Female")
    }
  }

  @Test
  fun `should get contacts with domestic status`() {
    val contact = testAPIClient.getContact(1)

    with(contact) {
      assertThat(id).isEqualTo(1)
      assertThat(domesticStatusCode).isEqualTo("M")
      assertThat(domesticStatusDescription).isEqualTo("Married or in civil partnership")
    }
  }

  @ParameterizedTest
  @ValueSource(strings = ["ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__R", "ROLE_CONTACTS__RW"])
  fun `should get contacts with staff flag`(role: String) {
    val contact = testAPIClient.getContact(1, role)

    with(contact) {
      assertThat(id).isEqualTo(1)
      assertThat(isStaff).isTrue()
    }
  }

  @Test
  fun `should get the contact with employments`() {
    val newContact = testAPIClient.createAContact(CreateContactRequest(firstName = "First", lastName = "Bob", createdBy = "TEST"))
    val org1 = createOrg()
    val org2 = createOrg()

    val activeEmployment = employmentRepository.saveAndFlush(
      EmploymentEntity(
        employmentId = 0,
        organisationId = org1.nomisCorporateId,
        contactId = newContact.id,
        active = true,
        createdBy = "TEST",
        createdTime = LocalDateTime.now(),
        updatedBy = null,
        updatedTime = null,
      ),
    )

    val inactiveEmployment = employmentRepository.saveAndFlush(
      EmploymentEntity(
        employmentId = 0,
        organisationId = org2.nomisCorporateId,
        contactId = newContact.id,
        active = false,
        createdBy = "TEST",
        createdTime = LocalDateTime.now(),
        updatedBy = null,
        updatedTime = null,
      ),
    )

    val contact = testAPIClient.getContact(newContact.id)

    with(contact) {
      assertThat(id).isEqualTo(newContact.id)
      assertThat(contact.employments).hasSize(2)

      val activeEmploymentDetails = employments.find { it.employmentId == activeEmployment.employmentId }!!
      with(activeEmploymentDetails) {
        assertThat(employer.organisationId).isEqualTo(org1.nomisCorporateId)
        assertThat(isActive).isTrue()
      }
      val inactiveEmploymentDetails = employments.find { it.employmentId == inactiveEmployment.employmentId }!!
      with(inactiveEmploymentDetails) {
        assertThat(employer.organisationId).isEqualTo(org2.nomisCorporateId)
        assertThat(isActive).isFalse()
      }
    }
  }

  private fun createOrg(): MigrateOrganisationRequest {
    val request = MigrateOrganisationRequest(
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
          ),
        ),
      ),
    )
    testAPIClient.migrateAnOrganisation(request)
    return request
  }
}
