package uk.gov.justice.digital.hmpps.hmppscontactsapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
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
    whenever(repo.findByContactId(contactId, pageable)).thenReturn(
      PageImpl(
        listOf(
          // two relationships for A1234BC and one for X6789YZ
          prisonerContactEntity(999, "A1234BC", "S", "Social", "FRI", "Friend", true),
          prisonerContactEntity(888, "A1234BC", "O", "Official", "LAW", "Lawyer", false),
          prisonerContactEntity(777, "X6789YZ", "S", "Social", "FA", "Father", true),
        ),
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

    val linkedPrisoners = service.getLinkedPrisoners(contactId, pageable)

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
    whenever(repo.findByContactId(contactId, pageable)).thenReturn(
      PageImpl(
        listOf(
          prisonerContactEntity(999, "A1234BC", "S", "Social", "FRI", "Friend", true),
          prisonerContactEntity(777, "X6789YZ", "S", "Social", "FA", "Father", true),
        ),
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

    val linkedPrisoners = service.getLinkedPrisoners(contactId, pageable)

    assertThat(linkedPrisoners.content).isEqualTo(
      listOf(
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
      ),
    )

    verify(prisonerService, times(1)).getPrisoners(setOf("A1234BC", "X6789YZ"))
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
