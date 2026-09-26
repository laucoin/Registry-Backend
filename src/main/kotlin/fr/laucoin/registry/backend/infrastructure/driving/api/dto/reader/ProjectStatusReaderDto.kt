package fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader

import java.time.ZonedDateTime

data class ProjectStatusReaderDto(
	var registered: ParticipantStatusReaderDto,
	var guests: Long,
	var lastRefresh: ZonedDateTime,
) {
	data class ParticipantStatusReaderDto(
		var presentMinors: Long,
		var presentMajors: Long,
		var absentMinors: Long,
		var absentMajors: Long,
	)
}
