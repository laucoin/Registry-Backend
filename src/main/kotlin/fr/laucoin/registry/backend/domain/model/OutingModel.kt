package fr.laucoin.registry.backend.domain.model

import java.time.ZonedDateTime

data class OutingModel(
	val start: ZonedDateTime,
	val end: ZonedDateTime,
	val ongoing: Boolean = false,
)
