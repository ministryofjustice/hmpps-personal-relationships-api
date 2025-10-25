
data class EnrichedPrisonerContactRequestUpdated(

  val prisonerContact: SyncPrisonerRelationship,

  val approvedBy: String? = null,

  val approvedTime: LocalDateTime? = null,

)
