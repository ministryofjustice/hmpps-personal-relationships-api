package uk.gov.justice.digital.hmpps.hmppscontactsapi.service.sync

import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppscontactsapi.mapping.sync.toEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.mapping.sync.toModel
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncCreateContactRestrictionRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncUpdateContactRestrictionRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync.SyncContactRestriction
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.ContactRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.ContactRestrictionRepository

@Service
@Transactional
class SyncContactRestrictionService(
  val contactRepository: ContactRepository,
  val contactRestrictionRepository: ContactRestrictionRepository,
) {

  companion object {
    private val logger = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional(readOnly = true)
  fun getContactRestrictionById(contactRestrictionId: Long): SyncContactRestriction {
    val contactRestrictionEntity = contactRestrictionRepository.findById(contactRestrictionId)
      .orElseThrow { EntityNotFoundException("Contact restriction with ID $contactRestrictionId not found") }
    return contactRestrictionEntity.toModel()
  }

  fun createContactRestriction(request: SyncCreateContactRestrictionRequest): SyncContactRestriction {
    contactRepository.findById(request.contactId)
      .orElseThrow { EntityNotFoundException("Contact with ID ${request.contactId} not found") }
    return contactRestrictionRepository.saveAndFlush(request.toEntity()).toModel()
  }

  fun updateContactRestriction(
    contactRestrictionId: Long,
    request: SyncUpdateContactRestrictionRequest,
  ): SyncContactRestriction {
    val contact = contactRepository.findById(request.contactId)
      .orElseThrow { EntityNotFoundException("Contact with ID ${request.contactId} not found") }

    val restrictionEntity = contactRestrictionRepository.findById(contactRestrictionId)
      .orElseThrow { EntityNotFoundException("Contact restriction with ID $contactRestrictionId not found") }

    if (contact.contactId != restrictionEntity.contactId) {
      logger.error("Contact restriction update specified for a contact not linked to this restriction")
      throw ValidationException("Contact ID ${contact.contactId} is not linked to the restriction ${restrictionEntity.contactRestrictionId}")
    }

    val changedContactRestriction = restrictionEntity.copy(
      contactId = request.contactId,
      restrictionType = request.restrictionType,
      startDate = request.startDate,
      expiryDate = request.expiryDate,
      comments = request.comments,
      updatedBy = request.updatedBy,
      updatedTime = request.updatedTime,
    )

    return contactRestrictionRepository.saveAndFlush(changedContactRestriction).toModel()
  }

  fun deleteContactRestriction(contactRestrictionId: Long): SyncContactRestriction {
    val rowToDelete = contactRestrictionRepository.findById(contactRestrictionId)
      .orElseThrow { EntityNotFoundException("Contact restriction with ID $contactRestrictionId not found") }
    contactRestrictionRepository.deleteById(contactRestrictionId)
    return rowToDelete.toModel()
  }
}
