package uk.gov.justice.digital.hmpps.hmppscontactsapi.service.events

import org.assertj.core.api.Assertions.within
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppscontactsapi.config.FeatureSwitches
import uk.gov.justice.digital.hmpps.hmppscontactsapi.config.User
import uk.gov.justice.digital.hmpps.hmppscontactsapi.helpers.aUser
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.telemetry.StandardTelemetryEvent
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.telemetry.TelemetryService
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class OutboundEventsServiceTest {
  private val eventsPublisher: OutboundEventsPublisher = mock()
  private val featureSwitches: FeatureSwitches = mock()
  private val telemetryService: TelemetryService = mock()
  private val outboundEventsService = OutboundEventsService(eventsPublisher, featureSwitches, telemetryService)
  private val eventCaptor = argumentCaptor<OutboundHMPPSDomainEvent>()
  private val telemetryCaptor = argumentCaptor<StandardTelemetryEvent>()

  @Test
  fun `contact created event with id 1 is sent to the events publisher`() {
    featureSwitches.stub { on { isEnabled(OutboundEvent.CONTACT_CREATED) } doReturn true }
    outboundEventsService.send(OutboundEvent.CONTACT_CREATED, 1L, 1L, user = aUser("foo"))
    verify(
      expectedEventType = "contacts-api.contact.created",
      expectedAdditionalInformation = ContactInfo(contactId = 1L, source = Source.DPS, "foo", null),
      expectedPersonReference = PersonReference(dpsContactId = 1L),
      expectedDescription = "A contact has been created",
    )
  }

  @Test
  fun `contact updated event with id 1 is sent to the events publisher`() {
    featureSwitches.stub { on { isEnabled(OutboundEvent.CONTACT_UPDATED) } doReturn true }
    outboundEventsService.send(OutboundEvent.CONTACT_UPDATED, 1L, 1L, user = aUser("foo"))
    verify(
      expectedEventType = "contacts-api.contact.updated",
      expectedAdditionalInformation = ContactInfo(contactId = 1L, source = Source.DPS, "foo", null),
      expectedPersonReference = PersonReference(dpsContactId = 1L),
      expectedDescription = "A contact has been updated",
    )
  }

  @Test
  fun `contact deleted event with id 1 is sent to the events publisher`() {
    featureSwitches.stub { on { isEnabled(OutboundEvent.CONTACT_DELETED) } doReturn true }
    outboundEventsService.send(OutboundEvent.CONTACT_DELETED, 1L, 1L, user = aUser("foo"))
    verify(
      expectedEventType = "contacts-api.contact.deleted",
      expectedAdditionalInformation = ContactInfo(contactId = 1L, source = Source.DPS, "foo", null),
      expectedPersonReference = PersonReference(dpsContactId = 1L),
      expectedDescription = "A contact has been deleted",
    )
  }

  @Test
  fun `contact address created event with id 1 is sent to the events publisher`() {
    featureSwitches.stub { on { isEnabled(OutboundEvent.CONTACT_ADDRESS_CREATED) } doReturn true }
    outboundEventsService.send(OutboundEvent.CONTACT_ADDRESS_CREATED, 1L, 1L, user = aUser("address"))
    verify(
      expectedEventType = "contacts-api.contact-address.created",
      expectedAdditionalInformation = ContactAddressInfo(
        contactAddressId = 1L,
        source = Source.DPS,
        username = "address",
        activeCaseLoadId = null,
      ),
      expectedPersonReference = PersonReference(dpsContactId = 1L),
      expectedDescription = "A contact address has been created",
    )
  }

  @Test
  fun `contact address updated event with id 1 is sent to the events publisher`() {
    featureSwitches.stub { on { isEnabled(OutboundEvent.CONTACT_ADDRESS_UPDATED) } doReturn true }
    outboundEventsService.send(OutboundEvent.CONTACT_ADDRESS_UPDATED, 1L, 1L, user = aUser("address"))
    verify(
      expectedEventType = "contacts-api.contact-address.updated",
      expectedAdditionalInformation = ContactAddressInfo(
        contactAddressId = 1L,
        source = Source.DPS,
        username = "address",
        activeCaseLoadId = null,
      ),
      expectedPersonReference = PersonReference(dpsContactId = 1L),
      expectedDescription = "A contact address has been updated",
    )
  }

  @Test
  fun `contact address deleted event with id 1 is sent to the events publisher`() {
    featureSwitches.stub { on { isEnabled(OutboundEvent.CONTACT_ADDRESS_DELETED) } doReturn true }
    outboundEventsService.send(OutboundEvent.CONTACT_ADDRESS_DELETED, 1L, 1L, user = aUser("address"))
    verify(
      expectedEventType = "contacts-api.contact-address.deleted",
      expectedAdditionalInformation = ContactAddressInfo(
        contactAddressId = 1L,
        source = Source.DPS,
        username = "address",
        activeCaseLoadId = null,
      ),
      expectedPersonReference = PersonReference(dpsContactId = 1L),
      expectedDescription = "A contact address has been deleted",
    )
  }

  @Test
  fun `contact email created event with id 1 is sent to the events publisher`() {
    featureSwitches.stub { on { isEnabled(OutboundEvent.CONTACT_EMAIL_CREATED) } doReturn true }
    outboundEventsService.send(OutboundEvent.CONTACT_EMAIL_CREATED, 1L, 1L, user = aUser("email"))
    verify(
      expectedEventType = "contacts-api.contact-email.created",
      expectedAdditionalInformation = ContactEmailInfo(contactEmailId = 1L, source = Source.DPS, username = "email", activeCaseLoadId = null),
      expectedPersonReference = PersonReference(dpsContactId = 1L),
      expectedDescription = "A contact email address has been created",
    )
  }

  @Test
  fun `contact email updated event with id 1 is sent to the events publisher`() {
    featureSwitches.stub { on { isEnabled(OutboundEvent.CONTACT_EMAIL_UPDATED) } doReturn true }
    outboundEventsService.send(OutboundEvent.CONTACT_EMAIL_UPDATED, 1L, 1L, user = aUser("email"))
    verify(
      expectedEventType = "contacts-api.contact-email.updated",
      expectedAdditionalInformation = ContactEmailInfo(contactEmailId = 1, source = Source.DPS, username = "email", activeCaseLoadId = null),
      expectedPersonReference = PersonReference(dpsContactId = 1L),
      expectedDescription = "A contact email address has been updated",
    )
  }

  @Test
  fun `contact email deleted event with id 1 is sent to the events publisher`() {
    featureSwitches.stub { on { isEnabled(OutboundEvent.CONTACT_EMAIL_DELETED) } doReturn true }
    outboundEventsService.send(OutboundEvent.CONTACT_EMAIL_DELETED, 1L, 1L, user = aUser("email"))
    verify(
      expectedEventType = "contacts-api.contact-email.deleted",
      expectedAdditionalInformation = ContactEmailInfo(contactEmailId = 1, source = Source.DPS, username = "email", activeCaseLoadId = null),
      expectedPersonReference = PersonReference(dpsContactId = 1L),
      expectedDescription = "A contact email address has been deleted",
    )
  }

  @Test
  fun `contact phone created event with id 1 is sent to the events publisher`() {
    featureSwitches.stub { on { isEnabled(OutboundEvent.CONTACT_PHONE_CREATED) } doReturn true }
    outboundEventsService.send(OutboundEvent.CONTACT_PHONE_CREATED, 1L, 1L, user = aUser("phone"))
    verify(
      expectedEventType = "contacts-api.contact-phone.created",
      expectedAdditionalInformation = ContactPhoneInfo(contactPhoneId = 1L, source = Source.DPS, username = "phone", activeCaseLoadId = null),
      expectedPersonReference = PersonReference(dpsContactId = 1L),
      expectedDescription = "A contact phone number has been created",
    )
  }

  @Test
  fun `contact phone updated event with id 1 is sent to the events publisher`() {
    featureSwitches.stub { on { isEnabled(OutboundEvent.CONTACT_PHONE_UPDATED) } doReturn true }
    outboundEventsService.send(OutboundEvent.CONTACT_PHONE_UPDATED, 1L, 1L, user = aUser("phone"))
    verify(
      expectedEventType = "contacts-api.contact-phone.updated",
      expectedAdditionalInformation = ContactPhoneInfo(contactPhoneId = 1, source = Source.DPS, username = "phone", activeCaseLoadId = null),
      expectedPersonReference = PersonReference(dpsContactId = 1L),
      expectedDescription = "A contact phone number has been updated",
    )
  }

  @Test
  fun `contact phone deleted event with id 1 is sent to the events publisher`() {
    featureSwitches.stub { on { isEnabled(OutboundEvent.CONTACT_PHONE_DELETED) } doReturn true }
    outboundEventsService.send(OutboundEvent.CONTACT_PHONE_DELETED, 1L, 1L, user = aUser("phone"))
    verify(
      expectedEventType = "contacts-api.contact-phone.deleted",
      expectedAdditionalInformation = ContactPhoneInfo(contactPhoneId = 1, source = Source.DPS, username = "phone", activeCaseLoadId = null),
      expectedPersonReference = PersonReference(dpsContactId = 1L),
      expectedDescription = "A contact phone number has been deleted",
    )
  }

  @Test
  fun `contact address phone created event with id 1 is sent to the events publisher`() {
    featureSwitches.stub { on { isEnabled(OutboundEvent.CONTACT_ADDRESS_PHONE_CREATED) } doReturn true }
    outboundEventsService.send(
      OutboundEvent.CONTACT_ADDRESS_PHONE_CREATED,
      1L,
      1L,
      secondIdentifier = 99L,
      user = aUser("phone"),
    )
    verify(
      expectedEventType = "contacts-api.contact-address-phone.created",
      expectedAdditionalInformation = ContactAddressPhoneInfo(
        contactAddressPhoneId = 1L,
        contactAddressId = 99L,
        source = Source.DPS,
        username = "phone",
        activeCaseLoadId = null,
      ),
      expectedPersonReference = PersonReference(dpsContactId = 1L),
      expectedDescription = "A contact address phone number has been created",
    )
  }

  @Test
  fun `contact address phone updated event with id 1 is sent to the events publisher`() {
    featureSwitches.stub { on { isEnabled(OutboundEvent.CONTACT_ADDRESS_PHONE_UPDATED) } doReturn true }
    outboundEventsService.send(
      OutboundEvent.CONTACT_ADDRESS_PHONE_UPDATED,
      1L,
      1L,
      secondIdentifier = 99L,
      user = aUser("phone"),
    )
    verify(
      expectedEventType = "contacts-api.contact-address-phone.updated",
      expectedAdditionalInformation = ContactAddressPhoneInfo(
        contactAddressPhoneId = 1,
        contactAddressId = 99L,
        source = Source.DPS,
        username = "phone",
        activeCaseLoadId = null,
      ),
      expectedPersonReference = PersonReference(dpsContactId = 1L),
      expectedDescription = "A contact address phone number has been updated",
    )
  }

  @Test
  fun `contact address phone deleted event with id 1 is sent to the events publisher`() {
    featureSwitches.stub { on { isEnabled(OutboundEvent.CONTACT_ADDRESS_PHONE_DELETED) } doReturn true }
    outboundEventsService.send(
      OutboundEvent.CONTACT_ADDRESS_PHONE_DELETED,
      1L,
      1L,
      secondIdentifier = 99L,
      user = aUser("phone"),
    )
    verify(
      expectedEventType = "contacts-api.contact-address-phone.deleted",
      expectedAdditionalInformation = ContactAddressPhoneInfo(
        contactAddressPhoneId = 1,
        contactAddressId = 99L,
        source = Source.DPS,
        username = "phone",
        activeCaseLoadId = null,
      ),
      expectedPersonReference = PersonReference(dpsContactId = 1L),
      expectedDescription = "A contact address phone number has been deleted",
    )
  }

  @Test
  fun `contact identity created event with id 1 is sent to the events publisher`() {
    featureSwitches.stub { on { isEnabled(OutboundEvent.CONTACT_IDENTITY_CREATED) } doReturn true }
    outboundEventsService.send(OutboundEvent.CONTACT_IDENTITY_CREATED, 1L, 1L, user = aUser("id_user"))
    verify(
      expectedEventType = "contacts-api.contact-identity.created",
      expectedAdditionalInformation = ContactIdentityInfo(
        contactIdentityId = 1L,
        source = Source.DPS,
        username = "id_user",
        activeCaseLoadId = null,
      ),
      expectedPersonReference = PersonReference(dpsContactId = 1L),
      expectedDescription = "A contact proof of identity has been created",
    )
  }

  @Test
  fun `contact identity updated event with id 1 is sent to the events publisher`() {
    featureSwitches.stub { on { isEnabled(OutboundEvent.CONTACT_IDENTITY_UPDATED) } doReturn true }
    outboundEventsService.send(OutboundEvent.CONTACT_IDENTITY_UPDATED, 1L, 1L, user = aUser("id_user"))
    verify(
      expectedEventType = "contacts-api.contact-identity.updated",
      expectedAdditionalInformation = ContactIdentityInfo(
        contactIdentityId = 1,
        source = Source.DPS,
        username = "id_user",
        activeCaseLoadId = null,
      ),
      expectedPersonReference = PersonReference(dpsContactId = 1L),
      expectedDescription = "A contact proof of identity has been updated",
    )
  }

  @Test
  fun `contact identity deleted event with id 1 is sent to the events publisher`() {
    featureSwitches.stub { on { isEnabled(OutboundEvent.CONTACT_IDENTITY_DELETED) } doReturn true }
    outboundEventsService.send(OutboundEvent.CONTACT_IDENTITY_DELETED, 1L, 1L, user = aUser("id_user"))
    verify(
      expectedEventType = "contacts-api.contact-identity.deleted",
      expectedAdditionalInformation = ContactIdentityInfo(
        contactIdentityId = 1,
        source = Source.DPS,
        username = "id_user",
        activeCaseLoadId = null,
      ),
      expectedPersonReference = PersonReference(dpsContactId = 1L),
      expectedDescription = "A contact proof of identity has been deleted",
    )
  }

  @Test
  fun `contact restriction created event with id 1 is sent to the events publisher`() {
    featureSwitches.stub { on { isEnabled(OutboundEvent.CONTACT_RESTRICTION_CREATED) } doReturn true }
    outboundEventsService.send(OutboundEvent.CONTACT_RESTRICTION_CREATED, 1L, 1L, user = aUser("restriction_user"))
    verify(
      expectedEventType = "contacts-api.contact-restriction.created",
      expectedAdditionalInformation = ContactRestrictionInfo(
        contactRestrictionId = 1L,
        source = Source.DPS,
        username = "restriction_user",
        activeCaseLoadId = null,
      ),
      expectedPersonReference = PersonReference(dpsContactId = 1L),
      expectedDescription = "A contact restriction has been created",
    )
  }

  @Test
  fun `contact restriction updated event with id 1 is sent to the events publisher`() {
    featureSwitches.stub { on { isEnabled(OutboundEvent.CONTACT_RESTRICTION_UPDATED) } doReturn true }
    outboundEventsService.send(OutboundEvent.CONTACT_RESTRICTION_UPDATED, 1L, 1L, user = aUser("restriction_user"))
    verify(
      expectedEventType = "contacts-api.contact-restriction.updated",
      expectedAdditionalInformation = ContactRestrictionInfo(
        contactRestrictionId = 1L,
        source = Source.DPS,
        username = "restriction_user",
        activeCaseLoadId = null,
      ),
      expectedPersonReference = PersonReference(dpsContactId = 1L),
      expectedDescription = "A contact restriction has been updated",
    )
  }

  @Test
  fun `contact restriction deleted event with id 1 is sent to the events publisher`() {
    featureSwitches.stub { on { isEnabled(OutboundEvent.CONTACT_RESTRICTION_DELETED) } doReturn true }
    outboundEventsService.send(OutboundEvent.CONTACT_RESTRICTION_DELETED, 1L, 1L, user = aUser("restriction_user"))
    verify(
      expectedEventType = "contacts-api.contact-restriction.deleted",
      expectedAdditionalInformation = ContactRestrictionInfo(
        contactRestrictionId = 1L,
        source = Source.DPS,
        username = "restriction_user",
        activeCaseLoadId = null,
      ),
      expectedPersonReference = PersonReference(dpsContactId = 1L),
      expectedDescription = "A contact restriction has been deleted",
    )
  }

  @Test
  fun `prisoner contact created event with id 1 is sent to the events publisher`() {
    featureSwitches.stub { on { isEnabled(OutboundEvent.PRISONER_CONTACT_CREATED) } doReturn true }
    outboundEventsService.send(OutboundEvent.PRISONER_CONTACT_CREATED, 1L, 1L, "A1234AA", user = aUser("foo"))
    verify(
      expectedEventType = "contacts-api.prisoner-contact.created",
      expectedAdditionalInformation = PrisonerContactInfo(
        prisonerContactId = 1L,
        source = Source.DPS,
        username = "foo",
        activeCaseLoadId = null,
      ),
      expectedPersonReference = PersonReference(dpsContactId = 1L, nomsNumber = "A1234AA"),
      expectedDescription = "A prisoner contact has been created",
    )
  }

  @Test
  fun `prisoner contact updated event with id 1 is sent to the events publisher`() {
    featureSwitches.stub { on { isEnabled(OutboundEvent.PRISONER_CONTACT_UPDATED) } doReturn true }
    outboundEventsService.send(OutboundEvent.PRISONER_CONTACT_UPDATED, 1L, 1L, "A1234AA", user = aUser("foo"))
    verify(
      expectedEventType = "contacts-api.prisoner-contact.updated",
      expectedAdditionalInformation = PrisonerContactInfo(
        prisonerContactId = 1L,
        source = Source.DPS,
        username = "foo",
        activeCaseLoadId = null,
      ),
      expectedPersonReference = PersonReference(dpsContactId = 1L, nomsNumber = "A1234AA"),
      expectedDescription = "A prisoner contact has been updated",
    )
  }

  @Test
  fun `prisoner contact deleted event with id 1 is sent to the events publisher`() {
    featureSwitches.stub { on { isEnabled(OutboundEvent.PRISONER_CONTACT_DELETED) } doReturn true }
    outboundEventsService.send(OutboundEvent.PRISONER_CONTACT_DELETED, 1L, 1L, "A1234AA", user = aUser("foo"))
    verify(
      expectedEventType = "contacts-api.prisoner-contact.deleted",
      expectedAdditionalInformation = PrisonerContactInfo(
        prisonerContactId = 1L,
        source = Source.DPS,
        username = "foo",
        activeCaseLoadId = null,
      ),
      expectedPersonReference = PersonReference(dpsContactId = 1L, nomsNumber = "A1234AA"),
      expectedDescription = "A prisoner contact has been deleted",
    )
  }

  @Test
  fun `prisoner contact restriction created event with id 1 is sent to the events publisher`() {
    featureSwitches.stub { on { isEnabled(OutboundEvent.PRISONER_CONTACT_RESTRICTION_CREATED) } doReturn true }
    outboundEventsService.send(
      OutboundEvent.PRISONER_CONTACT_RESTRICTION_CREATED,
      1L,
      1L,
      "A1234AA",
      user = aUser("restriction_user", "CLI"),
    )
    verify(
      expectedEventType = "contacts-api.prisoner-contact-restriction.created",
      expectedAdditionalInformation = PrisonerContactRestrictionInfo(
        prisonerContactRestrictionId = 1L,
        source = Source.DPS,
        username = "restriction_user",
        activeCaseLoadId = "CLI",
      ),
      expectedPersonReference = PersonReference(dpsContactId = 1L, nomsNumber = "A1234AA"),
      expectedDescription = "A prisoner contact restriction has been created",
    )
  }

  @Test
  fun `prisoner contact restriction updated event with id 1 is sent to the events publisher`() {
    featureSwitches.stub { on { isEnabled(OutboundEvent.PRISONER_CONTACT_RESTRICTION_UPDATED) } doReturn true }
    outboundEventsService.send(
      OutboundEvent.PRISONER_CONTACT_RESTRICTION_UPDATED,
      1L,
      1L,
      "A1234AA",
      user = aUser("restriction_user"),
    )
    verify(
      expectedEventType = "contacts-api.prisoner-contact-restriction.updated",
      expectedAdditionalInformation = PrisonerContactRestrictionInfo(
        prisonerContactRestrictionId = 1L,
        source = Source.DPS,
        username = "restriction_user",
        activeCaseLoadId = null,
      ),
      expectedPersonReference = PersonReference(dpsContactId = 1L, nomsNumber = "A1234AA"),
      expectedDescription = "A prisoner contact restriction has been updated",
    )
  }

  @Test
  fun `prisoner contact resrtiction deleted event with id 1 is sent to the events publisher`() {
    featureSwitches.stub { on { isEnabled(OutboundEvent.PRISONER_CONTACT_RESTRICTION_DELETED) } doReturn true }
    outboundEventsService.send(
      OutboundEvent.PRISONER_CONTACT_RESTRICTION_DELETED,
      1L,
      1L,
      "A1234AA",
      user = aUser("restriction_user"),
    )
    verify(
      expectedEventType = "contacts-api.prisoner-contact-restriction.deleted",
      expectedAdditionalInformation = PrisonerContactRestrictionInfo(
        prisonerContactRestrictionId = 1L,
        source = Source.DPS,
        username = "restriction_user",
        activeCaseLoadId = null,
      ),
      expectedPersonReference = PersonReference(dpsContactId = 1L, nomsNumber = "A1234AA"),
      expectedDescription = "A prisoner contact restriction has been deleted",
    )
  }

  @Test
  fun `events are not published for any outbound event when not enabled`() {
    featureSwitches.stub { on { isEnabled(any<OutboundEvent>(), any()) } doReturn false }
    OutboundEvent.entries.forEach { outboundEventsService.send(it, 1L, 1L, user = User.SYS_USER) }
    verifyNoInteractions(eventsPublisher)
  }

  @ParameterizedTest
  @EnumSource(
    value = OutboundEvent::class,
    mode = EnumSource.Mode.EXCLUDE,
    names = ["PRISONER_RESTRICTIONS_CHANGED"],
  )
  fun `should trap exception sending event`(event: OutboundEvent) {
    featureSwitches.stub { on { isEnabled(event) } doReturn true }
    whenever(eventsPublisher.send(any())).thenThrow(RuntimeException("Boom!"))

    outboundEventsService.send(event, 1L, 1L, user = User.SYS_USER)

    verify(eventsPublisher).send(any())
  }

  @Test
  fun `should throw error if send called with PRISONER_RESTRICTION_CHANGED`() {
    featureSwitches.stub { on { isEnabled(OutboundEvent.PRISONER_RESTRICTIONS_CHANGED) } doReturn true }
    val exception = org.junit.jupiter.api.assertThrows<IllegalStateException> {
      outboundEventsService.send(OutboundEvent.PRISONER_RESTRICTIONS_CHANGED, 1L, 1L, user = User.SYS_USER)
    }
    assertThat(exception.message).contains("sendPrisonerRestrictionsChanged should not be called from this context")
    verifyNoInteractions(eventsPublisher)
  }

  @Test
  fun `sendPrisonerRestrictionsChanged sends event with correct info`() {
    featureSwitches.stub { on { isEnabled(OutboundEvent.PRISONER_RESTRICTIONS_CHANGED) } doReturn true }
    val updatedIds = listOf(101L, 102L)
    val removedIds = listOf(201L)
    val noms = "A1234BC"
    val user = aUser("mergeuser", "MDI")

    outboundEventsService.sendPrisonerRestrictionsChanged(
      updatedRestrictionIds = updatedIds,
      removedRestrictionIds = removedIds,
      noms = noms,
      source = Source.DPS,
      user = user,
    )

    verify(eventsPublisher).send(eventCaptor.capture())
    val event = eventCaptor.firstValue
    assertThat(event.eventType).isEqualTo("personal-relationships-api.prisoner-restrictions.changed")
    assertThat(event.additionalInformation)
      .isInstanceOf(PrisonerRestrictionsChangedInfo::class.java)
    val info = event.additionalInformation as PrisonerRestrictionsChangedInfo
    assertThat(info.addedRestrictionIds).isEqualTo(updatedIds)
    assertThat(info.removedRestrictionIds).isEqualTo(removedIds)
    assertThat(info.username).isEqualTo("mergeuser")
    assertThat(info.activeCaseLoadId).isEqualTo("MDI")
    assertThat(event.personReference?.nomsNumber()).isEqualTo(noms)
    assertThat(event.description).isEqualTo("A prisoner restriction has been changed")
    verify(telemetryService).track(any())
    verifyNoMoreInteractions(eventsPublisher)
  }

  @Test
  fun `sendPrisonerRestrictionsChanged does not send event if feature is off`() {
    featureSwitches.stub { on { isEnabled(OutboundEvent.PRISONER_RESTRICTIONS_CHANGED) } doReturn false }
    outboundEventsService.sendPrisonerRestrictionsChanged(
      updatedRestrictionIds = listOf(1L),
      removedRestrictionIds = listOf(2L),
      noms = "A1234BC",
      source = Source.DPS,
      user = aUser("mergeuser"),
    )
    verifyNoInteractions(eventsPublisher)
    verifyNoInteractions(telemetryService)
  }

  @Test
  fun `sendPrisonerRestrictionsChanged logs error if publisher throws`() {
    featureSwitches.stub { on { isEnabled(OutboundEvent.PRISONER_RESTRICTIONS_CHANGED) } doReturn true }
    whenever(eventsPublisher.send(any())).thenThrow(RuntimeException("fail"))
    outboundEventsService.sendPrisonerRestrictionsChanged(
      updatedRestrictionIds = listOf(1L),
      removedRestrictionIds = listOf(2L),
      noms = "A1234BC",
      source = Source.DPS,
      user = aUser("mergeuser"),
    )
    verify(eventsPublisher).send(any())
    // error is logged, but test does not fail
  }

  private fun verify(
    expectedEventType: String,
    expectedAdditionalInformation: AdditionalInformation,
    expectedPersonReference: PersonReference,
    expectedOccurredAt: LocalDateTime = LocalDateTime.now(),
    expectedDescription: String,
  ) {
    verify(eventsPublisher).send(eventCaptor.capture())

    with(eventCaptor.firstValue) {
      assertThat(eventType).isEqualTo(expectedEventType)
      assertThat(additionalInformation).isEqualTo(expectedAdditionalInformation)
      assertThat(personReference?.dpsContactId()).isEqualTo(expectedPersonReference.dpsContactId())
      assertThat(personReference?.nomsNumber()).isEqualTo(expectedPersonReference.nomsNumber())
      assertThat(occurredAt).isCloseTo(expectedOccurredAt, within(60, ChronoUnit.SECONDS))
      assertThat(description).isEqualTo(expectedDescription)
    }

    verify(telemetryService).track(telemetryCaptor.capture())
    with(telemetryCaptor.firstValue) {
      assertThat(eventType).isEqualTo(expectedEventType)
      assertThat(properties()["prisoner_number"]).isEqualTo(expectedPersonReference.nomsNumber())
      assertThat(properties()["contact_id"]).isEqualTo(expectedPersonReference.dpsContactId())
      assertThat(properties()["version"]).isEqualTo("1")
      assertThat(properties()["description"]).isEqualTo(expectedDescription)
      assertThat(properties()["source"]).isEqualTo(expectedAdditionalInformation.source.toString())
      assertThat(properties()["username"]).isEqualTo(expectedAdditionalInformation.username)
      assertThat(properties()["occurred_at"]).isNotNull()
      if (expectedAdditionalInformation.activeCaseLoadId != null) {
        assertThat(properties()["active_caseload_id"]).isEqualTo(expectedAdditionalInformation.activeCaseLoadId)
      } else {
        assertThat(properties()["active_caseload_id"]).isEqualTo("unknown")
      }
    }

    verifyNoMoreInteractions(eventsPublisher)
  }
}
