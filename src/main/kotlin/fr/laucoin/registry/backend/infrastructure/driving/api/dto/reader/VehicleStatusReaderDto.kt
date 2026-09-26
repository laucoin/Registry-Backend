package fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader

import java.time.ZonedDateTime

data class VehicleStatusReaderDto(
	var present: Long,
	var absent: Long,
	var lastRefresh: ZonedDateTime,
)
