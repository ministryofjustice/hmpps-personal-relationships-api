package uk.gov.justice.digital.hmpps.personalrelationships.service

import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.personalrelationships.config.User
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.restrictions.CreateContactRestrictionRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.restrictions.CreatePrisonerContactRestrictionRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.restrictions.UpdateContactRestrictionRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.restrictions.UpdatePrisonerContactRestrictionRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.ContactRestrictionDetails
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.ContactRestrictions
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.ContactsRestrictionsResponse
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.GlobalContactRestriction
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.PrisonerContactRestriction
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.PrisonerContactRestrictionDetails
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.PrisonerContactRestrictions
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.PrisonerContactRestrictionsResponse
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.PrisonerContactsRestrictionsResponse
import uk.gov.justice.digital.hmpps.personalrelationships.repository.ContactRepository
import uk.gov.justice.digital.hmpps.personalrelationships.repository.ContactRestrictionDetailsRepository
import uk.gov.justice.digital.hmpps.personalrelationships.repository.PrisonerContactRepository
import uk.gov.justice.digital.hmpps.personalrelationships.repository.PrisonerContactRestrictionDetailsRepository
import java.time.LocalDate

@Service
class RestrictionsService(
  private val contactRestrictionDetailsRepository: ContactRestrictionDetailsRepository,
  private val contactRepository: ContactRepository,
  private val prisonerContactRepository: PrisonerContactRepository,
  private val prisonerContactRestrictionDetailsRepository: PrisonerContactRestrictionDetailsRepository,
  private val manageUsersService: ManageUsersService,
  private val transactionalRestrictionsService: TransactionalRestrictionsService,
) {

  fun getGlobalRestrictionsForContact(contactId: Long): List<ContactRestrictionDetails> {
    validateContactExists(contactId)
    return getGlobalRestrictionDetails(contactId)
  }

  fun getGlobalRestrictionsForContacts(contactIds: Set<Long>): ContactsRestrictionsResponse {
    val contactRestrictions = buildList {
      contactRepository.findAllById(contactIds)
        .forEach { contact ->
          val contactId = contact.contactId!!

          add(
            ContactRestrictions(
              contactId = contactId,
              globalContactRestrictions = getGlobalRestrictionDetails(contactId),
            ),
          )
        }
    }

    return ContactsRestrictionsResponse(contactRestrictions)
  }

  fun getPrisonerContactRestrictions(prisonerContactId: Long): PrisonerContactRestrictionsResponse {
    val prisonerContact = prisonerContactRepository.findById(prisonerContactId)
      .orElseThrow { EntityNotFoundException("Prisoner contact ($prisonerContactId) could not be found") }
    val restrictionsWithEnteredBy = prisonerContactRestrictionDetailsRepository.findAllByPrisonerContactId(
      prisonerContactId,
    ).map { entity -> entity to (entity.updatedBy ?: entity.createdBy) }
    val enteredByMap = restrictionsWithEnteredBy
      .map { (_, enteredByUsername) -> enteredByUsername }
      .toSet().associateWith { enteredByUsername -> manageUsersService.getUserByUsername(enteredByUsername)?.name ?: enteredByUsername }
    return PrisonerContactRestrictionsResponse(
      prisonerContactRestrictions = restrictionsWithEnteredBy.map { (entity, enteredByUsername) ->
        PrisonerContactRestrictionDetails(
          prisonerContactRestrictionId = entity.prisonerContactRestrictionId,
          prisonerContactId = prisonerContactId,
          contactId = prisonerContact.contactId,
          prisonerNumber = prisonerContact.prisonerNumber,
          restrictionType = entity.restrictionType,
          restrictionTypeDescription = entity.restrictionTypeDescription,
          startDate = entity.startDate,
          expiryDate = entity.expiryDate,
          comments = entity.comments,
          enteredByUsername = enteredByUsername,
          enteredByDisplayName = enteredByMap[enteredByUsername] ?: enteredByUsername,
          createdBy = entity.createdBy,
          createdTime = entity.createdTime,
          updatedBy = entity.updatedBy,
          updatedTime = entity.updatedTime,
        )
      },
      contactGlobalRestrictions = getGlobalRestrictionsForContact(prisonerContact.contactId),
    )
  }

  fun getPrisonerContactRestrictions(prisonerContactIds: Set<Long>): PrisonerContactsRestrictionsResponse {
    val prisonerContactRestrictions = buildList {
      prisonerContactRepository.findAllById(prisonerContactIds)
        .forEach { prisonerContact ->

          val prisonerContactRestrictions = run {
            val restrictionsWithEnteredBy = prisonerContactRestrictionDetailsRepository.findAllByPrisonerContactId(prisonerContact.prisonerContactId)
              .map { entity -> entity to (entity.updatedBy ?: entity.createdBy) }

            val enteredByMap = restrictionsWithEnteredBy
              .map { (_, enteredByUsername) -> enteredByUsername }
              .toSet().associateWith { enteredByUsername -> manageUsersService.getUserByUsername(enteredByUsername)?.name ?: enteredByUsername }

            restrictionsWithEnteredBy.map { (entity, enteredByUsername) ->
              PrisonerContactRestriction(
                prisonerContactRestrictionId = entity.prisonerContactRestrictionId,
                prisonerContactId = prisonerContact.prisonerContactId,
                contactId = prisonerContact.contactId,
                prisonerNumber = prisonerContact.prisonerNumber,
                restrictionType = entity.restrictionType,
                restrictionTypeDescription = entity.restrictionTypeDescription,
                startDate = entity.startDate,
                expiryDate = entity.expiryDate,
                comments = entity.comments,
                enteredByDisplayName = enteredByMap[enteredByUsername] ?: enteredByUsername,
              )
            }
          }

          val globalContactRestrictions = run {
            val restrictionsWithEnteredBy = contactRestrictionDetailsRepository.findAllByContactId(prisonerContact.contactId)
              .map { entity -> entity to (entity.updatedBy ?: entity.createdBy) }

            val enteredByMap = restrictionsWithEnteredBy
              .map { (_, enteredByUsername) -> enteredByUsername }
              .toSet().associateWith { enteredByUsername -> manageUsersService.getUserByUsername(enteredByUsername)?.name ?: enteredByUsername }

            restrictionsWithEnteredBy.map { (entity, enteredByUsername) ->
              GlobalContactRestriction(
                contactRestrictionId = entity.contactRestrictionId,
                contactId = entity.contactId,
                restrictionType = entity.restrictionType,
                restrictionTypeDescription = entity.restrictionTypeDescription,
                startDate = entity.startDate,
                expiryDate = entity.expiryDate,
                comments = entity.comments,
                enteredByDisplayName = enteredByMap[enteredByUsername] ?: enteredByUsername,
              )
            }
          }

          add(
            PrisonerContactRestrictions(
              prisonerContactId = prisonerContact.prisonerContactId,
              prisonerContactRestrictions = prisonerContactRestrictions,
              globalContactRestrictions = globalContactRestrictions,
            ),
          )
        }
    }

    return PrisonerContactsRestrictionsResponse(prisonerContactRestrictions)
  }

  fun createContactGlobalRestriction(
    contactId: Long,
    request: CreateContactRestrictionRequest,
    user: User,
  ): ContactRestrictionDetails {
    validateExpiryDateBeforeStartDate(request.startDate, request.expiryDate)
    val enteredByDisplayName = getUserDisplayName(user.username)
    return transactionalRestrictionsService.createContactGlobalRestriction(
      contactId,
      request,
      user,
      enteredByDisplayName,
    )
  }

  private fun validateExpiryDateBeforeStartDate(startDate: LocalDate, expiryDate: LocalDate?) {
    if (expiryDate != null && startDate.isAfter(expiryDate)) {
      throw ValidationException("Restriction start date should be before the restriction end date")
    }
  }

  fun updateContactGlobalRestriction(
    contactId: Long,
    contactRestrictionId: Long,
    request: UpdateContactRestrictionRequest,
    user: User,
  ): ContactRestrictionDetails {
    validateExpiryDateBeforeStartDate(request.startDate, request.expiryDate)
    val enteredByDisplayName = getUserDisplayName(user.username)
    return transactionalRestrictionsService.updateContactGlobalRestriction(
      contactId,
      contactRestrictionId,
      request,
      user,
      enteredByDisplayName,
    )
  }

  fun createPrisonerContactRestriction(
    prisonerContactId: Long,
    request: CreatePrisonerContactRestrictionRequest,
    user: User,
  ): PrisonerContactRestrictionDetails {
    val enteredByDisplayName = getUserDisplayName(user.username)
    return transactionalRestrictionsService.createPrisonerContactRestriction(
      prisonerContactId,
      request,
      user,
      enteredByDisplayName,
    )
  }

  fun updatePrisonerContactRestriction(
    prisonerContactId: Long,
    prisonerContactRestrictionId: Long,
    request: UpdatePrisonerContactRestrictionRequest,
    user: User,
  ): PrisonerContactRestrictionDetails {
    val enteredByDisplayName = getUserDisplayName(user.username)
    return transactionalRestrictionsService.updatePrisonerContactRestriction(
      prisonerContactId,
      prisonerContactRestrictionId,
      request,
      user,
      enteredByDisplayName,
    )
  }

  private fun getUserDisplayName(username: String) = manageUsersService.getUserByUsername(username)?.name ?: username

  private fun validateContactExists(contactId: Long) {
    contactRepository.findById(contactId)
      .orElseThrow { EntityNotFoundException("Contact ($contactId) could not be found") }
  }

  private fun getGlobalRestrictionDetails(contactId: Long): List<ContactRestrictionDetails> {
    val restrictionsWithEnteredBy = contactRestrictionDetailsRepository.findAllByContactId(contactId)
      .map { entity -> entity to (entity.updatedBy ?: entity.createdBy) }

    val enteredByMap = restrictionsWithEnteredBy
      .map { (_, enteredByUsername) -> enteredByUsername }
      .toSet()
      .associateWith { enteredByUsername ->
        manageUsersService.getUserByUsername(enteredByUsername)?.name ?: enteredByUsername
      }

    return restrictionsWithEnteredBy.map { (entity, enteredByUsername) ->
      ContactRestrictionDetails(
        contactRestrictionId = entity.contactRestrictionId,
        contactId = entity.contactId,
        restrictionType = entity.restrictionType,
        restrictionTypeDescription = entity.restrictionTypeDescription,
        startDate = entity.startDate,
        expiryDate = entity.expiryDate,
        comments = entity.comments,
        enteredByUsername = enteredByUsername,
        enteredByDisplayName = enteredByMap[enteredByUsername] ?: enteredByUsername,
        createdBy = entity.createdBy,
        createdTime = entity.createdTime,
        updatedBy = entity.updatedBy,
        updatedTime = entity.updatedTime,
      )
    }
  }
}
