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
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class OutboundEventsServiceTest {
  private val eventsPublisher: OutboundEventsPublisher = mock()
  private val featureSwitches: FeatureSwitches = mock()
  private val outboundEventsService = OutboundEventsService(eventsPublisher, featureSwitches)
  private val eventCaptor = argumentCaptor<OutboundHMPPSDomainEvent>()

  @Test
  fun `contact created event with id 1 is sent to the events publisher`() {
    featureSwitches.stub { on { isEnabled(OutboundEvent.CONTACT_CREATED) } doReturn true }
    outboundEventsService.send(OutboundEvent.CONTACT_CREATED, 1L, 1L)
    verify(
      expectedEventType = "contacts-api.contact.created",
      expectedAdditionalInformation = ContactInfo(contactId = 1L, source = Source.DPS),
      expectedPersonReference = PersonReference(dpsContactId = 1L),
      expectedDescription = "A contact has been created",
    )
  }

  @Test
  fun `contact updated event with id 1 is sent to the events publisher`() {
    featureSwitches.stub { on { isEnabled(OutboundEvent.CONTACT_UPDATED) } doReturn true }
    outboundEventsService.send(OutboundEvent.CONTACT_UPDATED, 1L, 1L)
    verify(
      expectedEventType = "contacts-api.contact.updated",
      expectedAdditionalInformation = ContactInfo(contactId = 1L, source = Source.DPS),
      expectedPersonReference = PersonReference(dpsContactId = 1L),
      expectedDescription = "A contact has been updated",
    )
  }

  @Test
  fun `contact deleted event with id 1 is sent to the events publisher`() {
    featureSwitches.stub { on { isEnabled(OutboundEvent.CONTACT_DELETED) } doReturn true }
    outboundEventsService.send(OutboundEvent.CONTACT_DELETED, 1L, 1L)
    verify(
      expectedEventType = "contacts-api.contact.deleted",
      expectedAdditionalInformation = ContactInfo(contactId = 1L, source = Source.DPS),
      expectedPersonReference = PersonReference(dpsContactId = 1L),
      expectedDescription = "A contact has been deleted",
    )
  }

  @Test
  fun `contact address created event with id 1 is sent to the events publisher`() {
    featureSwitches.stub { on { isEnabled(OutboundEvent.CONTACT_ADDRESS_CREATED) } doReturn true }
    outboundEventsService.send(OutboundEvent.CONTACT_ADDRESS_CREATED, 1L, 1L)
    verify(
      expectedEventType = "contacts-api.contact-address.created",
      expectedAdditionalInformation = ContactAddressInfo(contactAddressId = 1L, source = Source.DPS),
      expectedPersonReference = PersonReference(dpsContactId = 1L),
      expectedDescription = "A contact address has been created",
    )
  }

  @Test
  fun `contact address updated event with id 1 is sent to the events publisher`() {
    featureSwitches.stub { on { isEnabled(OutboundEvent.CONTACT_ADDRESS_UPDATED) } doReturn true }
    outboundEventsService.send(OutboundEvent.CONTACT_ADDRESS_UPDATED, 1L, 1L)
    verify(
      expectedEventType = "contacts-api.contact-address.updated",
      expectedAdditionalInformation = ContactAddressInfo(contactAddressId = 1L, source = Source.DPS),
      expectedPersonReference = PersonReference(dpsContactId = 1L),
      expectedDescription = "A contact address has been updated",
    )
  }

  @Test
  fun `contact address deleted event with id 1 is sent to the events publisher`() {
    featureSwitches.stub { on { isEnabled(OutboundEvent.CONTACT_ADDRESS_DELETED) } doReturn true }
    outboundEventsService.send(OutboundEvent.CONTACT_ADDRESS_DELETED, 1L, 1L)
    verify(
      expectedEventType = "contacts-api.contact-address.deleted",
      expectedAdditionalInformation = ContactAddressInfo(contactAddressId = 1L, source = Source.DPS),
      expectedPersonReference = PersonReference(dpsContactId = 1L),
      expectedDescription = "A contact address has been deleted",
    )
  }

  @Test
  fun `contact email created event with id 1 is sent to the events publisher`() {
    featureSwitches.stub { on { isEnabled(OutboundEvent.CONTACT_EMAIL_CREATED) } doReturn true }
    outboundEventsService.send(OutboundEvent.CONTACT_EMAIL_CREATED, 1L, 1L)
    verify(
      expectedEventType = "contacts-api.contact-email.created",
      expectedAdditionalInformation = ContactEmailInfo(contactEmailId = 1L, source = Source.DPS),
      expectedPersonReference = PersonReference(dpsContactId = 1L),
      expectedDescription = "A contact email address has been created",
    )
  }

  @Test
  fun `contact email updated event with id 1 is sent to the events publisher`() {
    featureSwitches.stub { on { isEnabled(OutboundEvent.CONTACT_EMAIL_UPDATED) } doReturn true }
    outboundEventsService.send(OutboundEvent.CONTACT_EMAIL_UPDATED, 1L, 1L)
    verify(
      expectedEventType = "contacts-api.contact-email.updated",
      expectedAdditionalInformation = ContactEmailInfo(contactEmailId = 1, source = Source.DPS),
      expectedPersonReference = PersonReference(dpsContactId = 1L),
      expectedDescription = "A contact email address has been updated",
    )
  }

  @Test
  fun `contact email deleted event with id 1 is sent to the events publisher`() {
    featureSwitches.stub { on { isEnabled(OutboundEvent.CONTACT_EMAIL_DELETED) } doReturn true }
    outboundEventsService.send(OutboundEvent.CONTACT_EMAIL_DELETED, 1L, 1L)
    verify(
      expectedEventType = "contacts-api.contact-email.deleted",
      expectedAdditionalInformation = ContactEmailInfo(contactEmailId = 1, source = Source.DPS),
      expectedPersonReference = PersonReference(dpsContactId = 1L),
      expectedDescription = "A contact email address has been deleted",
    )
  }

  @Test
  fun `contact phone created event with id 1 is sent to the events publisher`() {
    featureSwitches.stub { on { isEnabled(OutboundEvent.CONTACT_PHONE_CREATED) } doReturn true }
    outboundEventsService.send(OutboundEvent.CONTACT_PHONE_CREATED, 1L, 1L)
    verify(
      expectedEventType = "contacts-api.contact-phone.created",
      expectedAdditionalInformation = ContactPhoneInfo(contactPhoneId = 1L, source = Source.DPS),
      expectedPersonReference = PersonReference(dpsContactId = 1L),
      expectedDescription = "A contact phone number has been created",
    )
  }

  @Test
  fun `contact phone updated event with id 1 is sent to the events publisher`() {
    featureSwitches.stub { on { isEnabled(OutboundEvent.CONTACT_PHONE_UPDATED) } doReturn true }
    outboundEventsService.send(OutboundEvent.CONTACT_PHONE_UPDATED, 1L, 1L)
    verify(
      expectedEventType = "contacts-api.contact-phone.updated",
      expectedAdditionalInformation = ContactPhoneInfo(contactPhoneId = 1, source = Source.DPS),
      expectedPersonReference = PersonReference(dpsContactId = 1L),
      expectedDescription = "A contact phone number has been updated",
    )
  }

  @Test
  fun `contact phone deleted event with id 1 is sent to the events publisher`() {
    featureSwitches.stub { on { isEnabled(OutboundEvent.CONTACT_PHONE_DELETED) } doReturn true }
    outboundEventsService.send(OutboundEvent.CONTACT_PHONE_DELETED, 1L, 1L)
    verify(
      expectedEventType = "contacts-api.contact-phone.deleted",
      expectedAdditionalInformation = ContactPhoneInfo(contactPhoneId = 1, source = Source.DPS),
      expectedPersonReference = PersonReference(dpsContactId = 1L),
      expectedDescription = "A contact phone number has been deleted",
    )
  }

  @Test
  fun `contact identity created event with id 1 is sent to the events publisher`() {
    featureSwitches.stub { on { isEnabled(OutboundEvent.CONTACT_IDENTITY_CREATED) } doReturn true }
    outboundEventsService.send(OutboundEvent.CONTACT_IDENTITY_CREATED, 1L, 1L)
    verify(
      expectedEventType = "contacts-api.contact-identity.created",
      expectedAdditionalInformation = ContactIdentityInfo(contactIdentityId = 1L, source = Source.DPS),
      expectedPersonReference = PersonReference(dpsContactId = 1L),
      expectedDescription = "A contact proof of identity has been created",
    )
  }

  @Test
  fun `contact identity updated event with id 1 is sent to the events publisher`() {
    featureSwitches.stub { on { isEnabled(OutboundEvent.CONTACT_IDENTITY_UPDATED) } doReturn true }
    outboundEventsService.send(OutboundEvent.CONTACT_IDENTITY_UPDATED, 1L, 1L)
    verify(
      expectedEventType = "contacts-api.contact-identity.updated",
      expectedAdditionalInformation = ContactIdentityInfo(contactIdentityId = 1, source = Source.DPS),
      expectedPersonReference = PersonReference(dpsContactId = 1L),
      expectedDescription = "A contact proof of identity has been updated",
    )
  }

  @Test
  fun `contact identity deleted event with id 1 is sent to the events publisher`() {
    featureSwitches.stub { on { isEnabled(OutboundEvent.CONTACT_IDENTITY_DELETED) } doReturn true }
    outboundEventsService.send(OutboundEvent.CONTACT_IDENTITY_DELETED, 1L, 1L)
    verify(
      expectedEventType = "contacts-api.contact-identity.deleted",
      expectedAdditionalInformation = ContactIdentityInfo(contactIdentityId = 1, source = Source.DPS),
      expectedPersonReference = PersonReference(dpsContactId = 1L),
      expectedDescription = "A contact proof of identity has been deleted",
    )
  }

  @Test
  fun `contact restriction created event with id 1 is sent to the events publisher`() {
    featureSwitches.stub { on { isEnabled(OutboundEvent.CONTACT_RESTRICTION_CREATED) } doReturn true }
    outboundEventsService.send(OutboundEvent.CONTACT_RESTRICTION_CREATED, 1L, 1L)
    verify(
      expectedEventType = "contacts-api.contact-restriction.created",
      expectedAdditionalInformation = ContactRestrictionInfo(contactRestrictionId = 1L, source = Source.DPS),
      expectedPersonReference = PersonReference(dpsContactId = 1L),
      expectedDescription = "A contact restriction has been created",
    )
  }

  @Test
  fun `contact restriction updated event with id 1 is sent to the events publisher`() {
    featureSwitches.stub { on { isEnabled(OutboundEvent.CONTACT_RESTRICTION_UPDATED) } doReturn true }
    outboundEventsService.send(OutboundEvent.CONTACT_RESTRICTION_UPDATED, 1L, 1L)
    verify(
      expectedEventType = "contacts-api.contact-restriction.updated",
      expectedAdditionalInformation = ContactRestrictionInfo(contactRestrictionId = 1L, source = Source.DPS),
      expectedPersonReference = PersonReference(dpsContactId = 1L),
      expectedDescription = "A contact restriction has been updated",
    )
  }

  @Test
  fun `contact restriction deleted event with id 1 is sent to the events publisher`() {
    featureSwitches.stub { on { isEnabled(OutboundEvent.CONTACT_RESTRICTION_DELETED) } doReturn true }
    outboundEventsService.send(OutboundEvent.CONTACT_RESTRICTION_DELETED, 1L, 1L)
    verify(
      expectedEventType = "contacts-api.contact-restriction.deleted",
      expectedAdditionalInformation = ContactRestrictionInfo(contactRestrictionId = 1L, source = Source.DPS),
      expectedPersonReference = PersonReference(dpsContactId = 1L),
      expectedDescription = "A contact restriction has been deleted",
    )
  }

  @Test
  fun `prisoner contact created event with id 1 is sent to the events publisher`() {
    featureSwitches.stub { on { isEnabled(OutboundEvent.PRISONER_CONTACT_CREATED) } doReturn true }
    outboundEventsService.send(OutboundEvent.PRISONER_CONTACT_CREATED, 1L, 1L, "A1234AA")
    verify(
      expectedEventType = "contacts-api.prisoner-contact.created",
      expectedAdditionalInformation = PrisonerContactInfo(prisonerContactId = 1L, source = Source.DPS),
      expectedPersonReference = PersonReference(dpsContactId = 1L, nomsNumber = "A1234AA"),
      expectedDescription = "A prisoner contact has been created",
    )
  }

  @Test
  fun `prisoner contact updated event with id 1 is sent to the events publisher`() {
    featureSwitches.stub { on { isEnabled(OutboundEvent.PRISONER_CONTACT_UPDATED) } doReturn true }
    outboundEventsService.send(OutboundEvent.PRISONER_CONTACT_UPDATED, 1L, 1L, "A1234AA")
    verify(
      expectedEventType = "contacts-api.prisoner-contact.updated",
      expectedAdditionalInformation = PrisonerContactInfo(prisonerContactId = 1L, source = Source.DPS),
      expectedPersonReference = PersonReference(dpsContactId = 1L, nomsNumber = "A1234AA"),
      expectedDescription = "A prisoner contact has been updated",
    )
  }

  @Test
  fun `prisoner contact deleted event with id 1 is sent to the events publisher`() {
    featureSwitches.stub { on { isEnabled(OutboundEvent.PRISONER_CONTACT_DELETED) } doReturn true }
    outboundEventsService.send(OutboundEvent.PRISONER_CONTACT_DELETED, 1L, 1L, "A1234AA")
    verify(
      expectedEventType = "contacts-api.prisoner-contact.deleted",
      expectedAdditionalInformation = PrisonerContactInfo(prisonerContactId = 1L, source = Source.DPS),
      expectedPersonReference = PersonReference(dpsContactId = 1L, nomsNumber = "A1234AA"),
      expectedDescription = "A prisoner contact has been deleted",
    )
  }

  @Test
  fun `prisoner contact restriction created event with id 1 is sent to the events publisher`() {
    featureSwitches.stub { on { isEnabled(OutboundEvent.PRISONER_CONTACT_RESTRICTION_CREATED) } doReturn true }
    outboundEventsService.send(OutboundEvent.PRISONER_CONTACT_RESTRICTION_CREATED, 1L, 1L, "A1234AA")
    verify(
      expectedEventType = "contacts-api.prisoner-contact-restriction.created",
      expectedAdditionalInformation = PrisonerContactRestrictionInfo(prisonerContactRestrictionId = 1L, source = Source.DPS),
      expectedPersonReference = PersonReference(dpsContactId = 1L, nomsNumber = "A1234AA"),
      expectedDescription = "A prisoner contact restriction has been created",
    )
  }

  @Test
  fun `prisoner contact restriction updated event with id 1 is sent to the events publisher`() {
    featureSwitches.stub { on { isEnabled(OutboundEvent.PRISONER_CONTACT_RESTRICTION_UPDATED) } doReturn true }
    outboundEventsService.send(OutboundEvent.PRISONER_CONTACT_RESTRICTION_UPDATED, 1L, 1L, "A1234AA")
    verify(
      expectedEventType = "contacts-api.prisoner-contact-restriction.updated",
      expectedAdditionalInformation = PrisonerContactRestrictionInfo(prisonerContactRestrictionId = 1L, source = Source.DPS),
      expectedPersonReference = PersonReference(dpsContactId = 1L, nomsNumber = "A1234AA"),
      expectedDescription = "A prisoner contact restriction has been updated",
    )
  }

  @Test
  fun `prisoner contact resrtiction deleted event with id 1 is sent to the events publisher`() {
    featureSwitches.stub { on { isEnabled(OutboundEvent.PRISONER_CONTACT_RESTRICTION_DELETED) } doReturn true }
    outboundEventsService.send(OutboundEvent.PRISONER_CONTACT_RESTRICTION_DELETED, 1L, 1L, "A1234AA")
    verify(
      expectedEventType = "contacts-api.prisoner-contact-restriction.deleted",
      expectedAdditionalInformation = PrisonerContactRestrictionInfo(prisonerContactRestrictionId = 1L, source = Source.DPS),
      expectedPersonReference = PersonReference(dpsContactId = 1L, nomsNumber = "A1234AA"),
      expectedDescription = "A prisoner contact restriction has been deleted",
    )
  }

  @Test
  fun `events are not published for any outbound event when not enabled`() {
    featureSwitches.stub { on { isEnabled(any<OutboundEvent>(), any()) } doReturn false }
    OutboundEvent.entries.forEach { outboundEventsService.send(it, 1L, 1L) }
    verifyNoInteractions(eventsPublisher)
  }

  @ParameterizedTest
  @EnumSource(OutboundEvent::class)
  fun `should trap exception sending event`(event: OutboundEvent) {
    featureSwitches.stub { on { isEnabled(event) } doReturn true }
    whenever(eventsPublisher.send(any())).thenThrow(RuntimeException("Boom!"))

    outboundEventsService.send(event, 1L, 1L)

    verify(eventsPublisher).send(any())
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

    verifyNoMoreInteractions(eventsPublisher)
  }
}
