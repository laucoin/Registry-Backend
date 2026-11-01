package fr.laucoin.registry.backend.domain.model

import java.time.ZonedDateTime

data class ProjectStatusModel(
	val registered: ParticipantStatusModel,
	val guests: Long,
	val lastRefresh: ZonedDateTime? = null
) {
	data class ParticipantStatusModel(
		var presentMinors: Long,
		var presentMajors: Long,
		var absentMinors: Long,
		var absentMajors: Long,
	)
}
