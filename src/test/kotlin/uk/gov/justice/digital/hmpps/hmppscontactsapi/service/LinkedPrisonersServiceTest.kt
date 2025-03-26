package uk.gov.justice.digital.hmpps.hmppscontactsapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.Pageable
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.PrisonerContactSummaryEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.helpers.prisoner
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.LinkedPrisonerDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.PrisonerContactSummaryRepository

class LinkedPrisonersServiceTest {

  private val repo: PrisonerContactSummaryRepository = mock()
  private val prisonerService: PrisonerService = mock()
  private val service = LinkedPrisonersService(repo, prisonerService)
  private val contactId: Long = 123
  private val pageable = Pageable.unpaged()

  @Test
  fun `Should search for unique prisoner ids and join with multiple relationships even if to the same prisoner`() {
    whenever(repo.findByContactId(contactId)).thenReturn(
      listOf(
        // two relationships for A1234BC and one for X6789YZ
        prisonerContactEntity(999, "A1234BC", "S", "Social", "FRI", "Friend", true),
        prisonerContactEntity(888, "A1234BC", "O", "Official", "LAW", "Lawyer", false),
        prisonerContactEntity(777, "X6789YZ", "S", "Social", "FA", "Father", true),
      ),
    )
    whenever(
      prisonerService.getPrisoners(
        setOf(
          "A1234BC",
          "X6789YZ",
        ),
      ),
    ).thenReturn(
      listOf(
        prisoner(
          prisonerNumber = "A1234BC",
          firstName = "A",
          middleNames = "1234",
          lastName = "BC",
          prisonId = "BXI",
          prisonName = "Brixton (HMP)",
        ),
        prisoner(prisonerNumber = "X6789YZ", firstName = "X", middleNames = null, lastName = "YZ"),
      ),
    )

    val linkedPrisoners = service.getLinkedPrisoners(contactId, 0, 10)

    assertThat(linkedPrisoners.content).isEqualTo(
      listOf(
        LinkedPrisonerDetails(
          prisonerNumber = "A1234BC",
          firstName = "A",
          middleNames = "1234",
          lastName = "BC",
          prisonId = "BXI",
          prisonName = "Brixton (HMP)",
          prisonerContactId = 999,
          relationshipTypeCode = "S",
          relationshipTypeDescription = "Social",
          relationshipToPrisonerCode = "FRI",
          relationshipToPrisonerDescription = "Friend",
          isRelationshipActive = true,
        ),
        LinkedPrisonerDetails(
          prisonerNumber = "A1234BC",
          firstName = "A",
          middleNames = "1234",
          lastName = "BC",
          prisonId = "BXI",
          prisonName = "Brixton (HMP)",
          prisonerContactId = 888,
          relationshipTypeCode = "O",
          relationshipTypeDescription = "Official",
          relationshipToPrisonerCode = "LAW",
          relationshipToPrisonerDescription = "Lawyer",
          isRelationshipActive = false,
        ),
        LinkedPrisonerDetails(
          prisonerNumber = "X6789YZ",
          firstName = "X",
          middleNames = null,
          lastName = "YZ",
          prisonerContactId = 777,
          relationshipTypeCode = "S",
          relationshipTypeDescription = "Social",
          relationshipToPrisonerCode = "FA",
          relationshipToPrisonerDescription = "Father",
          isRelationshipActive = true,
        ),
      ),
    )

    verify(prisonerService, times(1)).getPrisoners(setOf("A1234BC", "X6789YZ"))
  }

  @Test
  fun `should include results even if they don't have a matching prisoner`() {
    whenever(repo.findByContactId(contactId)).thenReturn(
      listOf(
        prisonerContactEntity(999, "A1234BC", "S", "Social", "FRI", "Friend", true),
        prisonerContactEntity(777, "X6789YZ", "S", "Social", "FA", "Father", true),
      ),
    )
    whenever(
      prisonerService.getPrisoners(
        setOf(
          "A1234BC",
          "X6789YZ",
        ),
      ),
    ).thenReturn(listOf(prisoner(prisonerNumber = "A1234BC", firstName = "A", middleNames = "1234", lastName = "BC")))

    val linkedPrisoners = service.getLinkedPrisoners(contactId, 0, 10)

    assertThat(linkedPrisoners.content).isEqualTo(
      listOf(
        LinkedPrisonerDetails(
          prisonerNumber = "X6789YZ",
          firstName = null,
          middleNames = null,
          lastName = null,
          prisonerContactId = 777,
          relationshipTypeCode = "S",
          relationshipTypeDescription = "Social",
          relationshipToPrisonerCode = "FA",
          relationshipToPrisonerDescription = "Father",
          isRelationshipActive = true,
        ),
        LinkedPrisonerDetails(
          prisonerNumber = "A1234BC",
          firstName = "A",
          middleNames = "1234",
          lastName = "BC",
          prisonerContactId = 999,
          relationshipTypeCode = "S",
          relationshipTypeDescription = "Social",
          relationshipToPrisonerCode = "FRI",
          relationshipToPrisonerDescription = "Friend",
          isRelationshipActive = true,
        ),
      ),
    )

    verify(prisonerService, times(1)).getPrisoners(setOf("A1234BC", "X6789YZ"))
  }

  @Test
  fun `should work with more than 1000 linked prisoners by batching calls to prisoner search`() {
    val entities =
      (1..2000).map { id -> prisonerContactEntity(id.toLong(), "A${id}BC", "S", "Social", "FRI", "Friend", true) }
    val prisonersBatchOne = (1..1000).map { id ->
      prisoner(
        prisonerNumber = "A${id}BC",
        firstName = "A",
        middleNames = "1234",
        lastName = "BC",
      )
    }
    val prisonersBatchTwo = (1001..2000).map { id ->
      prisoner(
        prisonerNumber = "A${id}BC",
        firstName = "A",
        middleNames = "1234",
        lastName = "BC",
      )
    }
    whenever(repo.findByContactId(contactId)).thenReturn(entities)
    whenever(prisonerService.getPrisoners(any())).thenReturn(prisonersBatchOne).thenReturn(prisonersBatchTwo)

    val linkedPrisoners = service.getLinkedPrisoners(contactId, 0, 10000)
    assertThat(linkedPrisoners.content).hasSize(2000)
    // Check they all found a matching prisoner
    assertThat(linkedPrisoners.content).allMatch { it.firstName == "A" }
    verify(prisonerService, times(2)).getPrisoners(any())
  }

  @Test
  fun `should apply paging`() {
    val entities =
      (1..29).map { id -> prisonerContactEntity(id.toLong(), "A${id}BC", "S", "Social", "FRI", "Friend", true) }
    val prisonersBatchOne = (1..29).map { id ->
      prisoner(
        prisonerNumber = "A${id}BC",
        firstName = "A",
        middleNames = "1234",
        lastName = "BC",
      )
    }
    whenever(repo.findByContactId(contactId)).thenReturn(entities)
    whenever(prisonerService.getPrisoners(any())).thenReturn(prisonersBatchOne)

    val pageOne = service.getLinkedPrisoners(contactId, 0, 10)
    assertThat(pageOne.content).hasSize(10)
    assertThat(pageOne.metadata!!.totalElements).isEqualTo(29)
    assertThat(pageOne.metadata!!.totalPages).isEqualTo(3)
    assertThat(pageOne.metadata!!.number).isEqualTo(0)
    assertThat(pageOne.metadata!!.size).isEqualTo(10)

    val pageTwo = service.getLinkedPrisoners(contactId, 1, 10)
    assertThat(pageTwo.content).hasSize(10)
    assertThat(pageTwo.metadata!!.totalElements).isEqualTo(29)
    assertThat(pageTwo.metadata!!.totalPages).isEqualTo(3)
    assertThat(pageTwo.metadata!!.number).isEqualTo(1)
    assertThat(pageTwo.metadata!!.size).isEqualTo(10)

    val pageThree = service.getLinkedPrisoners(contactId, 2, 10)
    assertThat(pageThree.content).hasSize(9)
    assertThat(pageThree.metadata!!.totalElements).isEqualTo(29)
    assertThat(pageThree.metadata!!.totalPages).isEqualTo(3)
    assertThat(pageThree.metadata!!.number).isEqualTo(2)
    assertThat(pageThree.metadata!!.size).isEqualTo(10)

    val megaPage = service.getLinkedPrisoners(contactId, 0, 1000)
    assertThat(megaPage.content).hasSize(29)
    assertThat(megaPage.metadata!!.totalElements).isEqualTo(29)
    assertThat(megaPage.metadata!!.totalPages).isEqualTo(1)
    assertThat(megaPage.metadata!!.number).isEqualTo(0)
    assertThat(megaPage.metadata!!.size).isEqualTo(1000)
  }

  @Test
  fun `Should sort by prisoner name and then prisoner number`() {
    whenever(repo.findByContactId(contactId)).thenReturn(
      listOf(
        prisonerContactEntity(888, "D1234EF", "O", "Official", "LAW", "Lawyer", false),
        prisonerContactEntity(999, "A1234BC", "S", "Social", "FRI", "Friend", true),
        prisonerContactEntity(777, "X6789YZ", "S", "Social", "FA", "Father", true),
        prisonerContactEntity(1000, "A1235BC", "S", "Social", "FA", "Father", true),
      ),
    )
    val prisonerWithSameName = prisoner(
      prisonerNumber = "A1234BC",
      firstName = "A",
      middleNames = "1234",
      lastName = "BC",
      prisonId = "BXI",
      prisonName = "Brixton (HMP)",
    )
    whenever(
      prisonerService.getPrisoners(
        setOf(
          "A1234BC",
          "D1234EF",
          "X6789YZ",
          "A1235BC",
        ),
      ),
    ).thenReturn(
      listOf(
        prisonerWithSameName,
        prisonerWithSameName.copy(prisonerNumber = "X6789YZ"),
        prisonerWithSameName.copy(prisonerNumber = "A1235BC", middleNames = "1235"),
        prisoner(prisonerNumber = "D1234EF", firstName = "D", middleNames = null, lastName = "EF"),
        prisoner(prisonerNumber = "A1235BC", firstName = "D", middleNames = null, lastName = "EF"),
      ),
    )

    val linkedPrisoners = service.getLinkedPrisoners(contactId, 0, 10)

    assertThat(linkedPrisoners.content).extracting("prisonerNumber").isEqualTo(
      listOf("A1234BC", "X6789YZ", "A1235BC", "D1234EF"),
    )
  }

  private fun prisonerContactEntity(
    prisonerContactId: Long,
    prisonerNumber: String,
    contactType: String,
    contactTypeDescription: String,
    relationshipCode: String,
    relationshipDescription: String,
    active: Boolean,
  ): PrisonerContactSummaryEntity = PrisonerContactSummaryEntity(
    prisonerContactId,
    contactId = contactId,
    title = "Mr.",
    firstName = "First",
    middleNames = "Any",
    lastName = "Last",
    dateOfBirth = null,
    contactAddressId = 3L,
    flat = "2B",
    property = "123",
    street = "Baker Street",
    area = "Westminster",
    cityCode = "SHEF",
    cityDescription = "Sheffield",
    countyCode = "SYORKS",
    countyDescription = "South Yorkshire",
    postCode = "NW1 6XE",
    countryCode = "UK",
    countryDescription = "United Kingdom",
    primaryAddress = false,
    mailFlag = false,
    contactPhoneId = 4L,
    phoneType = "Mobile",
    phoneTypeDescription = "Mobile Phone",
    phoneNumber = "07123456789",
    extNumber = "0123",
    contactEmailId = 5L,
    emailAddress = "john.doe@example.com",
    prisonerNumber = prisonerNumber,
    relationshipToPrisoner = relationshipCode,
    relationshipToPrisonerDescription = relationshipDescription,
    active = active,
    approvedVisitor = true,
    nextOfKin = false,
    emergencyContact = false,
    currentTerm = true,
    comments = "No comments",
    relationshipType = contactType,
    relationshipTypeDescription = contactTypeDescription,
  )
}
