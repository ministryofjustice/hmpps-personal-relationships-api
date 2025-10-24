
data class ResetPrisonerContactRequestUpdated(

  val prisonerContact: SyncPrisonerRelationship,

  val approvedBy: String? = null,

  val approvedTime: LocalDateTime? = null,

)
