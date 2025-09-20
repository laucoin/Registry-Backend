package fr.laucoin.registry.backend.domain.model

import java.time.ZonedDateTime
import java.time.ZonedDateTime.now

data class VehicleStatusModel(
	var present: Long,
	var absent: Long,
	val lastRefresh: ZonedDateTime = now()
)
