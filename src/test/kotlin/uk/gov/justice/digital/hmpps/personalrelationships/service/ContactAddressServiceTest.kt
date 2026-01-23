package uk.gov.justice.digital.hmpps.personalrelationships.service

import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.openapitools.jackson.nullable.JsonNullable
import uk.gov.justice.digital.hmpps.personalrelationships.entity.ContactAddressEntity
import uk.gov.justice.digital.hmpps.personalrelationships.entity.ContactEntity
import uk.gov.justice.digital.hmpps.personalrelationships.helpers.aUser
import uk.gov.justice.digital.hmpps.personalrelationships.integration.helper.isEqualTo
import uk.gov.justice.digital.hmpps.personalrelationships.model.ReferenceCodeGroup
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.address.CreateContactAddressRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.address.PatchContactAddressRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.request.address.UpdateContactAddressRequest
import uk.gov.justice.digital.hmpps.personalrelationships.model.response.ReferenceCode
import uk.gov.justice.digital.hmpps.personalrelationships.repository.ContactAddressPhoneRepository
import uk.gov.justice.digital.hmpps.personalrelationships.repository.ContactAddressRepository
import uk.gov.justice.digital.hmpps.personalrelationships.repository.ContactRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class ContactAddressServiceTest {
  private val contactRepository: ContactRepository = mock()
  private val contactAddressRepository: ContactAddressRepository = mock()
  private val referenceCodeService: ReferenceCodeService = mock()
  private val contactAddressPhoneService: ContactAddressPhoneService = mock()
  private val contactAddressPhoneRepository: ContactAddressPhoneRepository = mock()
  private val contactAddressService = ContactAddressService(contactRepository, contactAddressRepository, referenceCodeService, contactAddressPhoneService, contactAddressPhoneRepository)
  private val referenceData = ReferenceCode(1L, ReferenceCodeGroup.RELATIONSHIP_TYPE, "FRIEND", "Friend", 0, true)

  private val contactId: Long = 1L
  private val contactAddressId: Long = 2L

  private val user = aUser("address")

  @BeforeEach
  fun setUp() {
    whenever(referenceCodeService.validateReferenceCode(any(), any(), any())).thenReturn(referenceData)
  }

  @Test
  fun `should get a contact address by id`() {
    whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(contactEntity(contactId)))
    whenever(contactAddressRepository.findById(contactAddressId)).thenReturn(Optional.of(contactAddressEntity()))

    val contactAddress = contactAddressService.get(contactId, contactAddressId)

    with(contactAddress) {
      assertThat(primaryAddress).isTrue()
      assertThat(flat).isEqualTo("1B")
      assertThat(property).isEqualTo("Mason House")
      assertThat(street).isEqualTo("Main Street")
      assertThat(postcode).isEqualTo("LS13 4KD")
    }

    verify(contactRepository).findById(contactId)
    verify(contactAddressRepository).findById(contactAddressId)
  }

  @Test
  fun `should fail to get a contact address by id`() {
    whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(contactEntity(contactId)))
    whenever(contactAddressRepository.findById(contactAddressId)).thenReturn(Optional.empty())

    assertThrows<EntityNotFoundException> {
      contactAddressService.get(contactId, contactAddressId)
    }

    verify(contactRepository).findById(contactId)
    verify(contactAddressRepository).findById(contactAddressId)
  }

  @Test
  fun `should fail to get an address if the contact is not present`() {
    whenever(contactRepository.findById(contactId)).thenReturn(Optional.empty())
    whenever(contactAddressRepository.findById(contactAddressId)).thenReturn(Optional.of(contactAddressEntity()))

    assertThrows<EntityNotFoundException> {
      contactAddressService.get(contactId, contactAddressId)
    }

    verify(contactRepository).findById(contactId)
    verify(contactAddressRepository, never()).findById(contactAddressId)
  }

  @Test
  fun `should create a contact address`() {
    val request = createContactAddressRequest()
    val contactAddressEntity = request.toEntity(contactId, contactAddressId)

    whenever(contactRepository.findById(contactId))
      .thenReturn(Optional.of(contactEntity()))

    whenever(contactAddressRepository.saveAndFlush(any()))
      .thenReturn(contactAddressEntity)
    whenever(contactAddressPhoneService.createMultipleAddressSpecificPhones(contactId, contactAddressId, "address", request.phoneNumbers))
      .thenReturn(listOf(1L))

    val (contactAddress, _) = contactAddressService.create(contactId, request, user)

    val addressCaptor = argumentCaptor<ContactAddressEntity>()

    verify(contactRepository).findById(contactId)
    verify(contactAddressPhoneService).createMultipleAddressSpecificPhones(contactId, contactAddressId, "address", request.phoneNumbers)
    verify(contactAddressRepository).saveAndFlush(addressCaptor.capture())

    with(addressCaptor.firstValue) {
      assertThat(addressType).isEqualTo(request.addressType)
      assertThat(primaryAddress).isEqualTo(request.primaryAddress)
      assertThat(property).isEqualTo(request.property)
      assertThat(street).isEqualTo(request.street)
      assertThat(area).isEqualTo(request.area)
      assertThat(cityCode).isEqualTo(request.cityCode)
      assertThat(countyCode).isEqualTo(request.countyCode)
      assertThat(countryCode).isEqualTo(request.countryCode)
      assertThat(postCode).isEqualTo(request.postcode)
    }

    // Checks the model response
    with(contactAddress) {
      assertThat(addressType).isEqualTo(request.addressType)
      assertThat(primaryAddress).isEqualTo(request.primaryAddress)
      assertThat(phoneNumberIds).isEqualTo(listOf(1L))
      assertThat(property).isEqualTo(request.property)
      assertThat(street).isEqualTo(request.street)
      assertThat(area).isEqualTo(request.area)
      assertThat(cityCode).isEqualTo(request.cityCode)
      assertThat(countyCode).isEqualTo(request.countyCode)
      assertThat(countyCode).isEqualTo(request.countyCode)
      assertThat(postcode).isEqualTo(request.postcode)
      assertThat(createdBy).isEqualTo("address")
      assertThat(createdTime).isAfterOrEqualTo(LocalDateTime.now().minusMinutes(1))
    }

    verify(contactRepository).findById(contactId)
  }

  @Test
  fun `should unset other primary addresses if this is now the primary contact address`() {
    val request = createContactAddressRequest().copy(primaryAddress = true, mailFlag = false)
    val contactAddressEntity = request.toEntity(contactId, contactAddressId)

    whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(contactEntity()))
    whenever(contactAddressRepository.saveAndFlush(any())).thenReturn(contactAddressEntity)
    whenever(contactAddressRepository.resetPrimaryAddressFlagForContact(contactId)).thenReturn(listOf(987564321L, 123456789L))

    val (_, otherUpdatedAddressIds) = contactAddressService.create(contactId, request, user)

    assertThat(otherUpdatedAddressIds).isEqualTo(setOf(123456789L, 987564321L))
    verify(contactRepository).findById(contactId)
    verify(contactAddressRepository).saveAndFlush(any())
    verify(contactAddressRepository).resetPrimaryAddressFlagForContact(contactId)
    verify(contactAddressRepository, never()).resetMailAddressFlagForContact(contactId)
  }

  @Test
  fun `should unset other mail addresses if this is now the mail contact address`() {
    val request = createContactAddressRequest().copy(primaryAddress = false, mailFlag = true)
    val contactAddressEntity = request.toEntity(contactId, contactAddressId)

    whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(contactEntity()))
    whenever(contactAddressRepository.saveAndFlush(any())).thenReturn(contactAddressEntity)
    whenever(contactAddressRepository.resetMailAddressFlagForContact(contactId)).thenReturn(listOf(987564321L, 123456789L))

    val (_, otherUpdatedAddressIds) = contactAddressService.create(contactId, request, user)

    assertThat(otherUpdatedAddressIds).isEqualTo(setOf(123456789L, 987564321L))
    verify(contactRepository).findById(contactId)
    verify(contactAddressRepository).saveAndFlush(any())
    verify(contactAddressRepository, never()).resetPrimaryAddressFlagForContact(contactId)
    verify(contactAddressRepository).resetMailAddressFlagForContact(contactId)
  }

  @Test
  fun `should unset other primary and mail addresses if this is now the primary and mail contact address`() {
    val request = createContactAddressRequest().copy(primaryAddress = true, mailFlag = true)
    val contactAddressEntity = request.toEntity(contactId, contactAddressId)

    whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(contactEntity()))
    whenever(contactAddressRepository.saveAndFlush(any())).thenReturn(contactAddressEntity)
    whenever(contactAddressRepository.resetPrimaryAddressFlagForContact(contactId)).thenReturn(listOf(999L))
    whenever(contactAddressRepository.resetMailAddressFlagForContact(contactId)).thenReturn(listOf(111L))

    val (_, otherUpdatedAddressIds) = contactAddressService.create(contactId, request, user)

    assertThat(otherUpdatedAddressIds).isEqualTo(setOf(111L, 999L))
    verify(contactRepository).findById(contactId)
    verify(contactAddressRepository).saveAndFlush(any())
    verify(contactAddressRepository).resetPrimaryAddressFlagForContact(contactId)
    verify(contactAddressRepository).resetMailAddressFlagForContact(contactId)
  }

  @Test
  fun `should fail to create a contact address when the contact is not present`() {
    val request = createContactAddressRequest()

    whenever(contactRepository.findById(contactId)).thenReturn(Optional.empty())

    whenever(contactAddressRepository.saveAndFlush(request.toEntity(contactId, contactAddressId)))
      .thenReturn(request.toEntity(contactId, contactAddressId))

    assertThrows<EntityNotFoundException> {
      contactAddressService.create(contactId, request, user)
    }

    verify(contactRepository).findById(contactId)
    verifyNoInteractions(contactAddressRepository)
  }

  @Test
  fun `should throw RuntimeException when saving address phone fails`() {
    val request = createContactAddressRequest()
    val contactAddressEntity = request.toEntity(contactId, contactAddressId)

    whenever(contactRepository.findById(contactId))
      .thenReturn(Optional.of(contactEntity()))

    whenever(contactAddressRepository.saveAndFlush(any()))
      .thenReturn(contactAddressEntity)
    whenever(contactAddressPhoneService.createMultipleAddressSpecificPhones(contactId, contactAddressId, "address", request.phoneNumbers))
      .thenThrow(RuntimeException("Error while saving the address phone!"))

    assertThrows<RuntimeException> {
      contactAddressService.create(contactId, request, user)
    }.message isEqualTo "Error while saving the address phone!"

    verify(contactRepository).findById(contactId)
  }

  @Test
  fun `should delete contact address by id`() {
    whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(contactEntity(contactId)))
    whenever(contactAddressRepository.findById(contactAddressId)).thenReturn(Optional.of(contactAddressEntity()))
    whenever(contactAddressRepository.deleteById(contactAddressId)).then {}

    contactAddressService.delete(contactId, contactAddressId)

    verify(contactRepository).findById(contactId)
    verify(contactAddressRepository).findById(contactAddressId)
    verify(contactAddressRepository).deleteById(contactAddressId)
  }

  @Test
  fun `should fail to delete contact address when the id is not found`() {
    whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(contactEntity(contactId)))
    whenever(contactAddressRepository.findById(contactAddressId)).thenReturn(Optional.empty())

    assertThrows<EntityNotFoundException> {
      contactAddressService.delete(contactId, contactAddressId)
    }

    verify(contactRepository).findById(contactId)
    verify(contactAddressRepository, never()).deleteById(contactAddressId)
  }

  @Test
  fun `should fail to delete contact address when the contact is not found`() {
    whenever(contactRepository.findById(contactId)).thenReturn(Optional.empty())
    whenever(contactAddressRepository.findById(contactAddressId)).thenReturn(Optional.of(contactAddressEntity()))

    assertThrows<EntityNotFoundException> {
      contactAddressService.delete(contactId, contactAddressId)
    }

    verify(contactRepository).findById(contactId)
    verify(contactAddressRepository, never()).deleteById(contactAddressId)
  }

  @Test
  fun `should update a contact address by id`() {
    val request = updateContactAddressRequest()

    whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(contactEntity()))

    whenever(contactAddressRepository.findById(contactAddressId))
      .thenReturn(Optional.of(request.toEntity(contactId, contactAddressId)))

    whenever(contactAddressRepository.saveAndFlush(any()))
      .thenReturn(request.toEntity(contactId, contactAddressId))

    val (updated, _) = contactAddressService.update(contactId, contactAddressId, request, user)

    val addressCaptor = argumentCaptor<ContactAddressEntity>()

    verify(contactAddressRepository).saveAndFlush(addressCaptor.capture())

    // Checks the entity saved
    with(addressCaptor.firstValue) {
      assertThat(addressType).isEqualTo(request.addressType)
      assertThat(primaryAddress).isEqualTo(request.primaryAddress)
      assertThat(property).isEqualTo(request.property)
      assertThat(street).isEqualTo(request.street)
      assertThat(area).isEqualTo(request.area)
      assertThat(cityCode).isEqualTo(request.cityCode)
      assertThat(countyCode).isEqualTo(request.countyCode)
      assertThat(countryCode).isEqualTo(request.countryCode)
      assertThat(postCode).isEqualTo(request.postcode)
      assertThat(updatedBy).isEqualTo("address")
      assertThat(updatedTime).isAfterOrEqualTo(LocalDateTime.now().minusMinutes(1))
    }

    // Checks the model returned
    with(updated) {
      assertThat(addressType).isEqualTo(request.addressType)
      assertThat(primaryAddress).isEqualTo(request.primaryAddress)
      assertThat(property).isEqualTo(request.property)
      assertThat(street).isEqualTo(request.street)
      assertThat(area).isEqualTo(request.area)
      assertThat(cityCode).isEqualTo(request.cityCode)
      assertThat(countyCode).isEqualTo(request.countyCode)
      assertThat(countyCode).isEqualTo(request.countyCode)
      assertThat(postcode).isEqualTo(request.postcode)
      assertThat(updatedBy).isEqualTo("address")
      assertThat(updatedTime).isAfterOrEqualTo(LocalDateTime.now().minusMinutes(1))
    }
  }

  @Test
  fun `should unset other primary addresses if changing this to the primary contact address`() {
    val request = updateContactAddressRequest().copy(primaryAddress = true, mailFlag = false)
    val existAddress = request.toEntity(contactId, contactAddressId).copy(primaryAddress = false, mailFlag = false)

    whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(contactEntity()))
    whenever(contactAddressRepository.findById(contactAddressId)).thenReturn(Optional.of(existAddress))
    whenever(contactAddressRepository.resetPrimaryAddressFlagForContact(contactId)).thenReturn(listOf(987654321L, 123456789L))

    whenever(contactAddressRepository.saveAndFlush(any()))
      .thenReturn(request.toEntity(contactId, contactAddressId))

    val (_, otherUpdatedIds) = contactAddressService.update(contactId, contactAddressId, request, user)

    val addressCaptor = argumentCaptor<ContactAddressEntity>()

    assertThat(otherUpdatedIds).isEqualTo(setOf(123456789L, 987654321L))
    verify(contactAddressRepository).saveAndFlush(addressCaptor.capture())
    verify(contactAddressRepository).resetPrimaryAddressFlagForContact(contactId)
    verify(contactAddressRepository, never()).resetMailAddressFlagForContact(contactId)
  }

  @Test
  fun `should unset other mail addresses if changing this to the mail contact address`() {
    val request = updateContactAddressRequest().copy(primaryAddress = false, mailFlag = true)
    val existAddress = request.toEntity(contactId, contactAddressId).copy(primaryAddress = false, mailFlag = false)

    whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(contactEntity()))
    whenever(contactAddressRepository.findById(contactAddressId)).thenReturn(Optional.of(existAddress))
    whenever(contactAddressRepository.resetMailAddressFlagForContact(contactId)).thenReturn(listOf(987654321L, 123456789L))

    whenever(contactAddressRepository.saveAndFlush(any()))
      .thenReturn(request.toEntity(contactId, contactAddressId))

    val (_, otherUpdatedIds) = contactAddressService.update(contactId, contactAddressId, request, user)

    val addressCaptor = argumentCaptor<ContactAddressEntity>()

    assertThat(otherUpdatedIds).isEqualTo(setOf(123456789L, 987654321L))
    verify(contactAddressRepository).saveAndFlush(addressCaptor.capture())
    verify(contactAddressRepository, never()).resetPrimaryAddressFlagForContact(contactId)
    verify(contactAddressRepository).resetMailAddressFlagForContact(contactId)
  }

  @Test
  fun `should unset other primary and mail addresses if changing this to the primary and mail contact address`() {
    val request = updateContactAddressRequest().copy(primaryAddress = true, mailFlag = true)
    val existAddress = request.toEntity(contactId, contactAddressId).copy(primaryAddress = false, mailFlag = false)

    whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(contactEntity()))
    whenever(contactAddressRepository.findById(contactAddressId)).thenReturn(Optional.of(existAddress))
    whenever(contactAddressRepository.resetPrimaryAddressFlagForContact(contactId)).thenReturn(listOf(999L))
    whenever(contactAddressRepository.resetMailAddressFlagForContact(contactId)).thenReturn(listOf(111L))

    whenever(contactAddressRepository.saveAndFlush(any()))
      .thenReturn(request.toEntity(contactId, contactAddressId))

    val (_, otherUpdatedIds) = contactAddressService.update(contactId, contactAddressId, request, user)

    val addressCaptor = argumentCaptor<ContactAddressEntity>()

    assertThat(otherUpdatedIds).isEqualTo(setOf(111L, 999L))
    verify(contactAddressRepository).saveAndFlush(addressCaptor.capture())
    verify(contactAddressRepository).resetPrimaryAddressFlagForContact(contactId)
    verify(contactAddressRepository).resetMailAddressFlagForContact(contactId)
  }

  @Test
  fun `should fail to update a contact address when the contact is not found`() {
    val updateRequest = updateContactAddressRequest()
    whenever(contactRepository.findById(contactId)).thenReturn(Optional.empty())

    assertThrows<EntityNotFoundException> {
      contactAddressService.update(contactId, contactAddressId, updateRequest, user)
    }

    verify(contactRepository).findById(contactId)
    verifyNoInteractions(contactAddressRepository)
  }

  @Test
  fun `should fail to update a contact address when the address is not found`() {
    val updateRequest = updateContactAddressRequest()

    whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(contactEntity(contactId)))
    whenever(contactAddressRepository.findById(contactAddressId)).thenReturn(Optional.empty())

    assertThrows<EntityNotFoundException> {
      contactAddressService.update(contactId, contactAddressId, updateRequest, user)
    }

    verify(contactRepository).findById(contactId)
    verify(contactAddressRepository).findById(contactAddressId)
    verify(contactAddressRepository, never()).saveAndFlush(any())
  }

  @Nested
  inner class CreateContactAddressReferenceDataValidation {

    val request = createContactAddressRequest()

    @BeforeEach
    fun setUp() {
      val contactAddressEntity = request.toEntity(contactId, contactAddressId)

      whenever(contactRepository.findById(contactId))
        .thenReturn(Optional.of(contactEntity()))

      whenever(contactAddressRepository.saveAndFlush(any()))
        .thenReturn(contactAddressEntity)
    }

    @ParameterizedTest
    @CsvSource(
      value = [
        "CITY;CVNTRY",
        "COUNTY;WARWKS",
        "COUNTRY;UK",
      ],
      delimiter = ';',
    )
    fun `should fail to create a contact address when the reference type not present`(referenceType: ReferenceCodeGroup, referenceValue: String) {
      whenever(referenceCodeService.validateReferenceCode(referenceType, referenceValue, true)).thenThrow(ValidationException("No reference data found for groupCode: $referenceType and code: $referenceValue"))

      val exception = assertThrows<ValidationException> {
        contactAddressService.create(contactId, request, user)
      }

      assertThat(exception.message).isEqualTo("No reference data found for groupCode: $referenceType and code: $referenceValue")
      verify(contactRepository).findById(contactId)
      verifyNoInteractions(contactAddressRepository)
    }
  }

  @Nested
  inner class UpdateContactAddressReferenceDataValidation {

    val request = updateContactAddressRequest()

    @BeforeEach
    fun setUp() {
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(contactEntity()))

      whenever(contactAddressRepository.findById(contactAddressId))
        .thenReturn(Optional.of(request.toEntity(contactId, contactAddressId)))

      whenever(contactAddressRepository.saveAndFlush(any()))
        .thenReturn(request.toEntity(contactId, contactAddressId))
    }

    @ParameterizedTest
    @CsvSource(
      value = [
        "CITY;CVNTRY",
        "COUNTY;WARWKS",
        "COUNTRY;UK",
      ],
      delimiter = ';',
    )
    fun `should fail to update a contact address when the reference type not present`(referenceType: ReferenceCodeGroup, referenceValue: String) {
      whenever(referenceCodeService.validateReferenceCode(referenceType, referenceValue, true)).thenThrow(ValidationException("No reference data found for groupCode: $referenceType and code: $referenceValue"))

      val exception = assertThrows<ValidationException> {
        contactAddressService.update(contactId, contactAddressId, request, user)
      }

      assertThat(exception.message).isEqualTo("No reference data found for groupCode: $referenceType and code: $referenceValue")
      verify(contactRepository).findById(contactId)
      verify(contactAddressRepository).findById(contactAddressId)
      verify(contactAddressRepository, never()).saveAndFlush(any())
    }
  }

  @Nested
  inner class PatchContactAddress {

    val request = patchContactAddressRequest()

    @BeforeEach
    fun setUp() {
      whenever(contactRepository.findById(contactId)).thenReturn(Optional.of(contactEntity()))

      whenever(contactAddressRepository.findById(contactAddressId))
        .thenReturn(Optional.of(request.toEntity(contactId, contactAddressId)))

      whenever(contactAddressRepository.saveAndFlush(any()))
        .thenReturn(request.toEntity(contactId, contactAddressId))
    }

    @Test
    fun `should throw EntityNotFoundException when contact does not exist`() {
      val request = PatchContactAddressRequest()

      whenever(contactRepository.findById(contactId)).thenReturn(Optional.empty())

      assertThrows<EntityNotFoundException> {
        contactAddressService.patch(contactId, contactAddressId, request, user)
      }
    }

    @Test
    fun `should throw EntityNotFoundException when contact address does not exist`() {
      val request = PatchContactAddressRequest()

      whenever(contactAddressRepository.findById(contactAddressId)).thenReturn(Optional.empty())

      assertThrows<EntityNotFoundException> {
        contactAddressService.patch(contactId, contactAddressId, request, user)
      }
    }

    @Test
    fun `should throw EntityNotFoundException when contact address city code does not exist`() {
      val groupCode = ReferenceCodeGroup.CITY
      val cityCode = "BHAM"
      val request = PatchContactAddressRequest(cityCode = JsonNullable.of(cityCode))

      whenever(referenceCodeService.validateReferenceCode(groupCode, cityCode, true)).thenThrow(ValidationException("No reference data found for groupCode: $groupCode and code: $cityCode"))

      val exception = assertThrows<ValidationException> {
        contactAddressService.patch(contactId, contactAddressId, request, user)
      }

      exception.message isEqualTo "No reference data found for groupCode: $groupCode and code: $cityCode"
    }

    @Test
    fun `should throw EntityNotFoundException when contact address county code does not exist`() {
      val groupCode = ReferenceCodeGroup.COUNTY
      val countyCode = "WM"
      val request = PatchContactAddressRequest(countyCode = JsonNullable.of(countyCode))

      whenever(referenceCodeService.validateReferenceCode(groupCode, countyCode, true)).thenThrow(ValidationException("No reference data found for groupCode: $groupCode and code: $countyCode"))

      val exception = assertThrows<ValidationException> {
        contactAddressService.patch(contactId, contactAddressId, request, user)
      }

      exception.message isEqualTo "No reference data found for groupCode: $groupCode and code: $countyCode"
    }

    @Test
    fun `should throw EntityNotFoundException when contact address country code does not exist`() {
      val groupCode = ReferenceCodeGroup.COUNTRY
      val countryCode = "WM"
      val request = PatchContactAddressRequest(countryCode = JsonNullable.of(countryCode))

      whenever(referenceCodeService.validateReferenceCode(groupCode, countryCode, true)).thenThrow(ValidationException("No reference data found for groupCode: $groupCode and code: $countryCode"))

      val exception = assertThrows<ValidationException> {
        contactAddressService.patch(contactId, contactAddressId, request, user)
      }

      exception.message isEqualTo "No reference data found for groupCode: $groupCode and code: $countryCode"
    }

    @Test
    fun `should patch primary address when primary address flag is set and not primary address already`() {
      val request = PatchContactAddressRequest(primaryAddress = JsonNullable.of(true))

      whenever(contactAddressRepository.resetPrimaryAddressFlagForContact(contactId)).thenReturn(listOf(987564321L, 123456789L))

      val (_, otherUpdatedIds) = contactAddressService.patch(contactId, contactAddressId, request, user)

      assertThat(otherUpdatedIds).isEqualTo(setOf(987564321L, 123456789L))
    }

    @Test
    fun `should patch mail address flag when mail flag is set and not mail address already`() {
      val request = PatchContactAddressRequest(mailFlag = JsonNullable.of(true))

      whenever(contactAddressRepository.resetMailAddressFlagForContact(contactId)).thenReturn(listOf(987564321L, 123456789L))

      val (_, otherUpdatedIds) = contactAddressService.patch(contactId, contactAddressId, request, user)

      assertThat(otherUpdatedIds).isEqualTo(setOf(987564321L, 123456789L))
    }

    @Test
    fun `should throw ValidationException when contact address not linked to this address`() {
      val request = PatchContactAddressRequest()

      whenever(contactAddressRepository.findById(contactAddressId)).thenReturn(
        Optional.of(
          contactAddressEntity(
            contactId = 5L,
            contactAddressId = contactAddressId,
          ),
        ),
      )

      val exception = assertThrows<ValidationException> {
        contactAddressService.patch(contactId, contactAddressId, request, user)
      }

      exception.message isEqualTo "Contact ID $contactId is not linked to the address $contactAddressId"
    }

    @Test
    fun `should patch when only the updated by field is provided`() {
      val request = PatchContactAddressRequest()

      contactAddressService.patch(contactId, contactAddressId, request, user)

      val contactCaptor = argumentCaptor<ContactAddressEntity>()

      verify(contactAddressRepository).saveAndFlush(contactCaptor.capture())
      val updatingEntity = contactCaptor.firstValue
      assertThat(updatingEntity.updatedBy).isEqualTo("address")
    }

    @Test
    fun `should patch when only the primaryAddress field is provided and is set to false when existing address is primary address`() {
      val request = PatchContactAddressRequest(primaryAddress = JsonNullable.of(false))

      whenever(contactAddressRepository.findById(contactAddressId)).thenReturn(Optional.of(contactAddressEntity(primaryAddress = true)))
      contactAddressService.patch(contactId, contactAddressId, request, user)

      val contactCaptor = argumentCaptor<ContactAddressEntity>()

      verify(contactAddressRepository).saveAndFlush(contactCaptor.capture())
      val updatingEntity = contactCaptor.firstValue
      assertThat(updatingEntity.primaryAddress).isEqualTo(request.primaryAddress.get())
    }

    @Test
    fun `should patch when only the address type field is provided`() {
      val request = PatchContactAddressRequest(addressType = JsonNullable.of("HOME"))

      contactAddressService.patch(contactId, contactAddressId, request, user)

      val contactCaptor = argumentCaptor<ContactAddressEntity>()

      verify(contactAddressRepository).saveAndFlush(contactCaptor.capture())
      val updatingEntity = contactCaptor.firstValue
      assertThat(updatingEntity.addressType).isEqualTo(request.addressType.get())
    }

    @Test
    fun `should patch when only the flat field is provided`() {
      val request = PatchContactAddressRequest(flat = JsonNullable.of("1 A"))

      contactAddressService.patch(contactId, contactAddressId, request, user)

      val contactCaptor = argumentCaptor<ContactAddressEntity>()

      verify(contactAddressRepository).saveAndFlush(contactCaptor.capture())
      val updatingEntity = contactCaptor.firstValue
      assertThat(updatingEntity.flat).isEqualTo(request.flat.get())
    }

    @Test
    fun `should patch when only the property field is provided`() {
      val request = PatchContactAddressRequest(property = JsonNullable.of("No 20"))

      contactAddressService.patch(contactId, contactAddressId, request, user)

      val contactCaptor = argumentCaptor<ContactAddressEntity>()

      verify(contactAddressRepository).saveAndFlush(contactCaptor.capture())
      val updatingEntity = contactCaptor.firstValue
      assertThat(updatingEntity.property).isEqualTo(request.property.get())
    }

    @Test
    fun `should patch when only the street field is provided`() {
      val request = PatchContactAddressRequest(street = JsonNullable.of("Bluebell Cress"))

      contactAddressService.patch(contactId, contactAddressId, request, user)

      val contactCaptor = argumentCaptor<ContactAddressEntity>()

      verify(contactAddressRepository).saveAndFlush(contactCaptor.capture())
      val updatingEntity = contactCaptor.firstValue
      assertThat(updatingEntity.street).isEqualTo(request.street.get())
    }

    @Test
    fun `should patch when only the area field is provided`() {
      val request = PatchContactAddressRequest(area = JsonNullable.of("West midlands"))

      contactAddressService.patch(contactId, contactAddressId, request, user)

      val contactCaptor = argumentCaptor<ContactAddressEntity>()

      verify(contactAddressRepository).saveAndFlush(contactCaptor.capture())
      val updatingEntity = contactCaptor.firstValue
      assertThat(updatingEntity.area).isEqualTo(request.area.get())
    }

    @Test
    fun `should patch when only the cityCode field is provided`() {
      val request = PatchContactAddressRequest(cityCode = JsonNullable.of("Birmingham"))

      contactAddressService.patch(contactId, contactAddressId, request, user)

      val contactCaptor = argumentCaptor<ContactAddressEntity>()

      verify(contactAddressRepository).saveAndFlush(contactCaptor.capture())
      val updatingEntity = contactCaptor.firstValue
      assertThat(updatingEntity.cityCode).isEqualTo(request.cityCode.get())
    }

    @Test
    fun `should patch when only the countyCode field is provided`() {
      val request = PatchContactAddressRequest(countyCode = JsonNullable.of("WM"))

      contactAddressService.patch(contactId, contactAddressId, request, user)

      val contactCaptor = argumentCaptor<ContactAddressEntity>()

      verify(contactAddressRepository).saveAndFlush(contactCaptor.capture())
      val updatingEntity = contactCaptor.firstValue
      assertThat(updatingEntity.countyCode).isEqualTo(request.countyCode.get())
    }

    @Test
    fun `should patch when only the countryCode field is provided`() {
      val request = PatchContactAddressRequest(countryCode = JsonNullable.of("UK"))

      contactAddressService.patch(contactId, contactAddressId, request, user)

      val contactCaptor = argumentCaptor<ContactAddressEntity>()

      verify(contactAddressRepository).saveAndFlush(contactCaptor.capture())
      val updatingEntity = contactCaptor.firstValue
      assertThat(updatingEntity.countryCode).isEqualTo(request.countryCode.get())
    }

    @Test
    fun `should patch when only the postcode field is provided`() {
      val request = PatchContactAddressRequest(postcode = JsonNullable.of("B42 2FS"))

      contactAddressService.patch(contactId, contactAddressId, request, user)

      val contactCaptor = argumentCaptor<ContactAddressEntity>()

      verify(contactAddressRepository).saveAndFlush(contactCaptor.capture())
      val updatingEntity = contactCaptor.firstValue
      assertThat(updatingEntity.postCode).isEqualTo(request.postcode.get())
    }

    @Test
    fun `should patch when only the verified field is provided`() {
      val request = PatchContactAddressRequest(verified = JsonNullable.of(true))

      contactAddressService.patch(contactId, contactAddressId, request, user)

      val contactCaptor = argumentCaptor<ContactAddressEntity>()

      verify(contactAddressRepository).saveAndFlush(contactCaptor.capture())
      val updatingEntity = contactCaptor.firstValue
      assertThat(updatingEntity.verified).isEqualTo(request.verified.get())
    }

    @Test
    fun `should patch when only the mailFlag field is provided`() {
      val request = PatchContactAddressRequest(mailFlag = JsonNullable.of(true))

      contactAddressService.patch(contactId, contactAddressId, request, user)

      val contactCaptor = argumentCaptor<ContactAddressEntity>()

      verify(contactAddressRepository).saveAndFlush(contactCaptor.capture())
      val updatingEntity = contactCaptor.firstValue
      assertThat(updatingEntity.mailFlag).isEqualTo(request.mailFlag.get())
    }

    @Test
    fun `should patch when only the startDate field is provided`() {
      val request = PatchContactAddressRequest(startDate = JsonNullable.of(LocalDate.of(2020, 4, 5)))

      contactAddressService.patch(contactId, contactAddressId, request, user)

      val contactCaptor = argumentCaptor<ContactAddressEntity>()

      verify(contactAddressRepository).saveAndFlush(contactCaptor.capture())
      val updatingEntity = contactCaptor.firstValue
      assertThat(updatingEntity.startDate).isEqualTo(request.startDate.get())
    }

    @Test
    fun `should patch when only the endDate field is provided`() {
      val request = PatchContactAddressRequest(endDate = JsonNullable.of(LocalDate.of(2020, 4, 5)))

      contactAddressService.patch(contactId, contactAddressId, request, user)

      val contactCaptor = argumentCaptor<ContactAddressEntity>()

      verify(contactAddressRepository).saveAndFlush(contactCaptor.capture())
      val updatingEntity = contactCaptor.firstValue
      assertThat(updatingEntity.endDate).isEqualTo(request.endDate.get())
    }

    @Test
    fun `should patch when only the noFixedAddress field is provided`() {
      val request = PatchContactAddressRequest(noFixedAddress = JsonNullable.of(true))

      contactAddressService.patch(contactId, contactAddressId, request, user)

      val contactCaptor = argumentCaptor<ContactAddressEntity>()

      verify(contactAddressRepository).saveAndFlush(contactCaptor.capture())
      val updatingEntity = contactCaptor.firstValue
      assertThat(updatingEntity.noFixedAddress).isEqualTo(request.noFixedAddress.get())
    }

    @Test
    fun `should patch when only the comments field is provided`() {
      val request = PatchContactAddressRequest(comments = JsonNullable.of("comments"))

      contactAddressService.patch(contactId, contactAddressId, request, user)

      val contactCaptor = argumentCaptor<ContactAddressEntity>()

      verify(contactAddressRepository).saveAndFlush(contactCaptor.capture())
      val updatingEntity = contactCaptor.firstValue
      assertThat(updatingEntity.comments).isEqualTo(request.comments.get())
    }

    @Test
    fun `should patch updatedTime when only the updatedBy field is provided`() {
      val request = PatchContactAddressRequest()

      contactAddressService.patch(contactId, contactAddressId, request, user)

      val contactCaptor = argumentCaptor<ContactAddressEntity>()

      verify(contactAddressRepository).saveAndFlush(contactCaptor.capture())
      val updatingEntity = contactCaptor.firstValue
      assertThat(updatingEntity.updatedTime).isInThePast()
    }

    @Test
    fun `should patch verifiedBy when only the verified field is provided`() {
      val request = PatchContactAddressRequest(
        verified = JsonNullable.of(true),
      )

      contactAddressService.patch(contactId, contactAddressId, request, user)

      val contactCaptor = argumentCaptor<ContactAddressEntity>()

      verify(contactAddressRepository).saveAndFlush(contactCaptor.capture())
      val updatingEntity = contactCaptor.firstValue
      assertThat(updatingEntity.verifiedBy).isEqualTo("address")
    }

    @Test
    fun `should patch verifiedTime when only the verified field is provided`() {
      val request = PatchContactAddressRequest(
        verified = JsonNullable.of(true),
      )

      contactAddressService.patch(contactId, contactAddressId, request, user)

      val contactCaptor = argumentCaptor<ContactAddressEntity>()

      verify(contactAddressRepository).saveAndFlush(contactCaptor.capture())
      val updatingEntity = contactCaptor.firstValue
      assertThat(updatingEntity.verifiedTime).isInThePast()
    }
  }
}

private fun updateContactAddressRequest() = UpdateContactAddressRequest(
  addressType = "HOME",
  primaryAddress = true,
  property = "13",
  street = "Main Street",
  area = "Dodworth",
  cityCode = "CVNTRY",
  countyCode = "WARWKS",
  postcode = "CV4 9NJ",
  countryCode = "UK",
)

private fun patchContactAddressRequest() = PatchContactAddressRequest(
  addressType = JsonNullable.of("HOME"),
  primaryAddress = JsonNullable.of(false),
  flat = JsonNullable.of("13"),
  property = JsonNullable.of("13"),
  street = JsonNullable.of("Main Street"),
  area = JsonNullable.of("Dodworth"),
  cityCode = JsonNullable.of("CVNTRY"),
  countyCode = JsonNullable.of("WARWKS"),
  postcode = JsonNullable.of("CV4 9NJ"),
  countryCode = JsonNullable.of("UK"),
  verified = JsonNullable.of(false),
  mailFlag = JsonNullable.of(false),
  startDate = JsonNullable.of(LocalDate.of(2000, 12, 25)),
  endDate = JsonNullable.of(LocalDate.of(2001, 12, 25)),
  noFixedAddress = JsonNullable.of(false),
  comments = JsonNullable.of("UK"),
)

private fun createContactAddressRequest() = CreateContactAddressRequest(
  addressType = "HOME",
  primaryAddress = true,
  property = "13",
  street = "Main Street",
  area = "Dodworth",
  cityCode = "CVNTRY",
  countyCode = "WARWKS",
  postcode = "CV4 9NJ",
  countryCode = "UK",
)

private fun contactEntity(contactId: Long = 1L) = ContactEntity(
  contactId = contactId,
  title = "Mr",
  firstName = "John",
  middleNames = null,
  lastName = "Smith",
  dateOfBirth = null,
  deceasedDate = null,
  createdBy = "TEST",
)

private fun contactAddressEntity(contactId: Long = 1L, contactAddressId: Long = 1L, primaryAddress: Boolean = true) = ContactAddressEntity(
  contactAddressId = contactAddressId,
  contactId = contactId,
  addressType = "HOME",
  primaryAddress = primaryAddress,
  flat = "1B",
  property = "Mason House",
  street = "Main Street",
  area = "Howarth",
  cityCode = "LEEDS",
  countyCode = "YORKS",
  postCode = "LS13 4KD",
  countryCode = "UK",
  createdBy = "TEST",
  createdTime = LocalDateTime.now(),
)

private fun CreateContactAddressRequest.toEntity(contactId: Long, contactAddressId: Long = 0) = ContactAddressEntity(
  contactAddressId = contactAddressId,
  contactId = contactId,
  addressType = this.addressType,
  primaryAddress = this.primaryAddress,
  flat = this.flat,
  property = this.property,
  street = this.street,
  area = this.area,
  cityCode = this.cityCode,
  countyCode = this.countyCode,
  postCode = this.postcode,
  countryCode = this.countryCode,
  createdBy = "address",
  createdTime = LocalDateTime.now(),
)

private fun UpdateContactAddressRequest.toEntity(contactId: Long, contactAddressId: Long = 1L): ContactAddressEntity = ContactAddressEntity(
  contactAddressId = contactAddressId,
  contactId = contactId,
  addressType = this.addressType,
  primaryAddress = this.primaryAddress,
  flat = this.flat,
  property = this.property,
  street = this.street,
  area = this.area,
  cityCode = this.cityCode,
  countyCode = this.countyCode,
  postCode = this.postcode,
  countryCode = this.countryCode,
  createdBy = "TEST",
  createdTime = LocalDateTime.now(),
).also {
  it.updatedBy = "address"
  it.updatedTime = LocalDateTime.now()
}

private fun PatchContactAddressRequest.toEntity(contactId: Long, contactAddressId: Long = 1L): ContactAddressEntity = ContactAddressEntity(
  contactAddressId = contactAddressId,
  contactId = contactId,
  addressType = this.addressType.orElse(""),
  primaryAddress = this.primaryAddress.orElse(true),
  flat = this.flat.orElse(""),
  property = this.property.orElse(""),
  street = this.street.orElse(""),
  area = this.area.orElse(""),
  cityCode = this.cityCode.orElse(""),
  countyCode = this.countyCode.orElse(""),
  postCode = this.postcode.orElse(""),
  countryCode = this.countryCode.orElse(""),
  createdBy = "TEST",
  createdTime = LocalDateTime.now(),
).also {
  it.updatedBy = "address"
  it.updatedTime = LocalDateTime.now()
}
