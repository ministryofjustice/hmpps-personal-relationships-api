package uk.gov.justice.digital.hmpps.personalrelationships.integration.resource.migrate

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.groups.Tuple
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.personalrelationships.integration.PostgresIntegrationTestBase
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.migrate.CodedValue
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.migrate.Corporate
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.migrate.MigrateAddress
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.migrate.MigrateContactRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.migrate.MigrateEmailAddress
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.migrate.MigrateEmployment
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.migrate.MigrateIdentifier
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.migrate.MigratePhoneNumber
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.migrate.MigrateRestriction
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.migrate.ElementType
import uk.gov.justice.digital.hmpps.personalrelationships.repository.ContactPhoneRepository
import uk.gov.justice.digital.hmpps.personalrelationships.repository.ContactRepository
import uk.gov.justice.digital.hmpps.personalrelationships.util.StubUser
import java.time.LocalDate
import java.time.LocalDateTime

class MigrateContactIntegrationTest : PostgresIntegrationTestBase() {
  @Autowired
  private lateinit var contactRepository: ContactRepository

  @Autowired
  private lateinit var contactPhoneRepository: ContactPhoneRepository

  private val aUsername = "XXX"
  private val aDateTime = LocalDateTime.of(2024, 1, 1, 13, 0)

  @BeforeEach
  fun initialiseData() {
    setCurrentUser(StubUser.SYNC_AND_MIGRATE_USER)
  }

  @Test
  fun `should return unauthorized if no token provided`() {
    webTestClient.post()
      .uri("/migrate/contact")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(basicMigrationRequest(personId = 500))
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @ParameterizedTest
  @ValueSource(strings = ["ROLE_CONTACTS_ADMIN", "ROLE_CONTACTS__R", "ROLE_CONTACTS__RW"])
  fun `should return forbidden without an authorised role on the token`(authRole: String) {
    setCurrentUser(StubUser(authRole, authRole, listOf(authRole), activeCaseLoadId = null, isSystemUser = true))
    webTestClient.post()
      .uri("/migrate/contact")
      .accept(MediaType.APPLICATION_JSON)
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(basicMigrationRequest(personId = 500L))
      .headers(setAuthorisationUsingCurrentUser())
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `should migrate a basic contact`() {
    val request = basicMigrationRequest(personId = 500L)
    val countContactsBefore = contactRepository.count()

    val result = testAPIClient.migrateAContact(request)

    with(result) {
      with(contact) {
        assertThat(elementType).isEqualTo(ElementType.CONTACT)
        assertThat(nomisId).isEqualTo(request.personId)
        // NOTE: The dpsId and personId should both be the same
        assertThat(dpsId).isEqualTo(request.personId)
      }
      assertThat(lastName).isEqualTo(request.lastName)
      assertThat(dateOfBirth).isEqualTo(request.dateOfBirth)
    }

    assertThat(contactRepository.count()).isEqualTo(countContactsBefore + 1)
  }

  @Test
  fun `should migrate a contact with addresses, phones, emails, restrictions, identifiers and employments`() {
    val request = basicMigrationRequest(502).copy(
      addresses = addresses(),
      phoneNumbers = phoneNumbers(),
      emailAddresses = emails(),
      identifiers = identifiers(),
      restrictions = restrictions(),
      employments = employments(),
    )

    val result = testAPIClient.migrateAContact(request)

    with(result) {
      assertThat(contact.elementType).isEqualTo(ElementType.CONTACT)
      assertThat(contact.nomisId).isEqualTo(request.personId)
      assertThat(contact.dpsId).isEqualTo(request.personId)

      assertThat(phoneNumbers).hasSize(2)
      assertThat(phoneNumbers).extracting("elementType", "nomisId")
        .containsAll(
          listOf(
            Tuple(ElementType.PHONE, 101L),
            Tuple(ElementType.PHONE, 102L),
          ),
        )

      assertThat(addresses).hasSize(2)
      assertThat(addresses[0].address).extracting("elementType", "nomisId").contains(ElementType.ADDRESS, 201L)
      assertThat(addresses[1].address).extracting("elementType", "nomisId").contains(ElementType.ADDRESS, 202L)

      assertThat(emailAddresses).hasSize(2)
      assertThat(emailAddresses).extracting("elementType", "nomisId")
        .containsAll(listOf(Tuple(ElementType.EMAIL, 301L), Tuple(ElementType.EMAIL, 302L)))

      assertThat(restrictions).hasSize(2)
      assertThat(restrictions).extracting("elementType", "nomisId")
        .containsAll(listOf(Tuple(ElementType.RESTRICTION, 401L), Tuple(ElementType.RESTRICTION, 402L)))

      assertThat(identities).hasSize(2)
      assertThat(identities).extracting("elementType", "nomisId")
        .containsAll(listOf(Tuple(ElementType.IDENTITY, 601L), Tuple(ElementType.IDENTITY, 602L)))

      assertThat(employments).hasSize(2)
      assertThat(employments).extracting("elementType", "nomisId")
        .containsAll(listOf(Tuple(ElementType.EMPLOYMENT, 501L), Tuple(ElementType.EMPLOYMENT, 502L)))
    }
  }

  @Test
  fun `should migrate a contact with addresses with linked phone numbers`() {
    val phoneCount = contactPhoneRepository.count()

    val request = basicMigrationRequest(personId = 503).copy(
      addresses = addressesWithPhones(),
    )

    val result = testAPIClient.migrateAContact(request)

    with(result) {
      assertThat(phoneNumbers).hasSize(0)
      assertThat(addresses).hasSize(1)
      assertThat(addresses[0].address).extracting("elementType", "nomisId").contains(ElementType.ADDRESS, 201L)
      assertThat(addresses[0].phones).hasSize(2)
      assertThat(addresses[0].phones).extracting("elementType", "nomisId")
        .containsAll(
          listOf(
            Tuple(ElementType.ADDRESS_PHONE, 101L),
            Tuple(ElementType.ADDRESS_PHONE, 102L),
          ),
        )
    }

    assertThat(contactPhoneRepository.count()).isEqualTo(phoneCount + 2)
  }

  private fun basicMigrationRequest(
    personId: Long,
    lastName: String = "Smith",
    firstName: String = "John",
    dateOfBirth: LocalDate = LocalDate.of(2001, 1, 1),
  ) = MigrateContactRequest(
    personId = personId,
    title = CodedValue("MR", "Mr"),
    lastName = lastName,
    firstName = firstName,
    dateOfBirth = dateOfBirth,
    gender = CodedValue("M", "Male"),
  ).also {
    it.createUsername = aUsername
    it.createDateTime = aDateTime
    it.modifyUsername = aUsername
    it.modifyDateTime = aDateTime
  }

  private fun phoneNumbers() = listOf(
    MigratePhoneNumber(phoneId = 101L, number = "11111", extension = "1", type = CodedValue("HOME", "Home")),
    MigratePhoneNumber(phoneId = 102L, number = "22222", extension = "2", type = CodedValue("WORK", "Work")),
  )

  private fun addresses() = listOf(
    MigrateAddress(
      addressId = 201L,
      type = CodedValue("HOME", "Home"),
      premise = "10",
      street = "Dublin Road",
      postCode = "D1 1DN",
      primaryAddress = true,
    ),
    MigrateAddress(
      addressId = 202L,
      type = CodedValue("WORK", "Work"),
      premise = "11",
      street = "Dublin Road",
      postCode = "D1 2DN",
    ),
  )

  private fun addressesWithPhones() = listOf(
    MigrateAddress(
      addressId = 201L,
      type = CodedValue("HOME", "Home"),
      premise = "10",
      street = "Dublin Road",
      postCode = "D1 1DN",
      primaryAddress = true,
      phoneNumbers = listOf(
        MigratePhoneNumber(phoneId = 101L, number = "11111", extension = "1", type = CodedValue("HOME", "Home")),
        MigratePhoneNumber(phoneId = 102L, number = "22222", extension = "2", type = CodedValue("WORK", "Work")),
      ),
    ),
  )

  private fun emails() = listOf(
    MigrateEmailAddress(emailAddressId = 301L, email = "a@.com"),
    MigrateEmailAddress(emailAddressId = 302L, email = "b@b.com"),
  )

  private fun restrictions() = listOf(
    MigrateRestriction(
      id = 401L,
      type = CodedValue("CHILD", "Desc"),
      comment = "Active",
      effectiveDate = LocalDate.now(),
      expiryDate = LocalDate.now().plusDays(30),
    ),
    MigrateRestriction(
      id = 402L,
      type = CodedValue("PREINF", "Desc"),
      comment = "Expired",
      effectiveDate = LocalDate.now().minusDays(30),
      expiryDate = LocalDate.now().minusDays(1),
    ),
  )

  private fun identifiers() = listOf(
    MigrateIdentifier(sequence = 601L, type = CodedValue("DL", "Driving Licence"), identifier = "DL1", issuedAuthority = "DVLA"),
    MigrateIdentifier(sequence = 602L, type = CodedValue("PASS", "Passport"), identifier = "PASS1", issuedAuthority = "UKBORDER"),
  )

  private fun employments() = listOf(
    MigrateEmployment(sequence = 501L, corporate = Corporate(id = 123L), active = true),
    MigrateEmployment(sequence = 502L, corporate = Corporate(id = 124L), active = false),
  )
}
