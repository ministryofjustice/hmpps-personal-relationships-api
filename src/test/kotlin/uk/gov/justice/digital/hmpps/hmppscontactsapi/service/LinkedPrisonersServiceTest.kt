package uk.gov.justice.digital.hmpps.hmppscontactsapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.PrisonerContactSummaryEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.helpers.prisoner
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.LinkedPrisonerDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.LinkedPrisonerRelationshipDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.PrisonerContactSummaryRepository

class LinkedPrisonersServiceTest {

  private val repo: PrisonerContactSummaryRepository = mock()
  private val prisonerService: PrisonerService = mock()
  private val service = LinkedPrisonersService(repo, prisonerService)
  private val contactId: Long = 123

  @Test
  fun `should call search service only once per prisoner even if multiple relationships`() {
    whenever(repo.findByContactIdAndActive(contactId, true)).thenReturn(
      listOf(
        // two relationships for A1234BC and one for X6789YZ
        prisonerContactEntity(999, "A1234BC", "S", "Social", "FRI", "Friend"),
        prisonerContactEntity(888, "A1234BC", "O", "Official", "LAW", "Lawyer"),
        prisonerContactEntity(777, "X6789YZ", "S", "Social", "FA", "Father"),
      ),
    )
    whenever(prisonerService.getPrisoner("A1234BC")).thenReturn(prisoner(prisonerNumber = "A1234BC", firstName = "A", middleNames = "1234", lastName = "BC", prisonId = "BXI", prisonName = "Brixton (HMP)"))
    whenever(prisonerService.getPrisoner("X6789YZ")).thenReturn(prisoner(prisonerNumber = "X6789YZ", firstName = "X", middleNames = null, lastName = "YZ"))

    val linkedPrisoners = service.getLinkedPrisoners(contactId)

    assertThat(linkedPrisoners).isEqualTo(
      listOf(
        LinkedPrisonerDetails(
          prisonerNumber = "A1234BC",
          firstName = "A",
          middleNames = "1234",
          lastName = "BC",
          prisonId = "BXI",
          prisonName = "Brixton (HMP)",
          relationships = listOf(
            LinkedPrisonerRelationshipDetails(
              prisonerContactId = 999,
              relationshipTypeCode = "S",
              relationshipTypeDescription = "Social",
              relationshipToPrisonerCode = "FRI",
              relationshipToPrisonerDescription = "Friend",
            ),
            LinkedPrisonerRelationshipDetails(
              prisonerContactId = 888,
              relationshipTypeCode = "O",
              relationshipTypeDescription = "Official",
              relationshipToPrisonerCode = "LAW",
              relationshipToPrisonerDescription = "Lawyer",
            ),
          ),
        ),
        LinkedPrisonerDetails(
          prisonerNumber = "X6789YZ",
          firstName = "X",
          middleNames = null,
          lastName = "YZ",
          relationships = listOf(

            LinkedPrisonerRelationshipDetails(
              prisonerContactId = 777,
              relationshipTypeCode = "S",
              relationshipTypeDescription = "Social",
              relationshipToPrisonerCode = "FA",
              relationshipToPrisonerDescription = "Father",
            ),
          ),
        ),
      ),
    )

    verify(prisonerService, times(1)).getPrisoner("A1234BC")
    verify(prisonerService, times(1)).getPrisoner("X6789YZ")
  }

  @Test
  fun `should exclude results if they don't have a matching prisoner`() {
    whenever(repo.findByContactIdAndActive(contactId, true)).thenReturn(
      listOf(
        // two relationships for A1234BC and one for X6789YZ
        prisonerContactEntity(999, "A1234BC", "S", "Social", "FRI", "Friend"),
        prisonerContactEntity(777, "X6789YZ", "S", "Social", "FA", "Father"),
      ),
    )
    whenever(prisonerService.getPrisoner("A1234BC")).thenReturn(prisoner(prisonerNumber = "A1234BC", firstName = "A", middleNames = "1234", lastName = "BC"))
    whenever(prisonerService.getPrisoner("X6789YZ")).thenReturn(null)

    val linkedPrisoners = service.getLinkedPrisoners(contactId)

    assertThat(linkedPrisoners).isEqualTo(
      listOf(
        LinkedPrisonerDetails(
          prisonerNumber = "A1234BC",
          firstName = "A",
          middleNames = "1234",
          lastName = "BC",
          relationships = listOf(
            LinkedPrisonerRelationshipDetails(
              prisonerContactId = 999,
              relationshipTypeCode = "S",
              relationshipTypeDescription = "Social",
              relationshipToPrisonerCode = "FRI",
              relationshipToPrisonerDescription = "Friend",
            ),
          ),
        ),
      ),
    )

    verify(prisonerService, times(1)).getPrisoner("A1234BC")
    verify(prisonerService, times(1)).getPrisoner("X6789YZ")
  }

  private fun prisonerContactEntity(
    prisonerContactId: Long,
    prisonerNumber: String,
    contactType: String,
    contactTypeDescription: String,
    relationshipCode: String,
    relationshipDescription: String,
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
    active = true,
    approvedVisitor = true,
    nextOfKin = false,
    emergencyContact = false,
    currentTerm = true,
    comments = "No comments",
    relationshipType = contactType,
    relationshipTypeDescription = contactTypeDescription,
  )
}
