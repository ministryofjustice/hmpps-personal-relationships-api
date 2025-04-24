package uk.gov.justice.digital.hmpps.hmppscontactsapi.service

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.never
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.ContactEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.EmploymentEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.helpers.aUser
import uk.gov.justice.digital.hmpps.hmppscontactsapi.helpers.createEmploymentEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.helpers.createOrganisationSummary
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.employment.Employment
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.employment.PatchEmploymentsRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.employment.PatchEmploymentsUpdateEmployment
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.employment.UpdateEmploymentRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.ContactRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.EmploymentRepository
import java.time.LocalDateTime.now
import java.util.*

class EmploymentServiceTest {
  private val contactId = 99L
  private val contact = ContactEntity(
    contactId = contactId,
    title = "Mr",
    lastName = "last",
    middleNames = "middle",
    firstName = "first",
    dateOfBirth = null,
    deceasedDate = null,
    createdBy = "user",
    createdTime = now(),
  )

  private val contactRepository: ContactRepository = mock()
  private val employmentRepository: EmploymentRepository = mock()
  private val organisationService: OrganisationService = mock()
  private val service = EmploymentService(contactRepository, employmentRepository, organisationService)
  private val user = aUser()

  @Test
  fun `Patch employments should create a new employment`() {
    val employmentEntity1 = createEmploymentEntity(id = 1, organisationId = 1)
    val employmentEntity2 = createEmploymentEntity(id = 2, organisationId = 2)
    val org1 = createOrganisationSummary(id = 1, organisationName = "One")
    val org2 = createOrganisationSummary(id = 2, organisationName = "Two")

    whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(contact))
    whenever(employmentRepository.saveAndFlush(any())).thenReturn(employmentEntity2)
    whenever(employmentRepository.findByContactId(contactId))
      .thenReturn(listOf(employmentEntity1)) // before save
      .thenReturn(listOf(employmentEntity1, employmentEntity2)) // after save
    whenever(organisationService.getOrganisationSummaryById(1)).thenReturn(org1)
    whenever(organisationService.getOrganisationSummaryById(2)).thenReturn(org2)

    val employments = service.patchEmployments(
      contactId,
      PatchEmploymentsRequest(
        createEmployments = listOf(Employment(2, true)),
        updateEmployments = emptyList(),
        deleteEmployments = emptyList(),
      ),
      user,
    )

    assertThat(employments.employmentsAfterUpdate).hasSize(2)
    assertThat(employments.createdIds).isEqualTo(listOf(2L))
    assertThat(employments.updatedIds).isEmpty()
    assertThat(employments.deletedIds).isEmpty()
    assertThat(employments.employmentsAfterUpdate[0].employmentId).isEqualTo(1)
    assertThat(employments.employmentsAfterUpdate[0].employer.organisationId).isEqualTo(1)
    assertThat(employments.employmentsAfterUpdate[1].employmentId).isEqualTo(2)
    assertThat(employments.employmentsAfterUpdate[1].employer.organisationId).isEqualTo(2)

    val employmentCaptor = argumentCaptor<EmploymentEntity>()
    verify(employmentRepository).saveAndFlush(employmentCaptor.capture())
    assertThat(employmentCaptor.firstValue).usingRecursiveComparison().ignoringFields("createdTime").isEqualTo(
      EmploymentEntity(
        employmentId = 0,
        organisationId = 2,
        contactId = contactId,
        active = true,
        createdBy = "USER1",
        createdTime = now(),
        updatedBy = null,
        updatedTime = null,
      ),
    )
  }

  @Test
  fun `Patch employments should update a new employment`() {
    val employmentEntityBeforeUpdate = createEmploymentEntity(
      id = 1,
      contactId = contactId,
      organisationId = 1,
      active = true,
      createdBy = "CREATED",
      updatedBy = null,
      updatedTime = null,
    )
    val employmentEntityAfterUpdate = createEmploymentEntity(
      id = 1,
      contactId = contactId,
      organisationId = 2,
      active = false,
      createdBy = "CREATED",
      updatedBy = "USER1",
      updatedTime = now(),
    )
    val org1 = createOrganisationSummary(id = 1, organisationName = "One")
    val org2 = createOrganisationSummary(id = 2, organisationName = "Two")

    whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(contact))
    whenever(employmentRepository.saveAndFlush(any())).thenReturn(employmentEntityAfterUpdate)
    whenever(employmentRepository.findByContactId(contactId))
      .thenReturn(listOf(employmentEntityBeforeUpdate)) // before save
      .thenReturn(listOf(employmentEntityAfterUpdate)) // after save
    whenever(organisationService.getOrganisationSummaryById(1)).thenReturn(org1)
    whenever(organisationService.getOrganisationSummaryById(2)).thenReturn(org2)

    val employments = service.patchEmployments(
      contactId,
      PatchEmploymentsRequest(
        createEmployments = emptyList(),
        updateEmployments = listOf(PatchEmploymentsUpdateEmployment(1, 2, false)),
        deleteEmployments = emptyList(),
      ),
      user,
    )

    assertThat(employments.employmentsAfterUpdate).hasSize(1)
    assertThat(employments.employmentsAfterUpdate[0].employmentId).isEqualTo(1)
    assertThat(employments.employmentsAfterUpdate[0].employer.organisationId).isEqualTo(2)
    assertThat(employments.employmentsAfterUpdate[0].isActive).isFalse()
    assertThat(employments.createdIds).isEmpty()
    assertThat(employments.updatedIds).isEqualTo(listOf(1L))
    assertThat(employments.deletedIds).isEmpty()

    val employmentCaptor = argumentCaptor<EmploymentEntity>()
    verify(employmentRepository).saveAndFlush(employmentCaptor.capture())
    assertThat(employmentCaptor.firstValue).usingRecursiveComparison().ignoringFields("createdTime", "updatedTime")
      .isEqualTo(
        EmploymentEntity(
          employmentId = 1,
          organisationId = 2,
          contactId = contactId,
          active = false,
          createdBy = "CREATED",
          createdTime = now(),
          updatedBy = "USER1",
          updatedTime = now(),
        ),
      )
  }

  @Test
  fun `Patch employments should blow up if employment to be updated not found`() {
    whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(contact))
    whenever(employmentRepository.findByContactId(contactId)).thenReturn(emptyList())

    val exception = assertThrows<EntityNotFoundException> {
      service.patchEmployments(
        contactId,
        PatchEmploymentsRequest(
          createEmployments = emptyList(),
          updateEmployments = listOf(PatchEmploymentsUpdateEmployment(1, 2, false)),
          deleteEmployments = emptyList(),
        ),
        user,
      )
    }
    assertThat(exception.message).isEqualTo("Employment with id 1 not found")
  }

  @Test
  fun `Patch employments should blow up if contact not found`() {
    whenever(contactRepository.findById(contactId)).thenReturn(Optional.empty())
    whenever(employmentRepository.findByContactId(contactId)).thenReturn(emptyList())

    val exception = assertThrows<EntityNotFoundException> {
      service.patchEmployments(
        contactId,
        PatchEmploymentsRequest(
          createEmployments = emptyList(),
          updateEmployments = listOf(PatchEmploymentsUpdateEmployment(1, 2, false)),
          deleteEmployments = emptyList(),
        ),
        user,
      )
    }
    assertThat(exception.message).isEqualTo("Contact (99) not found")
  }

  @Test
  fun `Patch employments should delete an employment`() {
    val employmentEntity = createEmploymentEntity(
      id = 1,
      contactId = contactId,
      organisationId = 1,
      active = true,
      createdBy = "CREATED",
      updatedBy = null,
      updatedTime = null,
    )
    val org1 = createOrganisationSummary(id = 1, organisationName = "One")

    whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(contact))
    doNothing().whenever(employmentRepository).delete(any())
    whenever(employmentRepository.findByContactId(contactId))
      .thenReturn(listOf(employmentEntity)) // before delete
      .thenReturn(emptyList()) // after delete
    whenever(organisationService.getOrganisationSummaryById(1)).thenReturn(org1)

    val employments = service.patchEmployments(
      contactId,
      PatchEmploymentsRequest(
        createEmployments = emptyList(),
        updateEmployments = emptyList(),
        deleteEmployments = listOf(1),
      ),
      user,
    )

    assertThat(employments.employmentsAfterUpdate).isEmpty()
    assertThat(employments.createdIds).isEmpty()
    assertThat(employments.updatedIds).isEmpty()
    assertThat(employments.deletedIds).isEqualTo(listOf(1L))

    val employmentCaptor = argumentCaptor<EmploymentEntity>()
    verify(employmentRepository).delete(employmentCaptor.capture())
    assertThat(employmentCaptor.firstValue).isEqualTo(employmentEntity)
  }

  @Test
  fun `Patch employments should blow up if employment to be deleted is not found`() {
    whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(contact))
    whenever(employmentRepository.findByContactId(contactId)).thenReturn(emptyList())

    val exception = assertThrows<EntityNotFoundException> {
      service.patchEmployments(
        contactId,
        PatchEmploymentsRequest(
          createEmployments = emptyList(),
          updateEmployments = emptyList(),
          deleteEmployments = listOf(99),
        ),
        user,
      )
    }
    assertThat(exception.message).isEqualTo("Employment with id 99 not found")
  }

  @Test
  fun `should get a contacts employments`() {
    val employmentEntity1 = createEmploymentEntity(id = 1, organisationId = 1)
    val employmentEntity2 = createEmploymentEntity(id = 2, organisationId = 2)
    val orgEntity1 = createOrganisationSummary(id = 1, organisationName = "One")
    val orgEntity2 = createOrganisationSummary(id = 2, organisationName = "Two")

    whenever(employmentRepository.findByContactId(contactId)).thenReturn(listOf(employmentEntity1, employmentEntity2))
    whenever(organisationService.getOrganisationSummaryById(1)).thenReturn(orgEntity1)
    whenever(organisationService.getOrganisationSummaryById(2)).thenReturn(orgEntity2)

    val employments = service.getEmploymentDetails(contactId)
    assertThat(employments).hasSize(2)
    assertThat(employments[0].employmentId).isEqualTo(1)
    assertThat(employments[0].employer.organisationId).isEqualTo(1)
    assertThat(employments[1].employmentId).isEqualTo(2)
    assertThat(employments[1].employer.organisationId).isEqualTo(2)
  }

  @Test
  fun `should get an employment`() {
    val employmentId = 10900L
    val employmentEntity = createEmploymentEntity(id = 1, organisationId = 1)
    val orgEntity1 = createOrganisationSummary(id = 1, organisationName = "One")

    whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(contact))
    whenever(employmentRepository.findById(employmentId)).thenReturn(Optional.of(employmentEntity))
    whenever(organisationService.getOrganisationSummaryById(1)).thenReturn(orgEntity1)

    val employment = service.getEmployment(contactId, employmentId)
    assertThat(employment.employmentId).isEqualTo(1)
    assertThat(employment.employer.organisationId).isEqualTo(1)
  }

  @Test
  fun `should create employment`() {
    val employmentEntity = createEmploymentEntity(id = 1, organisationId = 1)
    val org = createOrganisationSummary(id = 1, organisationName = "One")

    whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(contact))
    whenever(employmentRepository.saveAndFlush(any())).thenReturn(employmentEntity)
    whenever(organisationService.getOrganisationSummaryById(1)).thenReturn(org)

    val created = service.createEmployment(
      contactId = contactId,
      organisationId = 1,
      isActive = true,
      createdBy = "USER1",
    )

    assertThat(created.employmentId).isEqualTo(1)
    assertThat(created.employer.organisationId).isEqualTo(1)

    val employmentCaptor = argumentCaptor<EmploymentEntity>()
    verify(employmentRepository).saveAndFlush(employmentCaptor.capture())
    assertThat(employmentCaptor.firstValue).usingRecursiveComparison().ignoringFields("createdTime").isEqualTo(
      EmploymentEntity(
        employmentId = 0,
        organisationId = 1,
        contactId = contactId,
        active = true,
        createdBy = "USER1",
        createdTime = now(),
        updatedBy = null,
        updatedTime = null,
      ),
    )
  }

  @Test
  fun `should blow up if employer invalid on create`() {
    val employmentEntity = createEmploymentEntity(id = 1, organisationId = 1)
    val expectedException = EntityNotFoundException("Organisation with id 1 not found")

    whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(contact))
    whenever(employmentRepository.saveAndFlush(any())).thenReturn(employmentEntity)
    whenever(organisationService.getOrganisationSummaryById(1)).thenThrow(expectedException)

    val exception = assertThrows<EntityNotFoundException> {
      service.createEmployment(
        contactId = contactId,
        organisationId = 1,
        isActive = true,
        createdBy = "USER1",
      )
    }
    assertThat(exception).isEqualTo(expectedException)
    verify(employmentRepository, never()).saveAndFlush(any())
  }

  @Test
  fun `should blow up if contact not found on create`() {
    val employmentEntity = createEmploymentEntity(id = 1, organisationId = 1)

    whenever(contactRepository.findById(contactId)).thenReturn(Optional.empty())
    whenever(employmentRepository.saveAndFlush(any())).thenReturn(employmentEntity)

    val exception = assertThrows<EntityNotFoundException> {
      service.createEmployment(
        contactId = contactId,
        organisationId = 1,
        isActive = true,
        createdBy = "USER1",
      )
    }
    assertThat(exception.message).isEqualTo("Contact (99) not found")
    verify(employmentRepository, never()).saveAndFlush(any())
  }

  @Test
  fun `should update employment`() {
    val employmentId = 999L
    val employmentEntityBeforeUpdate =
      createEmploymentEntity(id = employmentId, contactId = contactId, organisationId = 1, active = true)
    val employmentEntityAfterUpdate =
      createEmploymentEntity(id = employmentId, contactId = contactId, organisationId = 2, active = false)
    val org = createOrganisationSummary(id = 2, organisationName = "One")

    whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(contact))
    whenever(employmentRepository.findById(any())).thenReturn(Optional.of(employmentEntityBeforeUpdate))
    whenever(employmentRepository.saveAndFlush(any())).thenReturn(employmentEntityAfterUpdate)
    whenever(organisationService.getOrganisationSummaryById(2)).thenReturn(org)

    val updated = service.updateEmployment(
      contactId,
      employmentId,
      UpdateEmploymentRequest(
        organisationId = 2,
        isActive = false,
      ),
      user,
    )

    assertThat(updated.employmentId).isEqualTo(999)
    assertThat(updated.employer.organisationId).isEqualTo(2)

    val employmentCaptor = argumentCaptor<EmploymentEntity>()
    verify(employmentRepository).saveAndFlush(employmentCaptor.capture())
    assertThat(employmentCaptor.firstValue).usingRecursiveComparison().ignoringFields("createdTime", "updatedTime")
      .isEqualTo(
        EmploymentEntity(
          employmentId = 999,
          organisationId = 2,
          contactId = contactId,
          active = false,
          createdBy = "USER",
          createdTime = now(),
          updatedBy = "USER1",
          updatedTime = now(),
        ),
      )
  }

  @Test
  fun `should blow up if employer invalid on update`() {
    val employmentEntity = createEmploymentEntity(id = 1, organisationId = 1)
    val expectedException = EntityNotFoundException("Organisation with id 1 not found")

    whenever(employmentRepository.findById(any())).thenReturn(Optional.of(employmentEntity))
    whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(contact))
    whenever(employmentRepository.saveAndFlush(any())).thenReturn(employmentEntity)
    whenever(organisationService.getOrganisationSummaryById(1)).thenThrow(expectedException)

    val exception = assertThrows<EntityNotFoundException> {
      service.updateEmployment(
        contactId,
        1,
        UpdateEmploymentRequest(
          organisationId = 1,
          isActive = true,
        ),
        user,
      )
    }
    assertThat(exception).isEqualTo(expectedException)
    verify(employmentRepository, never()).saveAndFlush(any())
  }

  @Test
  fun `should blow up if employment not found on update`() {
    val employmentEntity = createEmploymentEntity(id = 1, organisationId = 1)

    whenever(employmentRepository.findById(any())).thenReturn(Optional.empty())
    whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(contact))
    whenever(employmentRepository.saveAndFlush(any())).thenReturn(employmentEntity)

    val exception = assertThrows<EntityNotFoundException> {
      service.updateEmployment(
        contactId,
        1,
        UpdateEmploymentRequest(
          organisationId = 1,
          isActive = true,
        ),
        user,
      )
    }
    assertThat(exception.message).isEqualTo("Employment (1) not found")
    verify(employmentRepository, never()).saveAndFlush(any())
  }

  @Test
  fun `should blow up if contact not found on update`() {
    val employmentEntity = createEmploymentEntity(id = 1, organisationId = 1)

    whenever(contactRepository.findById(contactId)).thenReturn(Optional.empty())
    whenever(employmentRepository.findById(any())).thenReturn(Optional.of(employmentEntity))
    whenever(employmentRepository.saveAndFlush(any())).thenReturn(employmentEntity)

    val exception = assertThrows<EntityNotFoundException> {
      service.updateEmployment(
        contactId,
        1,
        UpdateEmploymentRequest(
          organisationId = 1,
          isActive = true,
        ),
        user,
      )
    }
    assertThat(exception.message).isEqualTo("Contact (99) not found")
    verify(employmentRepository, never()).saveAndFlush(any())
  }

  @Test
  fun `should blow up if employment not found on delete`() {
    whenever(employmentRepository.findById(any())).thenReturn(Optional.empty())
    whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(contact))

    val exception = assertThrows<EntityNotFoundException> {
      service.deleteEmployment(contactId, 1)
    }
    assertThat(exception.message).isEqualTo("Employment (1) not found")
    verify(employmentRepository, never()).delete(any())
  }

  @Test
  fun `should blow up if contact not found on delete`() {
    val employmentEntity = createEmploymentEntity(id = 1, organisationId = 1)

    whenever(contactRepository.findById(contactId)).thenReturn(Optional.empty())
    whenever(employmentRepository.findById(any())).thenReturn(Optional.of(employmentEntity))

    val exception = assertThrows<EntityNotFoundException> {
      service.deleteEmployment(contactId, 1)
    }
    assertThat(exception.message).isEqualTo("Contact (99) not found")
    verify(employmentRepository, never()).delete(any())
  }

  @Test
  fun `should delete employment`() {
    val employmentEntity = createEmploymentEntity(id = 1, organisationId = 1)

    whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(contact))
    whenever(employmentRepository.findById(any())).thenReturn(Optional.of(employmentEntity))

    service.deleteEmployment(contactId, 1)

    verify(employmentRepository).delete(employmentEntity)
  }
}
