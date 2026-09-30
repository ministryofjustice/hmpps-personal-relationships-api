package uk.gov.justice.digital.hmpps.personalrelationships.model.internal

import uk.gov.justice.digital.hmpps.personalrelationships.entity.ContactAddressDetailsEntity
import uk.gov.justice.digital.hmpps.personalrelationships.entity.ContactAddressPhoneEntity
import uk.gov.justice.digital.hmpps.personalrelationships.entity.ContactEmailEntity
import uk.gov.justice.digital.hmpps.personalrelationships.entity.ContactEntity
import uk.gov.justice.digital.hmpps.personalrelationships.entity.ContactIdentityDetailsEntity
import uk.gov.justice.digital.hmpps.personalrelationships.entity.ContactPhoneDetailsEntity
import uk.gov.justice.digital.hmpps.personalrelationships.entity.EmploymentEntity
import uk.gov.justice.digital.hmpps.personalrelationships.entity.PrisonerContactEntity

data class TransactionalContactCreationResult(
  val contact: ContactEntity,
  val relationship: PrisonerContactEntity?,
  val addresses: List<ContactAddressDetailsEntity>,
  val addressPhones: List<ContactAddressPhoneEntity>,
  val phones: List<ContactPhoneDetailsEntity>,
  val emails: List<ContactEmailEntity>,
  val identities: List<ContactIdentityDetailsEntity>,
  val employments: List<EmploymentEntity>,
)
