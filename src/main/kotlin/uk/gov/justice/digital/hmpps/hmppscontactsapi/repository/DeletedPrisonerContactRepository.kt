package uk.gov.justice.digital.hmpps.hmppscontactsapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppscontactsapi.entity.DeletedPrisonerContactEntity

@Repository
interface DeletedPrisonerContactRepository : JpaRepository<DeletedPrisonerContactEntity, Long> {
  fun findByPrisonerContactId(prisonerContactId: Long): List<DeletedPrisonerContactEntity>
}
