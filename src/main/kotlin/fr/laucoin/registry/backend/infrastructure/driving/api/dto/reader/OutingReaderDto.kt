package fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader

import java.time.ZonedDateTime

data class OutingReaderDto(
	val start: ZonedDateTime,
	val end: ZonedDateTime,
	val ongoing: Boolean,
)
