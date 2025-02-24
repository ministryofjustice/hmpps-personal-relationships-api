package uk.gov.justice.digital.hmpps.hmppscontactsapi.service.sync

import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppscontactsapi.mapping.sync.mapEntityToSyncResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.mapping.sync.mapSyncRequestToEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncCreateContactRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.sync.SyncUpdateContactRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync.SyncContact
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.ContactWithFixedIdRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.migrate.DuplicatePersonException

@Service
@Transactional
class SyncContactService(
  val contactRepository: ContactWithFixedIdRepository,
) {
  companion object {
    private val logger = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional(readOnly = true)
  fun getContactById(contactId: Long): SyncContact {
    val contactEntity = contactRepository.findById(contactId)
      .orElseThrow { EntityNotFoundException("Contact with ID $contactId not found") }
    return contactEntity.mapEntityToSyncResponse()
  }

  fun deleteContact(contactId: Long) {
    contactRepository.findById(contactId)
      .orElseThrow { EntityNotFoundException("Contact with ID $contactId not found") }
    contactRepository.deleteById(contactId)
  }

  /**
   * Creation of a contact via sync will accept the NOMIS person_id and use this as
   * the primary key (contactId) in the contact database. There are two different sequence
   * ranges for contactId - one for those created in NOMIS and another for those created
   * in DPS. The ranges can not overlap.
   */
  fun createContact(request: SyncCreateContactRequest): SyncContact {
    if (contactRepository.existsById(request.personId)) {
      val message = "Sync: Duplicate person ID received ${request.personId}"
      logger.error(message)
      throw DuplicatePersonException(message)
    }
    return contactRepository.saveAndFlush(request.mapSyncRequestToEntity()).mapEntityToSyncResponse()
  }

  fun updateContact(contactId: Long, request: SyncUpdateContactRequest): SyncContact {
    val contact = contactRepository.findById(contactId)
      .orElseThrow { EntityNotFoundException("Contact with ID $contactId not found") }

    val changedContact = contact.copy(
      title = request.title,
      firstName = request.firstName,
      lastName = request.lastName,
      middleNames = request.middleName,
      dateOfBirth = request.dateOfBirth,
      deceasedDate = request.deceasedDate,
      staffFlag = request.isStaff,
      gender = request.gender,
      domesticStatus = request.domesticStatus,
      languageCode = request.languageCode,
      interpreterRequired = request.interpreterRequired ?: false,
      updatedBy = request.updatedBy,
      updatedTime = request.updatedTime,
    )

    return contactRepository.saveAndFlush(changedContact).mapEntityToSyncResponse()
  }
}
