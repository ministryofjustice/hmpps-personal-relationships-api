package uk.gov.justice.digital.hmpps.hmppscontactsapi.resource

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.openapitools.jackson.nullable.JsonNullable
import uk.gov.justice.digital.hmpps.hmppscontactsapi.facade.ContactFacade
import uk.gov.justice.digital.hmpps.hmppscontactsapi.facade.PrisonerContactRestrictionsFacade
import uk.gov.justice.digital.hmpps.hmppscontactsapi.helpers.aUser
import uk.gov.justice.digital.hmpps.hmppscontactsapi.helpers.createPrisonerContactRelationshipDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.helpers.createPrisonerContactRestrictionDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.AddContactRelationshipRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.ContactRelationship
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.PatchRelationshipRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.restrictions.CreatePrisonerContactRestrictionRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.request.restrictions.UpdatePrisonerContactRestrictionRequest
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.PrisonerContactRelationshipDetails
import uk.gov.justice.digital.hmpps.hmppscontactsapi.model.response.PrisonerContactRestrictionsResponse
import uk.gov.justice.digital.hmpps.hmppscontactsapi.service.PrisonerContactRelationshipService
import java.time.LocalDate

class PrisonerContactControllerTest {

  private val prisonerContactRelationshipService: PrisonerContactRelationshipService = mock()
  private val contactFacade: ContactFacade = mock()
  private val prisonerContactRestrictionsFacade: PrisonerContactRestrictionsFacade = mock()

  private val controller = PrisonerContactController(prisonerContactRelationshipService, contactFacade, prisonerContactRestrictionsFacade)

  @Nested
  inner class GetPrisonerContactRelationship {
    @Test
    fun `should return prisoner contact relationship when found`() {
      val prisonerContactId = 1L
      val mockPrisonerContactRelationship = getMockPrisonerContactRelationship()
      whenever(prisonerContactRelationshipService.getById(prisonerContactId)).thenReturn(mockPrisonerContactRelationship)

      val response: PrisonerContactRelationshipDetails =
        controller.getPrisonerContactById(prisonerContactId)

      assertThat(response).isEqualTo(mockPrisonerContactRelationship)
      verify(prisonerContactRelationshipService).getById(prisonerContactId)
    }

    @Test
    fun `should throw exception when relationship is not found`() {
      val prisonContactId = 999L
      whenever(prisonerContactRelationshipService.getById(prisonContactId)).thenThrow(EntityNotFoundException::class.java)

      assertThrows<EntityNotFoundException> {
        controller.getPrisonerContactById(prisonContactId)
      }
      verify(prisonerContactRelationshipService, times(1)).getById(prisonContactId)
    }

    private fun getMockPrisonerContactRelationship() = PrisonerContactRelationshipDetails(
      prisonerContactId = 1,
      contactId = 2,
      prisonerNumber = "A1234BC",
      relationshipTypeCode = "S",
      relationshipTypeDescription = "Social",
      relationshipToPrisonerCode = "FRI",
      relationshipToPrisonerDescription = "Friend",
      isNextOfKin = false,
      isEmergencyContact = true,
      isRelationshipActive = true,
      isApprovedVisitor = true,
      comments = "Close family friend",
    )
  }

  @Nested
  inner class PatchContactRelationship {
    private val prisonerContactId = 2L
    private val request = patchContactRelationshipRequest()
    private val user = aUser("updated")

    @Test
    fun `should update a contact relationship successfully`() {
      doNothing().whenever(contactFacade).patchRelationship(prisonerContactId, request, user)

      controller.patchContactRelationship(prisonerContactId, request, user)

      Mockito.verify(contactFacade).patchRelationship(prisonerContactId, request, user)
    }

    @Test
    fun `should propagate exceptions patching a contact relationship`() {
      whenever(contactFacade.patchRelationship(prisonerContactId, request, user)).thenThrow(RuntimeException("Bang!"))

      assertThrows<RuntimeException>("Bang!") {
        controller.patchContactRelationship(prisonerContactId, request, user)
      }
    }

    private fun patchContactRelationshipRequest() = PatchRelationshipRequest(
      relationshipToPrisonerCode = JsonNullable.of("ENG"),
    )
  }

  @Nested
  inner class AddContactRelationship {
    private val contactId = 123456L
    private val relationship = ContactRelationship(
      prisonerNumber = "A1234BC",
      relationshipTypeCode = "S",
      relationshipToPrisonerCode = "MOT",
      isNextOfKin = true,
      isEmergencyContact = false,
      isApprovedVisitor = false,
      comments = "Foo",
    )
    private val user = aUser("created")
    private val request = AddContactRelationshipRequest(contactId, relationship)

    @Test
    fun `should create a contact relationship successfully`() {
      val created = createPrisonerContactRelationshipDetails()
      whenever(contactFacade.addContactRelationship(request, user)).thenReturn(created)

      val result = controller.addContactRelationship(request, user)

      assertThat(result).isEqualTo(created)
      Mockito.verify(contactFacade).addContactRelationship(request, user)
    }

    @Test
    fun `should propagate exceptions getting a contact`() {
      whenever(contactFacade.addContactRelationship(request, user)).thenThrow(RuntimeException("Bang!"))

      assertThrows<RuntimeException>("Bang!") {
        controller.addContactRelationship(request, user)
      }
    }
  }

  @Nested
  inner class GetPrisonerContactRelationshipRestrictions {
    private val prisonerContactId = 123456L

    @Test
    fun `should get a prisoner contacts restrictions successfully`() {
      val expected = PrisonerContactRestrictionsResponse(emptyList(), emptyList())
      whenever(prisonerContactRestrictionsFacade.getPrisonerContactRestrictions(prisonerContactId)).thenReturn(expected)

      val result = controller.getPrisonerContactRestrictionsByPrisonerContactId(prisonerContactId)

      assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `should propagate exceptions getting a contact`() {
      whenever(prisonerContactRestrictionsFacade.getPrisonerContactRestrictions(prisonerContactId)).thenThrow(RuntimeException("Bang!"))

      assertThrows<RuntimeException>("Bang!") {
        controller.getPrisonerContactRestrictionsByPrisonerContactId(prisonerContactId)
      }
    }
  }

  @Nested
  inner class CreatePrisonerContactRestrictions {
    private val user = aUser("restrictions")
    private val request = CreatePrisonerContactRestrictionRequest(
      restrictionType = "BAN",
      startDate = LocalDate.of(2020, 1, 1),
      expiryDate = LocalDate.of(2022, 2, 2),
      comments = "Some comments",
    )

    @Test
    fun `should create restrictions`() {
      val expected = createPrisonerContactRestrictionDetails()
      whenever(prisonerContactRestrictionsFacade.createPrisonerContactRestriction(9, request, user)).thenReturn(expected)

      val response = controller.createPrisonerContactRestriction(9, request, user)

      assertThat(response.body).isEqualTo(expected)
      verify(prisonerContactRestrictionsFacade).createPrisonerContactRestriction(9, request, user)
    }

    @Test
    fun `should propagate exceptions creating a restriction`() {
      val expected = RuntimeException("Bang!")
      whenever(prisonerContactRestrictionsFacade.createPrisonerContactRestriction(9, request, user)).thenThrow(expected)

      val result = assertThrows<RuntimeException> {
        controller.createPrisonerContactRestriction(9, request, user)
      }
      assertThat(result).isEqualTo(expected)
    }
  }

  @Nested
  inner class UpdatePrisonerContactRestrictions {
    private val prisonerContactRestrictionId = 564L
    private val user = aUser("restrictions")
    private val request = UpdatePrisonerContactRestrictionRequest(
      restrictionType = "BAN",
      startDate = LocalDate.of(2020, 1, 1),
      expiryDate = LocalDate.of(2022, 2, 2),
      comments = "Some comments",
    )

    @Test
    fun `should update restrictions`() {
      val expected = createPrisonerContactRestrictionDetails()
      whenever(prisonerContactRestrictionsFacade.updatePrisonerContactRestriction(9, prisonerContactRestrictionId, request, user)).thenReturn(expected)

      val response = controller.updatePrisonerContactRestriction(9, prisonerContactRestrictionId, request, user)

      assertThat(response).isEqualTo(expected)
      verify(prisonerContactRestrictionsFacade).updatePrisonerContactRestriction(9, prisonerContactRestrictionId, request, user)
    }

    @Test
    fun `should propagate exceptions updating a restriction`() {
      val expected = RuntimeException("Bang!")
      whenever(prisonerContactRestrictionsFacade.updatePrisonerContactRestriction(9, prisonerContactRestrictionId, request, user)).thenThrow(expected)

      val result = assertThrows<RuntimeException> {
        controller.updatePrisonerContactRestriction(9, prisonerContactRestrictionId, request, user)
      }
      assertThat(result).isEqualTo(expected)
    }
  }
}
