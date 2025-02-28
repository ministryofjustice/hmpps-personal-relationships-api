package uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.db

import org.apache.commons.lang3.RandomStringUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.util.UriComponentsBuilder
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.ContactAddressEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.integration.PostgresIntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.ContactRelationship
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.CreateContactRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.ContactAddressRepository
import java.time.LocalDate
import java.time.LocalDateTime

class MostRelevantAddressTest : PostgresIntegrationTestBase() {

  @Autowired
  private lateinit var addressRepository: ContactAddressRepository

  private val prisonerNumber = RandomStringUtils.secure().next(7, true, false)
  private val contactName = RandomStringUtils.secure().next(35, true, false)
  private var savedContactId = 0L

  @BeforeEach
  fun initialiseData() {
    stubPrisonSearchWithResponse(prisonerNumber)
    savedContactId = testAPIClient.createAContact(
      CreateContactRequest(
        lastName = contactName,
        firstName = "ThisIsFor",
        relationship = ContactRelationship(
          prisonerNumber = prisonerNumber,
          relationshipTypeCode = "S",
          relationshipToPrisonerCode = "FRI",
          isNextOfKin = false,
          isEmergencyContact = false,
          isApprovedVisitor = false,
          comments = null,
        ),
        createdBy = "created",
      ),
    ).id
  }

  @ParameterizedTest
  @MethodSource("addressCombinations")
  protected fun `should display most relevant address in contact list`(case: Case) {
    createAddresses(case)
    val prisonerContacts = testAPIClient.getPrisonerContacts(prisonerNumber)
    assertThat(prisonerContacts.content).hasSize(1)
    assertThat(prisonerContacts.content[0].flat).isEqualTo(case.expectedAddressIndex?.toString())
  }

  @ParameterizedTest
  @MethodSource("addressCombinations")
  protected fun `should display most relevant address in contact search`(case: Case) {
    createAddresses(case)

    val searchUrl = UriComponentsBuilder.fromPath("contact/search")
      .queryParam("lastName", contactName)
      .queryParam("firstName", "ThisIsFor")
      .build()
      .toUri()

    val contacts = testAPIClient.getSearchContactResults(searchUrl)!!
    assertThat(contacts.content).hasSize(1)
    assertThat(contacts.content[0].flat).isEqualTo(case.expectedAddressIndex?.toString())
  }

  private fun createAddresses(case: Case) = case.specs.map { spec ->
    val contactAddressEntity = addressRepository.saveAndFlush(
      ContactAddressEntity(
        contactAddressId = 0,
        contactId = savedContactId,
        addressType = "HOME",
        primaryAddress = spec.primary,
        flat = spec.index.toString(),
        property = "Property",
        street = "Street",
        area = "Area",
        cityCode = "CITY",
        countyCode = "COUNTY",
        postCode = "POSTCODE",
        countryCode = "COUNTRY",
        verified = false,
        mailFlag = spec.mail,
        startDate = spec.startDate,
        endDate = spec.endDate,
        noFixedAddress = false,
        comments = null,
        createdBy = "USER",
        createdTime = spec.created,
      ),
    )
    CreatedAddress(contactAddressEntity.contactAddressId, spec)
  }

  companion object {

    @JvmStatic
    fun addressCombinations(): List<Arguments> {
      val now = LocalDateTime.now()
      val primaryAddressIsNotLatest = Case(
        "Primary address is not latest",
        0,
        listOf(
          AddressSpec(
            index = 0,
            primary = true,
            mail = false,
            startDate = null,
            endDate = null,
            created = now.minusMinutes(10),
          ),
          AddressSpec(index = 1, primary = false, mail = false, startDate = null, endDate = null, created = now),
          AddressSpec(
            index = 2,
            primary = false,
            mail = false,
            startDate = null,
            endDate = null,
            created = now.plusMinutes(10),
          ),
        ),
      )

      val noPrimarySoUseMailAddress = Case(
        "No primary so use mail address",
        1,
        listOf(
          AddressSpec(
            index = 0,
            primary = false,
            mail = false,
            startDate = null,
            endDate = null,
            created = now.minusMinutes(10),
          ),
          AddressSpec(index = 1, primary = false, mail = true, startDate = null, endDate = null, created = now),
          AddressSpec(
            index = 2,
            primary = false,
            mail = false,
            startDate = null,
            endDate = null,
            created = now.plusMinutes(10),
          ),
        ),
      )

      val noPrimaryOrMailSoUseLatestFrom = Case(
        "No primary or mail address so use latest non-null startDate",
        1,
        listOf(
          AddressSpec(
            index = 0,
            primary = false,
            mail = false,
            startDate = null,
            endDate = null,
            created = now.minusMinutes(10),
          ),
          AddressSpec(
            index = 1,
            primary = false,
            mail = false,
            startDate = LocalDate.of(2020, 1, 2),
            endDate = null,
            created = now,
          ),
          AddressSpec(
            index = 2,
            primary = false,
            mail = false,
            startDate = LocalDate.of(2020, 1, 1),
            endDate = null,
            created = now.plusMinutes(10),
          ),
        ),
      )

      val noPrimaryOrMailOrFromSoUseMostRecentlyCreated = Case(
        "No primary or mail address or startDate so use latest created",
        2,
        listOf(
          AddressSpec(
            index = 0,
            primary = false,
            mail = false,
            startDate = null,
            endDate = null,
            created = now.minusMinutes(10),
          ),
          AddressSpec(index = 1, primary = false, mail = false, startDate = null, endDate = null, created = now),
          AddressSpec(
            index = 2,
            primary = false,
            mail = false,
            startDate = null,
            endDate = null,
            created = now.plusMinutes(10),
          ),
        ),
      )

      val excludeEndDatedAddresses = Case(
        "Exclude end dated addresses",
        null,
        listOf(
          AddressSpec(
            index = 0,
            primary = true,
            mail = false,
            startDate = null,
            endDate = LocalDate.of(2024, 1, 1),
            created = now.minusMinutes(10),
          ),
          AddressSpec(index = 1, primary = false, mail = true, startDate = null, endDate = LocalDate.of(2024, 1, 1), created = now),
          AddressSpec(
            index = 2,
            primary = false,
            mail = false,
            startDate = LocalDate.of(2020, 1, 1),
            endDate = LocalDate.of(2024, 1, 1),
            created = now.plusMinutes(10),
          ),
        ),
      )

      return listOf(
        primaryAddressIsNotLatest,
        noPrimarySoUseMailAddress,
        noPrimaryOrMailSoUseLatestFrom,
        noPrimaryOrMailOrFromSoUseMostRecentlyCreated,
        excludeEndDatedAddresses,
      ).map { Arguments.of(it) }
    }
  }

  protected data class Case(val description: String, val expectedAddressIndex: Int?, val specs: List<AddressSpec>)
  protected data class AddressSpec(
    val index: Int,
    val primary: Boolean,
    val mail: Boolean,
    val startDate: LocalDate?,
    val endDate: LocalDate?,
    val created: LocalDateTime,
  )

  protected data class CreatedAddress(val contactAddressId: Long, val spec: AddressSpec)
}
