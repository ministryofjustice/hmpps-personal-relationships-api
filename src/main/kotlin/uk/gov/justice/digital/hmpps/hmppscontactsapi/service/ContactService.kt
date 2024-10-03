package uk.gov.justice.digital.hmpps.hmppscontactsapi.service

import jakarta.persistence.EntityNotFoundException
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.ContactEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.mapping.toEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.mapping.toModel
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.AddContactRelationshipRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.ContactRelationship
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.ContactSearchRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.CreateContactRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.ContactSearchResultItem
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.GetContactResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.ContactAddressDetailsRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.ContactRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.ContactSearchRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.PrisonerContactRepository
import kotlin.jvm.optionals.getOrNull

@Service
class ContactService(
  private val contactRepository: ContactRepository,
  private val prisonerContactRepository: PrisonerContactRepository,
  private val prisonerService: PrisonerService,
  private val contactSearchRepository: ContactSearchRepository,
  private val contactAddressDetailsRepository: ContactAddressDetailsRepository,
) {
  companion object {
    private val logger = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional
  fun createContact(request: CreateContactRequest): GetContactResponse {
    if (request.relationship != null) {
      validateRelationship(request.relationship)
    }
    val newContact = request.toModel()
    val createdContact = contactRepository.saveAndFlush(newContact)
    val newRelationship = request.relationship?.toEntity(createdContact.contactId, request.createdBy)
      ?.let { prisonerContactRepository.saveAndFlush(it) }

    logger.info("Created new contact {}", createdContact)
    newRelationship?.let { logger.info("Created new relationship {}", newRelationship) }
    return enrichContact(createdContact)
  }

  fun getContact(id: Long): GetContactResponse? {
    return contactRepository.findById(id).getOrNull()
      ?.let { enrichContact(it) }
  }

  fun searchContacts(pageable: Pageable, request: ContactSearchRequest): Page<ContactSearchResultItem> =
    contactSearchRepository.searchContacts(request, pageable).toModel()

  @Transactional
  fun addContactRelationship(contactId: Long, request: AddContactRelationshipRequest) {
    validateRelationship(request.relationship)
    getContact(contactId) ?: throw EntityNotFoundException("Contact ($contactId) could not be found")
    prisonerContactRepository.saveAndFlush(request.relationship.toEntity(contactId, request.createdBy))
  }

  private fun validateRelationship(relationship: ContactRelationship) {
    prisonerService.getPrisoner(relationship.prisonerNumber)
      ?: throw EntityNotFoundException("Prisoner (${relationship.prisonerNumber}) could not be found")
  }

  private fun enrichContact(entity: ContactEntity): GetContactResponse {
    val addresses = contactAddressDetailsRepository.findByContactId(entity.contactId).map { it.toModel() }
    return GetContactResponse(
      id = entity.contactId,
      title = entity.title,
      lastName = entity.lastName,
      firstName = entity.firstName,
      middleName = entity.middleName,
      dateOfBirth = entity.dateOfBirth,
      estimatedIsOverEighteen = entity.estimatedIsOverEighteen,
      isDeceased = entity.isDeceased,
      deceasedDate = entity.deceasedDate,
      addresses = addresses,
      createdBy = entity.createdBy,
      createdTime = entity.createdTime,
    )
  }
}
