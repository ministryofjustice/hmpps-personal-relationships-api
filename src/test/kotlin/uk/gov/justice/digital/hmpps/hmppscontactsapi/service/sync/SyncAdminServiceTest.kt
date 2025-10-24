package uk.gov.justice.digital.hmpps.hmppscontactsapi.service.sync

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.PrisonerContactEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.PrisonerContactRestrictionEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.migrate.CodedValue
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.MergePrisonerContactRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.ResetPrisonerContactRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncPrisonerRelationship
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncRelationshipRestriction
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.migrate.ElementType
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.PrisonerContactRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.PrisonerContactRestrictionRepository
import java.time.LocalDate
import java.time.LocalDateTime

class SyncAdminServiceTest {
  private val prisonerContactRepository: PrisonerContactRepository = mock()
  private val prisonerContactRestrictionRepository: PrisonerContactRestrictionRepository = mock()

  private val syncAdminService = SyncAdminService(
    prisonerContactRepository,
    prisonerContactRestrictionRepository,
  )

  @Nested
  inner class MergePrisonerContactsTests {
    @Test
    fun `should merge prisoner contacts successfully`() {
      val request = createMergePrisonerContactRequest()
      val existingRelationshipsRemoved = listOf(createPrisonerContactEntity("A1234AA", 1L))
      val existingRelationshipsRetained = listOf(createPrisonerContactEntity("A1234BB", 2L))
      val existingRestrictionsRemoved = listOf(createPrisonerContactRestrictionEntity(1L))
      val existingRestrictionsRetained = listOf(createPrisonerContactRestrictionEntity(2L))

      whenever(prisonerContactRepository.findAllByPrisonerNumber("A1234AA"))
        .thenReturn(existingRelationshipsRemoved)
      whenever(prisonerContactRepository.findAllByPrisonerNumber("A1234BB"))
        .thenReturn(existingRelationshipsRetained)
      whenever(prisonerContactRestrictionRepository.findAllByPrisonerContactId(1L))
        .thenReturn(existingRestrictionsRemoved)
      whenever(prisonerContactRestrictionRepository.findAllByPrisonerContactId(2L))
        .thenReturn(existingRestrictionsRetained)
      whenever(prisonerContactRepository.save(any())).thenReturn(createPrisonerContactEntity("A1234BB", 3L))
      whenever(prisonerContactRestrictionRepository.save(any())).thenReturn(createPrisonerContactRestrictionEntity(3L))

      val response = syncAdminService.mergePrisonerContacts(request)

      with(response) {
        assertThat(relationshipsCreated).hasSize(1)
        assertThat(relationshipsRemoved).hasSize(2)

        with(relationshipsCreated.first()) {
          assertThat(contactId).isEqualTo(3L)
          assertThat(relationship.elementType).isEqualTo(ElementType.PRISONER_CONTACT)
          assertThat(relationship.nomisId).isEqualTo(123L)
          assertThat(relationship.dpsId).isEqualTo(3L)
          assertThat(restrictions).hasSize(1)
          assertThat(restrictions.first().elementType).isEqualTo(ElementType.PRISONER_CONTACT_RESTRICTION)
        }

        with(relationshipsRemoved) {
          assertThat(this[0].prisonerNumber).isEqualTo("A1234AA")
          assertThat(this[0].contactId).isEqualTo(1L)
          assertThat(this[0].prisonerContactId).isEqualTo(1L)
          assertThat(this[0].prisonerContactRestrictionIds).hasSize(1)

          assertThat(this[1].prisonerNumber).isEqualTo("A1234BB")
          assertThat(this[1].contactId).isEqualTo(2L)
          assertThat(this[1].prisonerContactId).isEqualTo(2L)
          assertThat(this[1].prisonerContactRestrictionIds).hasSize(1)
        }
      }

      verify(prisonerContactRepository).findAllByPrisonerNumber("A1234AA")
      verify(prisonerContactRepository).findAllByPrisonerNumber("A1234BB")
      verify(prisonerContactRestrictionRepository).findAllByPrisonerContactId(1L)
      verify(prisonerContactRestrictionRepository).findAllByPrisonerContactId(2L)
      verify(prisonerContactRestrictionRepository).deleteAllByPrisonerContactId(1L)
      verify(prisonerContactRestrictionRepository).deleteAllByPrisonerContactId(2L)
      verify(prisonerContactRepository).deleteAllByPrisonerNumber("A1234AA")
      verify(prisonerContactRepository).deleteAllByPrisonerNumber("A1234BB")
      verify(prisonerContactRepository).save(any())
      verify(prisonerContactRestrictionRepository).save(any())
    }

    @Test
    fun `should handle merge with no existing relationships`() {
      val request = createMergePrisonerContactRequest()

      whenever(prisonerContactRepository.findAllByPrisonerNumber("A1234AA")).thenReturn(emptyList())
      whenever(prisonerContactRepository.findAllByPrisonerNumber("A1234BB")).thenReturn(emptyList())
      whenever(prisonerContactRepository.save(any())).thenReturn(createPrisonerContactEntity("A1234BB", 3L))
      whenever(prisonerContactRestrictionRepository.save(any())).thenReturn(createPrisonerContactRestrictionEntity(3L))

      val response = syncAdminService.mergePrisonerContacts(request)

      with(response) {
        assertThat(relationshipsCreated).hasSize(1)
        assertThat(relationshipsRemoved).isEmpty()
      }

      verify(prisonerContactRepository).findAllByPrisonerNumber("A1234AA")
      verify(prisonerContactRepository).findAllByPrisonerNumber("A1234BB")
      verify(prisonerContactRepository).deleteAllByPrisonerNumber("A1234AA")
      verify(prisonerContactRepository).deleteAllByPrisonerNumber("A1234BB")
      verify(prisonerContactRepository).save(any())
      verify(prisonerContactRestrictionRepository).save(any())
    }

    @Test
    fun `should handle merge with relationships but no restrictions`() {
      val request = createMergePrisonerContactRequest()
      val existingRelationshipsRemoved = listOf(createPrisonerContactEntity("A1234AA", 1L))
      val existingRelationshipsRetained = listOf(createPrisonerContactEntity("A1234BB", 2L))

      whenever(prisonerContactRepository.findAllByPrisonerNumber("A1234AA"))
        .thenReturn(existingRelationshipsRemoved)
      whenever(prisonerContactRepository.findAllByPrisonerNumber("A1234BB"))
        .thenReturn(existingRelationshipsRetained)
      whenever(prisonerContactRestrictionRepository.findAllByPrisonerContactId(1L)).thenReturn(emptyList())
      whenever(prisonerContactRestrictionRepository.findAllByPrisonerContactId(2L)).thenReturn(emptyList())
      whenever(prisonerContactRepository.save(any())).thenReturn(createPrisonerContactEntity("A1234BB", 3L))
      whenever(prisonerContactRestrictionRepository.save(any())).thenReturn(createPrisonerContactRestrictionEntity(3L))

      val response = syncAdminService.mergePrisonerContacts(request)

      with(response) {
        assertThat(relationshipsCreated).hasSize(1)
        assertThat(relationshipsRemoved).hasSize(2)

        with(relationshipsRemoved) {
          assertThat(this[0].prisonerContactRestrictionIds).isEmpty()
          assertThat(this[1].prisonerContactRestrictionIds).isEmpty()
        }
      }

      verify(prisonerContactRestrictionRepository).findAllByPrisonerContactId(1L)
      verify(prisonerContactRestrictionRepository).findAllByPrisonerContactId(2L)
      verify(prisonerContactRestrictionRepository).deleteAllByPrisonerContactId(1L)
      verify(prisonerContactRestrictionRepository).deleteAllByPrisonerContactId(2L)
    }

    @Test
    fun `should handle merge with empty prisoner contacts list`() {
      val request = createMergePrisonerContactRequest(emptyList())

      whenever(prisonerContactRepository.findAllByPrisonerNumber("A1234AA")).thenReturn(emptyList())
      whenever(prisonerContactRepository.findAllByPrisonerNumber("A1234BB")).thenReturn(emptyList())

      val response = syncAdminService.mergePrisonerContacts(request)

      with(response) {
        assertThat(relationshipsCreated).isEmpty()
        assertThat(relationshipsRemoved).isEmpty()
      }

      verify(prisonerContactRepository).findAllByPrisonerNumber("A1234AA")
      verify(prisonerContactRepository).findAllByPrisonerNumber("A1234BB")
      verify(prisonerContactRepository).deleteAllByPrisonerNumber("A1234AA")
      verify(prisonerContactRepository).deleteAllByPrisonerNumber("A1234BB")
      verifyNoInteractions(prisonerContactRestrictionRepository)
    }

    @Test
    fun `should preserve approved visitor details during merge when approved visitor is true`() {
      val fixedTime = LocalDateTime.now()
      val request = createMergePrisonerContactRequest(
        listOf(createSyncPrisonerRelationship(approvedVisitor = true)),
      )
      val existingRelationshipsRemoved = listOf(
        createPrisonerContactEntity(
          "A1234AA",
          100L,
          approvedVisitor = true,
          approvedBy = "APPROVER_REMOVED",
          approvedTime = fixedTime.minusDays(2),
        ),
      )
      val existingRelationshipsRetained = listOf(
        createPrisonerContactEntity(
          "A1234BB",
          100L,
          approvedVisitor = true,
          approvedBy = "APPROVER_RETAINED",
          approvedTime = fixedTime.minusDays(1),
        ),
      )

      whenever(prisonerContactRepository.findAllByPrisonerNumber("A1234AA"))
        .thenReturn(existingRelationshipsRemoved)
      whenever(prisonerContactRepository.findAllByPrisonerNumber("A1234BB"))
        .thenReturn(existingRelationshipsRetained)
      whenever(prisonerContactRestrictionRepository.findAllByPrisonerContactId(100L)).thenReturn(emptyList())
      whenever(prisonerContactRepository.save(any())).thenReturn(createPrisonerContactEntity("A1234BB", 3L))
      whenever(prisonerContactRestrictionRepository.save(any())).thenReturn(createPrisonerContactRestrictionEntity(3L))

      val response = syncAdminService.mergePrisonerContacts(request)

      val contactCaptor = argumentCaptor<PrisonerContactEntity>()
      verify(prisonerContactRepository).save(contactCaptor.capture())

      with(contactCaptor.firstValue) {
        assertThat(approvedVisitor).isTrue()
        assertThat(approvedBy).isEqualTo("APPROVER_RETAINED")
        assertThat(approvedTime).isNotNull()
        assertThat(approvedTime).isEqualTo(fixedTime.minusDays(1))
      }

      assertThat(response.relationshipsCreated).hasSize(1)
    }

    @Test
    fun `should preserve approved visitor details from removed prisoner when retained prisoner has no approved visitor`() {
      val fixedTime = LocalDateTime.now()
      val request = createMergePrisonerContactRequest(
        listOf(createSyncPrisonerRelationship(approvedVisitor = true)),
      )
      val existingRelationshipsRemoved = listOf(
        createPrisonerContactEntity(
          "A1234BB",
          100L,
          approvedVisitor = true,
          approvedBy = "APPROVER_REMOVED",
          approvedTime = fixedTime.minusDays(2),
        ),
      )
      val existingRelationshipsRetained = listOf(
        createPrisonerContactEntity(
          "A1234BB",
          100L,
          approvedVisitor = false,
          approvedBy = null,
          approvedTime = null,
        ),
      )

      whenever(prisonerContactRepository.findAllByPrisonerNumber("A1234AA"))
        .thenReturn(existingRelationshipsRemoved)
      whenever(prisonerContactRepository.findAllByPrisonerNumber("A1234BB"))
        .thenReturn(existingRelationshipsRetained)
      whenever(prisonerContactRestrictionRepository.findAllByPrisonerContactId(100L)).thenReturn(emptyList())
      whenever(prisonerContactRepository.save(any())).thenReturn(createPrisonerContactEntity("A1234BB", 3L))
      whenever(prisonerContactRestrictionRepository.save(any())).thenReturn(createPrisonerContactRestrictionEntity(3L))

      val response = syncAdminService.mergePrisonerContacts(request)

      val contactCaptor = argumentCaptor<PrisonerContactEntity>()
      verify(prisonerContactRepository).save(contactCaptor.capture())

      with(contactCaptor.firstValue) {
        assertThat(approvedVisitor).isTrue()
        assertThat(approvedBy).isEqualTo("APPROVER_REMOVED")
        assertThat(approvedTime).isNotNull()
        assertThat(approvedTime).isEqualTo(fixedTime.minusDays(2))
      }

      assertThat(response.relationshipsCreated).hasSize(1)
    }

    @Test
    fun `should not preserve approved visitor details when approved visitor is false in incoming relationship`() {
      val request = createMergePrisonerContactRequest(
        listOf(createSyncPrisonerRelationship(approvedVisitor = false)),
      )
      val existingRelationshipsRemoved = listOf(
        createPrisonerContactEntity(
          "A1234AA",
          1L,
          approvedVisitor = true,
          approvedBy = "APPROVER_REMOVED",
          approvedTime = LocalDateTime.now().minusDays(2),
        ),
      )
      val existingRelationshipsRetained = listOf(
        createPrisonerContactEntity(
          "A1234BB",
          2L,
          approvedVisitor = true,
          approvedBy = "APPROVER_RETAINED",
          approvedTime = LocalDateTime.now().minusDays(1),
        ),
      )

      whenever(prisonerContactRepository.findAllByPrisonerNumber("A1234AA"))
        .thenReturn(existingRelationshipsRemoved)
      whenever(prisonerContactRepository.findAllByPrisonerNumber("A1234BB"))
        .thenReturn(existingRelationshipsRetained)
      whenever(prisonerContactRestrictionRepository.findAllByPrisonerContactId(1L)).thenReturn(emptyList())
      whenever(prisonerContactRestrictionRepository.findAllByPrisonerContactId(2L)).thenReturn(emptyList())
      whenever(prisonerContactRepository.save(any())).thenReturn(createPrisonerContactEntity("A1234BB", 3L))
      whenever(prisonerContactRestrictionRepository.save(any())).thenReturn(createPrisonerContactRestrictionEntity(3L))

      val response = syncAdminService.mergePrisonerContacts(request)

      val contactCaptor = argumentCaptor<PrisonerContactEntity>()
      verify(prisonerContactRepository).save(contactCaptor.capture())

      with(contactCaptor.firstValue) {
        assertThat(approvedVisitor).isFalse()
        assertThat(approvedBy).isNull()
        assertThat(approvedTime).isNull()
      }

      assertThat(response.relationshipsCreated).hasSize(1)
    }
  }

  @Nested
  inner class ResetPrisonerContactsTests {
    @Test
    fun `should reset prisoner contacts successfully`() {
      val request = createResetPrisonerContactRequest()
      val existingRelationships = listOf(createPrisonerContactEntity("A1234AA", 1L))
      val existingRestrictions = listOf(createPrisonerContactRestrictionEntity(1L))

      whenever(prisonerContactRepository.findAllByPrisonerNumber("A1234AA")).thenReturn(existingRelationships)
      whenever(prisonerContactRestrictionRepository.findAllByPrisonerContactId(1L)).thenReturn(existingRestrictions)
      whenever(prisonerContactRepository.save(any())).thenReturn(createPrisonerContactEntity("A1234AA", 2L))
      whenever(prisonerContactRestrictionRepository.save(any())).thenReturn(createPrisonerContactRestrictionEntity(2L))

      val response = syncAdminService.resetPrisonerContacts(request)

      with(response) {
        assertThat(relationshipsCreated).hasSize(1)
        assertThat(relationshipsRemoved).hasSize(1)

        with(relationshipsCreated.first()) {
          assertThat(contactId).isEqualTo(2L)
          assertThat(relationship.elementType).isEqualTo(ElementType.PRISONER_CONTACT)
          assertThat(relationship.nomisId).isEqualTo(123L)
          assertThat(relationship.dpsId).isEqualTo(2L)
          assertThat(restrictions).hasSize(1)
        }

        with(relationshipsRemoved.first()) {
          assertThat(prisonerNumber).isEqualTo("A1234AA")
          assertThat(contactId).isEqualTo(1L)
          assertThat(prisonerContactId).isEqualTo(1L)
          assertThat(prisonerContactRestrictionIds).hasSize(1)
        }
      }

      verify(prisonerContactRepository).findAllByPrisonerNumber("A1234AA")
      verify(prisonerContactRestrictionRepository).findAllByPrisonerContactId(1L)
      verify(prisonerContactRestrictionRepository).deleteAllByPrisonerContactId(1L)
      verify(prisonerContactRepository).deleteAllByPrisonerNumber("A1234AA")
      verify(prisonerContactRepository).save(any())
      verify(prisonerContactRestrictionRepository).save(any())
    }

    @Test
    fun `should handle reset with no existing relationships`() {
      val request = createResetPrisonerContactRequest()

      whenever(prisonerContactRepository.findAllByPrisonerNumber("A1234AA")).thenReturn(emptyList())
      whenever(prisonerContactRepository.save(any())).thenReturn(createPrisonerContactEntity("A1234AA", 2L))
      whenever(prisonerContactRestrictionRepository.save(any())).thenReturn(createPrisonerContactRestrictionEntity(2L))

      val response = syncAdminService.resetPrisonerContacts(request)

      with(response) {
        assertThat(relationshipsCreated).hasSize(1)
        assertThat(relationshipsRemoved).isEmpty()
      }

      verify(prisonerContactRepository).findAllByPrisonerNumber("A1234AA")
      verify(prisonerContactRepository).deleteAllByPrisonerNumber("A1234AA")
      verify(prisonerContactRepository).save(any())
      verify(prisonerContactRestrictionRepository).save(any())
    }

    @Test
    fun `should handle reset with relationships but no restrictions`() {
      val request = createResetPrisonerContactRequest()
      val existingRelationships = listOf(createPrisonerContactEntity("A1234AA", 1L))

      whenever(prisonerContactRepository.findAllByPrisonerNumber("A1234AA")).thenReturn(existingRelationships)
      whenever(prisonerContactRestrictionRepository.findAllByPrisonerContactId(1L)).thenReturn(emptyList())
      whenever(prisonerContactRepository.save(any())).thenReturn(createPrisonerContactEntity("A1234AA", 2L))
      whenever(prisonerContactRestrictionRepository.save(any())).thenReturn(createPrisonerContactRestrictionEntity(2L))

      val response = syncAdminService.resetPrisonerContacts(request)

      with(response) {
        assertThat(relationshipsCreated).hasSize(1)
        assertThat(relationshipsRemoved).hasSize(1)
        assertThat(relationshipsRemoved.first().prisonerContactRestrictionIds).isEmpty()
      }

      verify(prisonerContactRestrictionRepository).findAllByPrisonerContactId(1L)
      verify(prisonerContactRestrictionRepository).deleteAllByPrisonerContactId(1L)
    }

    @Test
    fun `should handle reset with empty prisoner contacts list`() {
      val request = createResetPrisonerContactRequest(emptyList())

      whenever(prisonerContactRepository.findAllByPrisonerNumber("A1234AA")).thenReturn(emptyList())

      val response = syncAdminService.resetPrisonerContacts(request)

      with(response) {
        assertThat(relationshipsCreated).isEmpty()
        assertThat(relationshipsRemoved).isEmpty()
      }

      verify(prisonerContactRepository).findAllByPrisonerNumber("A1234AA")
      verify(prisonerContactRepository).deleteAllByPrisonerNumber("A1234AA")
      verifyNoInteractions(prisonerContactRestrictionRepository)
    }

    @Test
    fun `should preserve approved visitor details during reset when approved visitor is true`() {
      val request = createResetPrisonerContactRequest()
      val existingRelationships = listOf(
        createPrisonerContactEntity("A1234AA", 100L, approvedVisitor = true, approvedBy = "APPROVER", approvedTime = LocalDateTime.now().minusDays(1)),
      )

      whenever(prisonerContactRepository.findAllByPrisonerNumber("A1234AA")).thenReturn(existingRelationships)
      whenever(prisonerContactRepository.save(any())).thenReturn(createPrisonerContactEntity("A1234AA", 2L))
      whenever(prisonerContactRestrictionRepository.save(any())).thenReturn(createPrisonerContactRestrictionEntity(2L))

      val response = syncAdminService.resetPrisonerContacts(request)

      val contactCaptor = argumentCaptor<PrisonerContactEntity>()
      verify(prisonerContactRepository).save(contactCaptor.capture())

      with(contactCaptor.firstValue) {
        assertThat(approvedBy).isEqualTo("APPROVER")
        assertThat(approvedTime).isNotNull()
      }

      assertThat(response.relationshipsCreated).hasSize(1)
    }
  }

  private fun createMergePrisonerContactRequest(
    prisonerContacts: List<SyncPrisonerRelationship> = listOf(createSyncPrisonerRelationship()),
  ) = MergePrisonerContactRequest(
    retainedPrisonerNumber = "A1234BB",
    removedPrisonerNumber = "A1234AA",
    prisonerContacts = prisonerContacts,
  )

  private fun createResetPrisonerContactRequest(
    prisonerContacts: List<SyncPrisonerRelationship> = listOf(createSyncPrisonerRelationship(prisonerNumber = "A1234AA")),
  ) = ResetPrisonerContactRequest(
    prisonerNumber = "A1234AA",
    prisonerContacts = prisonerContacts,
  )

  private fun createSyncPrisonerRelationship(
    prisonerNumber: String = "A1234BB",
    approvedVisitor: Boolean = true,
  ) = SyncPrisonerRelationship(
    id = 123L,
    contactId = 100L,
    contactType = CodedValue("S", "Social"),
    relationshipType = CodedValue("FAM", "Family"),
    currentTerm = true,
    active = true,
    expiryDate = null,
    approvedVisitor = approvedVisitor,
    nextOfKin = false,
    emergencyContact = false,
    comment = "Test relationship",
    prisonerNumber,
    restrictions = listOf(createSyncRelationshipRestriction()),
  )

  private fun createSyncRelationshipRestriction() = SyncRelationshipRestriction(
    id = 456L,
    restrictionType = CodedValue("VIS", "Visit Restriction"),
    comment = "Test restriction",
    startDate = LocalDate.now(),
    expiryDate = LocalDate.now().plusDays(30),
  )

  private fun createPrisonerContactEntity(
    prisonerNumber: String,
    contactId: Long,
    approvedVisitor: Boolean = false,
    approvedBy: String? = null,
    approvedTime: LocalDateTime? = null,
  ) = PrisonerContactEntity(
    prisonerContactId = contactId,
    contactId = contactId,
    prisonerNumber = prisonerNumber,
    relationshipType = "S",
    relationshipToPrisoner = "FAM",
    nextOfKin = false,
    emergencyContact = false,
    comments = "Test relationship",
    active = true,
    approvedVisitor = approvedVisitor,
    currentTerm = true,
    createdBy = "TEST_USER",
    createdTime = LocalDateTime.now(),
  ).apply {
    this.approvedBy = approvedBy
    this.approvedTime = approvedTime
  }

  private fun createPrisonerContactRestrictionEntity(prisonerContactId: Long) = PrisonerContactRestrictionEntity(
    prisonerContactRestrictionId = prisonerContactId,
    prisonerContactId = prisonerContactId,
    restrictionType = "VIS",
    startDate = LocalDate.now(),
    expiryDate = LocalDate.now().plusDays(30),
    comments = "Test restriction",
    createdBy = "TEST_USER",
    createdTime = LocalDateTime.now(),
    updatedBy = "TEST_USER",
    updatedTime = LocalDateTime.now(),
  )
}
