package uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.sync

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

/**
 * These are slimmed-down versions of the models used purely for reconciling
 * contacts and their sub-entities against values in NOMIS. This is used
 * by Syscon to run a no-frills check that the main data items are aligned.
 */

@Schema(description = "Reconciliation data for one contact")
data class SyncContactReconcile(
  @Schema(description = "Unique identifier for a contact", example = "1111")
  val contactId: Long,

  @Schema(description = "Contact first name", example = "Bob")
  val firstName: String,

  @Schema(description = "Contact last name", example = "Smith")
  val lastName: String,

  @Schema(description = "Contact middle names", example = "David", nullable = true)
  val middleNames: String? = null,

  @Schema(description = "Contact data of birth", example = "2001-02-01")
  val dateOfBirth: LocalDate? = null,

  @Schema(description = "Staff indicator", example = "false")
  val staffFlag: Boolean = false,

  val phones: List<ReconcilePhone> = emptyList(),

  val addresses: List<ReconcileAddress> = emptyList(),

  val emails: List<ReconcileEmail> = emptyList(),

  val identities: List<ReconcileIdentity> = emptyList(),

  val restrictions: List<ReconcileRestriction> = emptyList(),

  val relationships: List<ReconcileRelationship> = emptyList(),

  val employments: List<ReconcileEmployment> = emptyList(),
)

@Schema(description = "Contact phone reconciliation")
data class ReconcilePhone(
  @Schema(description = "Unique identifier for the contact phone", example = "1")
  val contactPhoneId: Long,

  @Schema(description = "Type of phone", example = "MOB")
  val phoneType: String,

  @Schema(description = "Phone number", example = "+1234567890")
  val phoneNumber: String,

  @Schema(description = "Extension number", example = "123")
  val extNumber: String?,
)

@Schema(description = "Contact address phone reconciliation")
data class ReconcileAddressPhone(
  @Schema(description = "Unique identifier for the contact address phone", example = "1")
  val contactAddressPhoneId: Long,

  @Schema(description = "Type of phone", example = "MOB")
  val phoneType: String,

  @Schema(description = "Phone number", example = "+1234567890")
  val phoneNumber: String,

  @Schema(description = "Extension number", example = "123")
  val extNumber: String?,
)

@Schema(description = "Contact email reconciliation")
data class ReconcileEmail(
  @Schema(description = "Unique identifier for the contact email", example = "1")
  val contactEmailId: Long,

  @Schema(description = "Email address", example = "test@example.com")
  val emailAddress: String,
)

@Schema(description = "Contact identity reconciliation")
data class ReconcileIdentity(
  @Schema(description = "Unique identifier for the contact identity", example = "1")
  val contactIdentityId: Long,

  @Schema(description = "Type of identity", example = "DL")
  val identityType: String,

  @Schema(description = "Identity ", example = "DL090 0909 909")
  val identityValue: String,

  @Schema(description = "Issuing authority", example = "DVLA")
  val issuingAuthority: String?,
)

@Schema(description = "Contact address reconciliation")
data class ReconcileAddress(
  @Schema(description = "The id of the contact address", example = "123456")
  val contactAddressId: Long = 0,

  @Schema(description = "The coded value for type of address", example = "HOME", nullable = true)
  val addressType: String? = null,

  @Schema(description = "True if this is the primary address otherwise false", example = "true")
  val primaryAddress: Boolean,

  @Schema(description = "Building or house number or name", example = "Mansion House", nullable = true)
  val property: String? = null,

  @Schema(description = "Street or road name", example = "Acacia Avenue", nullable = true)
  val street: String? = null,

  @Schema(description = "Area", example = "Morton Heights", nullable = true)
  val area: String? = null,

  @Schema(description = "Address-specific phone numbers")
  val addressPhones: List<ReconcileAddressPhone> = emptyList(),
)

@Schema(description = "Contact restriction reconciliation")
data class ReconcileRestriction(
  @Schema(description = "The ID of the contact restriction", example = "12345")
  val contactRestrictionId: Long,

  @Schema(description = "Type of restriction", example = "MOBILE")
  val restrictionType: String,

  @Schema(description = "Restriction created date", example = "2024-01-01")
  val startDate: LocalDate? = null,

  @Schema(description = "Restriction end date ", example = "2024-01-01")
  val expiryDate: LocalDate? = null,
)

@Schema(description = "Contact employment reconciliation")
data class ReconcileEmployment(
  @Schema(description = "The ID of the employment", example = "12345")
  val employmentId: Long,

  @Schema(description = "The ID of the organization associated with the employment", example = "12345")
  val organisationId: Long,

  @Schema(description = "If the employment is active", example = "true")
  val active: Boolean,
)

@Schema(description = "Prisoner contact relationship reconciliation")
data class ReconcileRelationship(
  @Schema(description = "The ID of the prisoner contact", example = "12345")
  val prisonerContactId: Long,

  @Schema(description = "The prisoner number", example = "A1234BC")
  val prisonerNumber: String,

  @Schema(description = "Social or official contact", example = "S")
  val contactType: String,

  @Schema(description = "The relationship code from reference data", example = "Friend")
  val relationshipType: String,

  @Schema(description = "Indicates if the prisoner contact is next of kin", example = "true")
  val nextOfKin: Boolean,

  @Schema(description = "Indicates if the prisoner contact is an emergency contact", example = "true")
  val emergencyContact: Boolean,

  @Schema(description = "Indicates if the prisoner contact is active", example = "true")
  val active: Boolean,

  @Schema(description = "Indicates if the prisoner contact is an approved visitor", example = "true")
  val approvedVisitor: Boolean,

  @Schema(description = "The list of restrictions on this relationship")
  val relationshipRestrictions: List<ReconcileRelationshipRestriction> = emptyList(),
)

@Schema(description = "Contact relationship restriction reconciliation")
data class ReconcileRelationshipRestriction(
  @Schema(description = "The ID of the prisoner contact restriction", example = "12345")
  val prisonerContactRestrictionId: Long,

  @Schema(description = "Type of restriction", example = "MOBILE")
  val restrictionType: String,

  @Schema(description = "Restriction created date", example = "2024-01-01")
  val startDate: LocalDate? = null,

  @Schema(description = "Restriction end date ", example = "2024-01-01")
  val expiryDate: LocalDate? = null,
)

// These are used when reconciling from the prisoner's perspective

@Schema(description = "Prisoner relationship reconciliation")
data class SyncPrisonerReconcile(
  val relationships: List<ReconcilePrisonerRelationship>,
)

@Schema(description = "Prisoner single relationship reconciliation")
data class ReconcilePrisonerRelationship(
  @Schema(description = "The ID of the contact", example = "12345")
  val contactId: Long,

  @Schema(description = "The ID of the prisoner contact", example = "12345")
  val prisonerContactId: Long,

  @Schema(description = "Contact first name", example = "Bob")
  val firstName: String? = null,

  @Schema(description = "Contact last name", example = "Smith")
  val lastName: String? = null,

  @Schema(description = "The prisoner number", example = "A1234BC")
  val prisonerNumber: String,

  @Schema(description = "Social or official contact", example = "S")
  val relationshipTypeCode: String,

  @Schema(description = "The relationship code from reference data", example = "FRIEND")
  val relationshipToPrisoner: String,

  @Schema(description = "Indicates if the prisoner contact is next of kin", example = "true")
  val nextOfKin: Boolean,

  @Schema(description = "Indicates if the prisoner contact is an emergency contact", example = "true")
  val emergencyContact: Boolean,

  @Schema(description = "Indicates if the prisoner contact is active", example = "true")
  val active: Boolean,

  @Schema(description = "Indicates if the prisoner contact is an approved visitor", example = "true")
  val approvedVisitor: Boolean,

  @Schema(description = "The list of restrictions on this relationship")
  val restrictions: List<ReconcileRelationshipRestriction> = emptyList(),
)
