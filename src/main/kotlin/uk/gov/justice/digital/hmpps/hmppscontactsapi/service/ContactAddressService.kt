package uk.gov.justice.digital.hmpps.hmppscontactsapi.service

import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppscontactsapi.config.User
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.ContactAddressEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.mapping.toEntity
import uk.gov.justice.digital.hmpps.hmppscontactsapi.mapping.toModel
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.ReferenceCodeGroup
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.address.CreateContactAddressRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.address.PatchContactAddressRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.address.UpdateContactAddressRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.ContactAddressResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.CreateAddressResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.UpdateAddressResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.ContactAddressPhoneRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.ContactAddressRepository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.repository.ContactRepository
import java.time.LocalDateTime

@Service
@Transactional
class ContactAddressService(
  private val contactRepository: ContactRepository,
  private val contactAddressRepository: ContactAddressRepository,
  private val referenceCodeService: ReferenceCodeService,
  private val contactAddressPhoneService: ContactAddressPhoneService,
  private val contactAddressPhoneRepository: ContactAddressPhoneRepository,
) {
  companion object {
    private val logger = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional(readOnly = true)
  fun get(contactId: Long, contactAddressId: Long): ContactAddressResponse {
    val contact = validateContactExists(contactId)
    val existing = validateExistingAddress(contactAddressId)

    if (contact.contactId != existing.contactId) {
      logger.error("Contact address specified for a contact not linked to this address")
      throw ValidationException("Contact ID ${contact.contactId} is not linked to the address ${existing.contactAddressId}")
    }

    return existing.toModel()
  }

  fun create(contactId: Long, request: CreateContactAddressRequest, user: User): CreateAddressResponse {
    validateContactExists(contactId)
    validateAddressType(request.addressType)
    validateCityCode(request.cityCode)
    validateCountyCode(request.countyCode)
    validateCountryCode(request.countryCode)

    val updatedAddressIds = mutableSetOf<Long>()
    if (request.primaryAddress) {
      updatedAddressIds += contactAddressRepository.resetPrimaryAddressFlagForContact(contactId)
    }
    if (request.mailFlag != null && request.mailFlag) {
      updatedAddressIds += contactAddressRepository.resetMailAddressFlagForContact(contactId)
    }
    val savedContactAddress = contactAddressRepository.saveAndFlush(request.toEntity(contactId, user))

    val contactAddressPhoneId = contactAddressPhoneService.createMultipleAddressSpecificPhones(
      contactId,
      savedContactAddress.contactAddressId,
      user.username,
      request.phoneNumbers,
    )
    val savedContactAddressModel = savedContactAddress.toModel(contactAddressPhoneId)
    return CreateAddressResponse(savedContactAddressModel, updatedAddressIds)
  }

  fun update(contactId: Long, contactAddressId: Long, request: UpdateContactAddressRequest, user: User): UpdateAddressResponse {
    val contact = validateContactExists(contactId)
    val existing = validateExistingAddress(contactAddressId)
    validateAddressType(request.addressType)
    validateCityCode(request.cityCode)
    validateCountyCode(request.countyCode)
    validateCountryCode(request.countryCode)
    val updatedAddressIds = mutableSetOf<Long>()
    if (request.primaryAddress && !existing.primaryAddress) {
      updatedAddressIds += contactAddressRepository.resetPrimaryAddressFlagForContact(contactId)
    }
    if (request.mailFlag != null && request.mailFlag && !existing.mailFlag) {
      updatedAddressIds += contactAddressRepository.resetMailAddressFlagForContact(contactId)
    }
    if (contact.contactId != existing.contactId) {
      logger.error("Contact address update specified for a contact not linked to this address")
      throw ValidationException("Contact ID ${contact.contactId} is not linked to the address ${existing.contactAddressId}")
    }

    val changedContactAddress = existing.copy(
      primaryAddress = request.primaryAddress,
      addressType = request.addressType,
      flat = request.flat,
      property = request.property,
      street = request.street,
      area = request.area,
      cityCode = request.cityCode,
      countyCode = request.countyCode,
      countryCode = request.countryCode,
      postCode = request.postcode,
      verified = request.verified,
      mailFlag = request.mailFlag ?: false,
      startDate = request.startDate,
      endDate = request.endDate,
      noFixedAddress = request.noFixedAddress ?: false,
      comments = request.comments,
    ).also {
      it.updatedBy = user.username
      it.updatedTime = LocalDateTime.now()
      if (!existing.verified && request.verified) {
        it.verifiedBy = user.username
        it.verifiedTime = LocalDateTime.now()
      }
    }

    return UpdateAddressResponse(contactAddressRepository.saveAndFlush(changedContactAddress).toModel(), updatedAddressIds)
  }

  fun patch(contactId: Long, contactAddressId: Long, request: PatchContactAddressRequest, user: User): UpdateAddressResponse {
    val contact = validateContactExists(contactId)
    val existing = validateExistingAddress(contactAddressId)
    if (contact.contactId != existing.contactId) {
      logger.error("Contact address update specified for a contact not linked to this address")
      throw ValidationException("Contact ID ${contact.contactId} is not linked to the address ${existing.contactAddressId}")
    }
    request.addressType.ifPresent { validateAddressType(it) }
    request.cityCode.ifPresent { validateCityCode(it) }
    request.countyCode.ifPresent { validateCountyCode(it) }
    request.countryCode.ifPresent { validateCountryCode(it) }
    val updatedAddressIds = mutableSetOf<Long>()

    request.primaryAddress.ifPresent {
      if (it && !existing.primaryAddress) {
        updatedAddressIds += contactAddressRepository.resetPrimaryAddressFlagForContact(contactId)
      }
    }

    request.mailFlag.ifPresent {
      if (it && !existing.mailFlag) {
        updatedAddressIds += contactAddressRepository.resetMailAddressFlagForContact(contactId)
      }
    }

    val changedContactAddress = existing.patchRequest(request, user)

    return UpdateAddressResponse(contactAddressRepository.saveAndFlush(changedContactAddress).toModel(), updatedAddressIds)
  }

  private fun ContactAddressEntity.patchRequest(
    request: PatchContactAddressRequest,
    user: User,
  ): ContactAddressEntity {
    val changedContactAddress = this.copy(
      primaryAddress = request.primaryAddress.orElse(this.primaryAddress),
      addressType = request.addressType.orElse(this.addressType),
      flat = request.flat.orElse(this.flat),
      property = request.property.orElse(this.property),
      street = request.street.orElse(this.street),
      area = request.area.orElse(this.area),
      cityCode = request.cityCode.orElse(this.cityCode),
      countyCode = request.countyCode.orElse(this.countyCode),
      countryCode = request.countryCode.orElse(this.countryCode),
      postCode = request.postcode.orElse(this.postCode),
      verified = request.verified.orElse(this.verified),
      mailFlag = request.mailFlag.orElse(this.mailFlag),
      startDate = request.startDate.orElse(this.startDate),
      endDate = request.endDate.orElse(this.endDate),
      noFixedAddress = request.noFixedAddress.orElse(this.noFixedAddress),
      comments = request.comments.orElse(this.comments),
    ).also { entity ->
      entity.updatedBy = user.username
      entity.updatedTime = LocalDateTime.now()
      request.verified.ifPresent {
        if (it && !this.verified) {
          entity.verifiedBy = user.username
          entity.verifiedTime = LocalDateTime.now()
        }
      }
    }
    return changedContactAddress
  }

  fun delete(contactId: Long, contactAddressId: Long): ContactAddressResponse {
    val contact = validateContactExists(contactId)
    val rowToDelete = validateExistingAddress(contactAddressId)
    contactAddressPhoneRepository.deleteAll(contactAddressPhoneRepository.findByContactId(contactId))

    if (contact.contactId != rowToDelete.contactId) {
      logger.error("Contact address delete specified for a contact not linked to this address")
      throw ValidationException("Contact ID ${contact.contactId} is not linked to the address ${rowToDelete.contactAddressId}")
    }

    contactAddressRepository.deleteById(contactAddressId)

    return rowToDelete.toModel()
  }

  private fun validateContactExists(contactId: Long) = contactRepository
    .findById(contactId)
    .orElseThrow { EntityNotFoundException("Contact ($contactId) not found") }

  private fun validateCountryCode(countryCode: String) {
    validateReferenceDataExists(countryCode, ReferenceCodeGroup.COUNTRY)
  }

  private fun validateCountyCode(countyCode: String?) {
    countyCode?.let { validateReferenceDataExists(it, ReferenceCodeGroup.COUNTY) }
  }

  private fun validateCityCode(cityCode: String?) {
    cityCode?.let { validateReferenceDataExists(it, ReferenceCodeGroup.CITY) }
  }

  private fun validateAddressType(addressType: String?) {
    addressType?.let { validateReferenceDataExists(it, ReferenceCodeGroup.ADDRESS_TYPE) }
  }

  private fun validateReferenceDataExists(code: String, groupCode: ReferenceCodeGroup) = referenceCodeService.validateReferenceCode(groupCode, code, true)

  private fun validateExistingAddress(contactAddressId: Long) = contactAddressRepository
    .findById(contactAddressId)
    .orElseThrow { EntityNotFoundException("Contact address ($contactAddressId) not found") }
}
