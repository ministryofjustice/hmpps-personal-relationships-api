package uk.gov.justice.digital.hmpps.hmppscontactsapi.service

import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppscontactsapi.client.manage.users.UserDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.ContactEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.ContactRestrictionEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.PrisonerContactEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.PrisonerContactRestrictionEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.helpers.aUser
import uk.gov.justice.digital.hmpps.hmppscontactsapi.helpers.createContactRestrictionDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.helpers.createContactRestrictionDetailsEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.helpers.createPrisonerContactRestrictionDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.helpers.createPrisonerContactRestrictionDetailsEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.ReferenceCodeGroup
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.restrictions.CreateContactRestrictionRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.restrictions.CreatePrisonerContactRestrictionRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.restrictions.UpdateContactRestrictionRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.restrictions.UpdatePrisonerContactRestrictionRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.ContactRestrictionDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.PrisonerContactRestrictionDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.PrisonerContactRestrictionsResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.ReferenceCode
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.ContactRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.ContactRestrictionDetailsRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.ContactRestrictionRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.PrisonerContactRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.PrisonerContactRestrictionDetailsRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.PrisonerContactRestrictionRepository
import java.time.LocalDate
import java.time.LocalDateTime.now
import java.util.*

class RestrictionsServiceTest {

  private val contactId = 99L
  private val prisonerContactId = 66L
  private val aContact = ContactEntity(
    contactId = contactId,
    title = "MR",
    lastName = "last",
    middleNames = "middle",
    firstName = "first",
    dateOfBirth = null,
    deceasedDate = null,
    createdBy = "user",
    createdTime = now(),
  )
  private val aPrisonerContact = PrisonerContactEntity(
    prisonerContactId = prisonerContactId,
    contactId = contactId,
    prisonerNumber = "A1234BC",
    relationshipType = "S",
    relationshipToPrisoner = "FRI",
    nextOfKin = true,
    emergencyContact = true,
    active = true,
    approvedVisitor = true,
    currentTerm = true,
    comments = null,
    createdBy = "user",
    createdTime = now(),
  )

  private val contactRestrictionDetailsRepository: ContactRestrictionDetailsRepository = mock()
  private val contactRestrictionRepository: ContactRestrictionRepository = mock()
  private val contactRepository: ContactRepository = mock()
  private val prisonerContactRepository: PrisonerContactRepository = mock()
  private val prisonerContactRestrictionDetailsRepository: PrisonerContactRestrictionDetailsRepository = mock()
  private val prisonerContactRestrictionRepository: PrisonerContactRestrictionRepository = mock()
  private val referenceCodeService: ReferenceCodeService = mock()
  private val manageUsersService: ManageUsersService = mock()
  private val service = RestrictionsService(
    contactRestrictionDetailsRepository,
    contactRestrictionRepository,
    contactRepository,
    prisonerContactRepository,
    prisonerContactRestrictionDetailsRepository,
    prisonerContactRestrictionRepository,
    referenceCodeService,
    manageUsersService,
  )

  @Nested
  inner class GetGlobalRestrictions {
    @Test
    fun `get global restrictions successfully`() {
      val now = now()
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(aContact))
      whenever(manageUsersService.getUserByUsername("created")).thenReturn(UserDetails("created", "Created User"))
      whenever(manageUsersService.getUserByUsername("updated")).thenReturn(UserDetails("updated", "Updated User"))
      whenever(contactRestrictionDetailsRepository.findAllByContactId(contactId)).thenReturn(
        listOf(
          createContactRestrictionDetailsEntity(
            123,
            createdTime = now,
            createdBy = "created",
            updatedBy = null,
          ),
          createContactRestrictionDetailsEntity(
            321,
            createdTime = now,
            createdBy = "created",
            updatedBy = "updated",
          ),
        ),
      )

      val restrictions = service.getGlobalRestrictionsForContact(contactId)

      assertThat(restrictions).hasSize(2)
      assertThat(restrictions).isEqualTo(
        listOf(
          createContactRestrictionDetails(
            123,
            createdTime = now,
            createdBy = "created",
            updatedBy = null,
            enteredByUsername = "created",
            enteredByDisplayName = "Created User",
          ),
          createContactRestrictionDetails(
            321,
            createdTime = now,
            createdBy = "created",
            updatedBy = "updated",
            enteredByUsername = "updated",
            enteredByDisplayName = "Updated User",
          ),
        ),
      )
    }

    @Test
    fun `get default to username if entered by display name not found`() {
      val now = now()
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(aContact))
      whenever(manageUsersService.getUserByUsername("created")).thenReturn(null)
      whenever(manageUsersService.getUserByUsername("updated")).thenReturn(null)
      whenever(contactRestrictionDetailsRepository.findAllByContactId(contactId)).thenReturn(
        listOf(
          createContactRestrictionDetailsEntity(
            123,
            createdTime = now,
            createdBy = "created",
            updatedBy = null,
          ),
          createContactRestrictionDetailsEntity(
            321,
            createdTime = now,
            createdBy = "created",
            updatedBy = "updated",
          ),
        ),
      )

      val restrictions = service.getGlobalRestrictionsForContact(contactId)

      assertThat(restrictions).hasSize(2)
      assertThat(restrictions).isEqualTo(
        listOf(
          createContactRestrictionDetails(
            123,
            createdTime = now,
            createdBy = "created",
            updatedBy = null,
            enteredByUsername = "created",
            enteredByDisplayName = "created",
          ),
          createContactRestrictionDetails(
            321,
            createdTime = now,
            createdBy = "created",
            updatedBy = "updated",
            enteredByUsername = "updated",
            enteredByDisplayName = "updated",
          ),
        ),
      )
    }

    @Test
    fun `only lookup once if same username is required multiple times`() {
      val now = now()
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(aContact))
      whenever(manageUsersService.getUserByUsername("created")).thenReturn(UserDetails("created", "Created User"))
      whenever(contactRestrictionDetailsRepository.findAllByContactId(contactId)).thenReturn(
        listOf(
          createContactRestrictionDetailsEntity(
            123,
            createdTime = now,
            createdBy = "created",
            updatedBy = null,
          ),
          createContactRestrictionDetailsEntity(
            321,
            createdTime = now,
            createdBy = "created",
            updatedBy = null,
          ),
        ),
      )

      val restrictions = service.getGlobalRestrictionsForContact(contactId)

      assertThat(restrictions).hasSize(2)
      assertThat(restrictions).isEqualTo(
        listOf(
          createContactRestrictionDetails(
            123,
            createdTime = now,
            createdBy = "created",
            updatedBy = null,
            enteredByUsername = "created",
            enteredByDisplayName = "Created User",
          ),
          createContactRestrictionDetails(
            321,
            createdTime = now,
            createdBy = "created",
            updatedBy = null,
            enteredByUsername = "created",
            enteredByDisplayName = "Created User",
          ),
        ),
      )
      verify(manageUsersService, times(1)).getUserByUsername("created")
    }

    @Test
    fun `should blow up if contact is not found getting global restrictions`() {
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.empty())

      val exception = assertThrows<EntityNotFoundException> {
        service.getGlobalRestrictionsForContact(contactId)
      }
      assertThat(exception.message).isEqualTo("Contact (99) could not be found")
    }
  }

  @Nested
  inner class GetPrisonerContact {
    @Test
    fun `get prisoner contact restrictions successfully`() {
      val now = now()
      whenever(prisonerContactRepository.findById(prisonerContactId)).thenReturn(Optional.of(aPrisonerContact))
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(aContact))
      whenever(manageUsersService.getUserByUsername("created_global")).thenReturn(
        UserDetails(
          "created_global",
          "Created User Global",
        ),
      )
      whenever(manageUsersService.getUserByUsername("updated_global")).thenReturn(
        UserDetails(
          "updated_global",
          "Updated User Global",
        ),
      )
      whenever(manageUsersService.getUserByUsername("created_pc")).thenReturn(UserDetails("created_pc", "Created PC"))
      whenever(manageUsersService.getUserByUsername("updated_pc")).thenReturn(UserDetails("updated_pc", "Updated PC"))
      whenever(contactRestrictionDetailsRepository.findAllByContactId(contactId)).thenReturn(
        listOf(
          createContactRestrictionDetailsEntity(
            123,
            createdTime = now,
            createdBy = "created_global",
            updatedBy = null,
          ),
          createContactRestrictionDetailsEntity(
            321,
            createdTime = now,
            createdBy = "created_global",
            updatedBy = "updated_global",
          ),
        ),
      )
      whenever(prisonerContactRestrictionDetailsRepository.findAllByPrisonerContactId(prisonerContactId)).thenReturn(
        listOf(
          createPrisonerContactRestrictionDetailsEntity(
            id = 789,
            prisonerContactId = prisonerContactId,
            createdTime = now,
            createdBy = "created_pc",
            updatedBy = null,
          ),
          createPrisonerContactRestrictionDetailsEntity(
            id = 987,
            prisonerContactId = prisonerContactId,
            createdTime = now,
            createdBy = "created_pc",
            updatedBy = "updated_pc",
          ),
        ),
      )

      val restrictions = service.getPrisonerContactRestrictions(prisonerContactId)

      assertThat(restrictions).isEqualTo(
        PrisonerContactRestrictionsResponse(
          prisonerContactRestrictions = listOf(
            createPrisonerContactRestrictionDetails(
              id = 789,
              prisonerContactId = prisonerContactId,
              contactId = contactId,
              prisonerNumber = aPrisonerContact.prisonerNumber,
              createdTime = now,
              createdBy = "created_pc",
              updatedBy = null,
              enteredByUsername = "created_pc",
              enteredByDisplayName = "Created PC",
            ),
            createPrisonerContactRestrictionDetails(
              id = 987,
              prisonerContactId = prisonerContactId,
              contactId = contactId,
              prisonerNumber = aPrisonerContact.prisonerNumber,
              createdTime = now,
              createdBy = "created_pc",
              updatedBy = "updated_pc",
              enteredByUsername = "updated_pc",
              enteredByDisplayName = "Updated PC",
            ),
          ),
          contactGlobalRestrictions = listOf(
            createContactRestrictionDetails(
              id = 123,
              createdTime = now,
              createdBy = "created_global",
              updatedBy = null,
              enteredByUsername = "created_global",
              enteredByDisplayName = "Created User Global",
            ),
            createContactRestrictionDetails(
              id = 321,
              createdTime = now,
              createdBy = "created_global",
              updatedBy = "updated_global",
              enteredByUsername = "updated_global",
              enteredByDisplayName = "Updated User Global",
            ),
          ),
        ),
      )
    }

    @Test
    fun `only lookup usernames once`() {
      val now = now()
      whenever(prisonerContactRepository.findById(prisonerContactId)).thenReturn(Optional.of(aPrisonerContact))
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(aContact))
      whenever(manageUsersService.getUserByUsername("created_pc")).thenReturn(UserDetails("created_pc", "Created PC"))
      whenever(contactRestrictionDetailsRepository.findAllByContactId(contactId)).thenReturn(emptyList())
      whenever(prisonerContactRestrictionDetailsRepository.findAllByPrisonerContactId(prisonerContactId)).thenReturn(
        listOf(
          createPrisonerContactRestrictionDetailsEntity(
            id = 789,
            prisonerContactId = prisonerContactId,
            createdTime = now,
            createdBy = "created_pc",
            updatedBy = null,
          ),
          createPrisonerContactRestrictionDetailsEntity(
            id = 987,
            prisonerContactId = prisonerContactId,
            createdTime = now,
            createdBy = "created_pc",
            updatedBy = null,
          ),
        ),
      )

      val restrictions = service.getPrisonerContactRestrictions(prisonerContactId)

      assertThat(restrictions).isEqualTo(
        PrisonerContactRestrictionsResponse(
          prisonerContactRestrictions = listOf(
            createPrisonerContactRestrictionDetails(
              id = 789,
              prisonerContactId = prisonerContactId,
              contactId = contactId,
              prisonerNumber = aPrisonerContact.prisonerNumber,
              createdTime = now,
              createdBy = "created_pc",
              updatedBy = null,
              enteredByUsername = "created_pc",
              enteredByDisplayName = "Created PC",
            ),
            createPrisonerContactRestrictionDetails(
              id = 987,
              prisonerContactId = prisonerContactId,
              contactId = contactId,
              prisonerNumber = aPrisonerContact.prisonerNumber,
              createdTime = now,
              createdBy = "created_pc",
              updatedBy = null,
              enteredByUsername = "created_pc",
              enteredByDisplayName = "Created PC",
            ),
          ),
          contactGlobalRestrictions = emptyList(),
        ),
      )
      verify(manageUsersService, times(1)).getUserByUsername("created_pc")
    }

    @Test
    fun `should blow up if prisoner contact is not found getting prisoner contact restrictions`() {
      whenever(prisonerContactRepository.findById(prisonerContactId)).thenReturn(Optional.empty())

      val exception = assertThrows<EntityNotFoundException> {
        service.getPrisonerContactRestrictions(prisonerContactId)
      }
      assertThat(exception.message).isEqualTo("Prisoner contact (66) could not be found")
    }
  }

  @Nested
  inner class CreateGlobal {

    private val user = aUser("created")

    @Test
    fun `blow up creating global restriction if contact is missing`() {
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.empty())

      val exception = assertThrows<EntityNotFoundException> {
        service.createContactGlobalRestriction(contactId, aCreateGlobalRestrictionRequest(), user)
      }
      assertThat(exception.message).isEqualTo("Contact (99) could not be found")
    }

    @Test
    fun `Validation exception when creating global restriction if contact expiry date is before start date`() {
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(aContact))

      val error = assertThrows<ValidationException> {
        service.createContactGlobalRestriction(
          contactId,
          aCreateGlobalRestrictionRequest(
            startDate = LocalDate.of(2022, 2, 2),
            expiryDate = LocalDate.of(2020, 1, 1),
          ),
          user,
        )
      }
      error.message isEqualTo "Restriction start date should be before the restriction end date"
    }

    @Test
    fun `blow up creating global restriction if type is invalid`() {
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(aContact))
      val expectedException = ValidationException("Invalid")
      whenever(
        referenceCodeService.validateReferenceCode(
          ReferenceCodeGroup.RESTRICTION,
          "BAN",
          allowInactive = false,
        ),
      ).thenThrow(expectedException)

      val exception = assertThrows<ValidationException> {
        service.createContactGlobalRestriction(contactId, aCreateGlobalRestrictionRequest(), user)
      }
      assertThat(exception).isEqualTo(expectedException)
      verify(referenceCodeService).validateReferenceCode(ReferenceCodeGroup.RESTRICTION, "BAN", allowInactive = false)
    }

    @Test
    fun `create global restriction`() {
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(aContact))
      whenever(manageUsersService.getUserByUsername("created")).thenReturn(UserDetails("created", "Created User"))
      whenever(
        referenceCodeService.validateReferenceCode(
          ReferenceCodeGroup.RESTRICTION,
          "BAN",
          allowInactive = false,
        ),
      ).thenReturn(
        ReferenceCode(
          referenceCodeId = 0,
          ReferenceCodeGroup.RESTRICTION,
          "BAN",
          "Banned",
          99,
          true,
        ),
      )
      whenever(contactRestrictionRepository.saveAndFlush(any())).thenAnswer { i ->
        (i.arguments[0] as ContactRestrictionEntity).copy(
          contactRestrictionId = 9999,
        )
      }

      val created = service.createContactGlobalRestriction(contactId, aCreateGlobalRestrictionRequest(), user)
      assertThat(created).isEqualTo(
        ContactRestrictionDetails(
          contactRestrictionId = 9999,
          contactId = contactId,
          restrictionType = "BAN",
          restrictionTypeDescription = "Banned",
          startDate = LocalDate.of(2020, 1, 1),
          expiryDate = LocalDate.of(2022, 2, 2),
          comments = "Some comments",
          enteredByUsername = "created",
          enteredByDisplayName = "Created User",
          createdBy = "created",
          createdTime = created.createdTime,
          updatedBy = null,
          updatedTime = null,
        ),
      )
      verify(contactRestrictionRepository).saveAndFlush(any())
      verify(referenceCodeService).validateReferenceCode(ReferenceCodeGroup.RESTRICTION, "BAN", allowInactive = false)
    }

    private fun aCreateGlobalRestrictionRequest(
      startDate: LocalDate = LocalDate.of(2020, 1, 1),
      expiryDate: LocalDate? = LocalDate.of(2022, 2, 2),
    ): CreateContactRestrictionRequest = CreateContactRestrictionRequest(
      restrictionType = "BAN",
      startDate = startDate,
      expiryDate = expiryDate,
      comments = "Some comments",
    )
  }

  @Nested
  inner class UpdateGlobalRestriction {
    private val contactRestrictionId = 654L
    private val existingEntity = ContactRestrictionEntity(
      contactRestrictionId = contactRestrictionId,
      contactId = contactId,
      restrictionType = "BAN",
      startDate = LocalDate.of(2020, 1, 1),
      expiryDate = LocalDate.of(2022, 2, 2),
      comments = "Some comments",
      createdBy = "created",
      createdTime = now(),
      updatedBy = null,
      updatedTime = null,
    )
    private val user = aUser("updated")

    @Test
    fun `blow up updating global restriction if contact is missing`() {
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.empty())

      val exception = assertThrows<EntityNotFoundException> {
        service.updateContactGlobalRestriction(
          contactId,
          contactRestrictionId,
          anUpdateGlobalRestrictionRequest(),
          user,
        )
      }
      assertThat(exception.message).isEqualTo("Contact (99) could not be found")
    }

    @Test
    fun `Validation exception when updating global restriction if contact expiry date is before start date`() {
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(aContact))

      val error = assertThrows<ValidationException> {
        service.updateContactGlobalRestriction(
          contactId,
          contactRestrictionId,
          anUpdateGlobalRestrictionRequest(
            startDate = LocalDate.of(1992, 2, 2),
            expiryDate = LocalDate.of(1990, 1, 1),
          ),
          user,
        )
      }
      error.message isEqualTo "Restriction start date should be before the restriction end date"
    }

    @Test
    fun `blow up updating global restriction if contact restriction is missing`() {
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(aContact))
      whenever(contactRestrictionRepository.findById(contactRestrictionId)).thenReturn(Optional.empty())

      val exception = assertThrows<EntityNotFoundException> {
        service.updateContactGlobalRestriction(
          contactId,
          contactRestrictionId,
          anUpdateGlobalRestrictionRequest(),
          user,
        )
      }
      assertThat(exception.message).isEqualTo("Contact restriction (654) could not be found")
    }

    @Test
    fun `blow up updating global restriction if type is invalid`() {
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(aContact))
      whenever(contactRestrictionRepository.findById(contactRestrictionId)).thenReturn(Optional.of(existingEntity))
      val expectedException = ValidationException("Invalid")
      whenever(referenceCodeService.validateReferenceCode(ReferenceCodeGroup.RESTRICTION, "CCTV", allowInactive = true)).thenThrow(expectedException)

      val exception = assertThrows<ValidationException> {
        service.updateContactGlobalRestriction(
          contactId,
          contactRestrictionId,
          anUpdateGlobalRestrictionRequest(),
          user,
        )
      }
      assertThat(exception).isEqualTo(expectedException)
      verify(referenceCodeService).validateReferenceCode(ReferenceCodeGroup.RESTRICTION, "CCTV", allowInactive = true)
    }

    @Test
    fun `updated global restriction`() {
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(aContact))
      whenever(contactRestrictionRepository.findById(contactRestrictionId)).thenReturn(Optional.of(existingEntity))
      whenever(manageUsersService.getUserByUsername("updated")).thenReturn(UserDetails("updated", "Updated User"))
      whenever(referenceCodeService.validateReferenceCode(ReferenceCodeGroup.RESTRICTION, "CCTV", allowInactive = true)).thenReturn(
        ReferenceCode(
          referenceCodeId = 0,
          ReferenceCodeGroup.RESTRICTION,
          "CCTV",
          "CCTV",
          99,
          true,
        ),
      )
      whenever(contactRestrictionRepository.saveAndFlush(any())).thenAnswer { i ->
        (i.arguments[0] as ContactRestrictionEntity).copy(
          contactRestrictionId = 9999,
        )
      }

      val updated = service.updateContactGlobalRestriction(
        contactId,
        contactRestrictionId,
        anUpdateGlobalRestrictionRequest(),
        user,
      )
      assertThat(updated).isEqualTo(
        ContactRestrictionDetails(
          contactRestrictionId = 9999,
          contactId = contactId,
          restrictionType = "CCTV",
          restrictionTypeDescription = "CCTV",
          startDate = LocalDate.of(1990, 1, 1),
          expiryDate = LocalDate.of(1992, 2, 2),
          comments = "Updated comments",
          enteredByUsername = "updated",
          enteredByDisplayName = "Updated User",
          createdBy = "created",
          createdTime = updated.createdTime,
          updatedBy = "updated",
          updatedTime = updated.updatedTime,
        ),
      )
      verify(contactRestrictionRepository).saveAndFlush(any())
      verify(referenceCodeService).validateReferenceCode(ReferenceCodeGroup.RESTRICTION, "CCTV", allowInactive = true)
    }

    private fun anUpdateGlobalRestrictionRequest(
      startDate: LocalDate = LocalDate.of(1990, 1, 1),
      expiryDate: LocalDate? = LocalDate.of(1992, 2, 2),
    ): UpdateContactRestrictionRequest = UpdateContactRestrictionRequest(
      restrictionType = "CCTV",
      startDate = startDate,
      expiryDate = expiryDate,
      comments = "Updated comments",
    )
  }

  @Nested
  inner class CreatePrisonerContactRestriction {
    private val user = aUser("created")

    @Test
    fun `blow up creating prisoner contact restriction if prisoner contact is missing`() {
      whenever(prisonerContactRepository.findById(prisonerContactId)).thenReturn(Optional.empty())

      val exception = assertThrows<EntityNotFoundException> {
        service.createPrisonerContactRestriction(prisonerContactId, aCreatePrisonerContactRestrictionRequest(), user)
      }
      assertThat(exception.message).isEqualTo("Prisoner contact (66) could not be found")
    }

    @Test
    fun `blow up creating prisoner contact restriction if type is invalid`() {
      whenever(prisonerContactRepository.findById(prisonerContactId)).thenReturn(Optional.of(aPrisonerContact))
      val expectedException = ValidationException("Invalid")
      whenever(
        referenceCodeService.validateReferenceCode(
          ReferenceCodeGroup.RESTRICTION,
          "BAN",
          allowInactive = false,
        ),
      ).thenThrow(expectedException)

      val exception = assertThrows<ValidationException> {
        service.createPrisonerContactRestriction(prisonerContactId, aCreatePrisonerContactRestrictionRequest(), user)
      }
      assertThat(exception).isEqualTo(expectedException)
      verify(referenceCodeService).validateReferenceCode(ReferenceCodeGroup.RESTRICTION, "BAN", allowInactive = false)
    }

    @Test
    fun `create prisoner contact restriction`() {
      whenever(manageUsersService.getUserByUsername("created")).thenReturn(UserDetails("created", "Created User"))
      whenever(prisonerContactRepository.findById(prisonerContactId)).thenReturn(Optional.of(aPrisonerContact))
      whenever(
        referenceCodeService.validateReferenceCode(
          ReferenceCodeGroup.RESTRICTION,
          "BAN",
          allowInactive = false,
        ),
      ).thenReturn(
        ReferenceCode(
          referenceCodeId = 0,
          ReferenceCodeGroup.RESTRICTION,
          "BAN",
          "Banned",
          99,
          true,
        ),
      )
      whenever(prisonerContactRestrictionRepository.saveAndFlush(any())).thenAnswer { i ->
        (i.arguments[0] as PrisonerContactRestrictionEntity).copy(
          prisonerContactRestrictionId = 9999,
        )
      }

      val created =
        service.createPrisonerContactRestriction(prisonerContactId, aCreatePrisonerContactRestrictionRequest(), user)
      assertThat(created).isEqualTo(
        PrisonerContactRestrictionDetails(
          prisonerContactRestrictionId = 9999,
          contactId = contactId,
          prisonerContactId = prisonerContactId,
          prisonerNumber = aPrisonerContact.prisonerNumber,
          restrictionType = "BAN",
          restrictionTypeDescription = "Banned",
          startDate = LocalDate.of(2020, 1, 1),
          expiryDate = LocalDate.of(2022, 2, 2),
          comments = "Some comments",
          enteredByUsername = "created",
          enteredByDisplayName = "Created User",
          createdBy = "created",
          createdTime = created.createdTime,
          updatedBy = null,
          updatedTime = null,
        ),
      )
      verify(prisonerContactRestrictionRepository).saveAndFlush(any())
      verify(referenceCodeService).validateReferenceCode(ReferenceCodeGroup.RESTRICTION, "BAN", allowInactive = false)
    }

    private fun aCreatePrisonerContactRestrictionRequest(): CreatePrisonerContactRestrictionRequest = CreatePrisonerContactRestrictionRequest(
      restrictionType = "BAN",
      startDate = LocalDate.of(2020, 1, 1),
      expiryDate = LocalDate.of(2022, 2, 2),
      comments = "Some comments",
    )
  }

  @Nested
  inner class UpdatePrisonerContactRestriction {
    private val prisonerContactRestrictionId = 654L
    private val existingEntity = PrisonerContactRestrictionEntity(
      prisonerContactRestrictionId = prisonerContactRestrictionId,
      prisonerContactId = prisonerContactId,
      restrictionType = "BAN",
      startDate = LocalDate.of(2020, 1, 1),
      expiryDate = LocalDate.of(2022, 2, 2),
      comments = "Some comments",
      createdBy = "created",
      createdTime = now(),
      updatedBy = null,
      updatedTime = null,
    )
    private val user = aUser("updated")

    @Test
    fun `blow up updating prisoner contact restriction if prisoner contact is missing`() {
      whenever(prisonerContactRepository.findById(prisonerContactId)).thenReturn(Optional.empty())

      val exception = assertThrows<EntityNotFoundException> {
        service.updatePrisonerContactRestriction(
          prisonerContactId,
          prisonerContactRestrictionId,
          anUpdatePrisonerContactRestrictionRequest(),
          user,
        )
      }
      assertThat(exception.message).isEqualTo("Prisoner contact (66) could not be found")
    }

    @Test
    fun `blow up updating prisoner contact restriction if prisoner contact restriction is missing`() {
      whenever(prisonerContactRepository.findById(prisonerContactId)).thenReturn(Optional.of(aPrisonerContact))
      whenever(prisonerContactRestrictionRepository.findById(prisonerContactRestrictionId)).thenReturn(Optional.empty())

      val exception = assertThrows<EntityNotFoundException> {
        service.updatePrisonerContactRestriction(
          prisonerContactId,
          prisonerContactRestrictionId,
          anUpdatePrisonerContactRestrictionRequest(),
          user,
        )
      }
      assertThat(exception.message).isEqualTo("Prisoner contact restriction (654) could not be found")
    }

    @Test
    fun `blow up updating prisoner contact restriction if type is invalid`() {
      whenever(prisonerContactRepository.findById(prisonerContactId)).thenReturn(Optional.of(aPrisonerContact))
      whenever(prisonerContactRestrictionRepository.findById(prisonerContactRestrictionId)).thenReturn(
        Optional.of(
          existingEntity,
        ),
      )
      val expectedException = ValidationException("Invalid")
      whenever(referenceCodeService.validateReferenceCode(ReferenceCodeGroup.RESTRICTION, "CCTV", allowInactive = true)).thenThrow(expectedException)

      val exception = assertThrows<ValidationException> {
        service.updatePrisonerContactRestriction(
          prisonerContactId,
          prisonerContactRestrictionId,
          anUpdatePrisonerContactRestrictionRequest(),
          user,
        )
      }
      assertThat(exception).isEqualTo(expectedException)
      verify(referenceCodeService).validateReferenceCode(ReferenceCodeGroup.RESTRICTION, "CCTV", allowInactive = true)
    }

    @Test
    fun `updated prisoner contact restriction`() {
      whenever(manageUsersService.getUserByUsername("updated")).thenReturn(UserDetails("updated", "Updated User"))
      whenever(prisonerContactRepository.findById(prisonerContactId)).thenReturn(Optional.of(aPrisonerContact))
      whenever(prisonerContactRestrictionRepository.findById(prisonerContactRestrictionId)).thenReturn(
        Optional.of(
          existingEntity,
        ),
      )
      whenever(referenceCodeService.validateReferenceCode(ReferenceCodeGroup.RESTRICTION, "CCTV", allowInactive = true)).thenReturn(
        ReferenceCode(
          referenceCodeId = 0,
          ReferenceCodeGroup.RESTRICTION,
          "CCTV",
          "CCTV",
          99,
          true,
        ),
      )
      whenever(prisonerContactRestrictionRepository.saveAndFlush(any())).thenAnswer { i ->
        (i.arguments[0] as PrisonerContactRestrictionEntity).copy(
          prisonerContactRestrictionId = 9999,
        )
      }

      val updated = service.updatePrisonerContactRestriction(
        prisonerContactId,
        prisonerContactRestrictionId,
        anUpdatePrisonerContactRestrictionRequest(),
        user,
      )
      assertThat(updated).isEqualTo(
        PrisonerContactRestrictionDetails(
          prisonerContactRestrictionId = 9999,
          prisonerContactId = prisonerContactId,
          contactId = contactId,
          prisonerNumber = aPrisonerContact.prisonerNumber,
          restrictionType = "CCTV",
          restrictionTypeDescription = "CCTV",
          startDate = LocalDate.of(1990, 1, 1),
          expiryDate = LocalDate.of(1992, 2, 2),
          comments = "Updated comments",
          enteredByUsername = "updated",
          enteredByDisplayName = "Updated User",
          createdBy = "created",
          createdTime = updated.createdTime,
          updatedBy = "updated",
          updatedTime = updated.updatedTime,
        ),
      )
      verify(prisonerContactRestrictionRepository).saveAndFlush(any())
      verify(referenceCodeService).validateReferenceCode(ReferenceCodeGroup.RESTRICTION, "CCTV", allowInactive = true)
    }

    private fun anUpdatePrisonerContactRestrictionRequest(): UpdatePrisonerContactRestrictionRequest = UpdatePrisonerContactRestrictionRequest(
      restrictionType = "CCTV",
      startDate = LocalDate.of(1990, 1, 1),
      expiryDate = LocalDate.of(1992, 2, 2),
      comments = "Updated comments",
    )
  }
}
