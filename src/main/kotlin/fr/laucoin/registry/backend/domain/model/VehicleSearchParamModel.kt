package fr.laucoin.registry.backend.domain.model

import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum
import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum.Companion.isAvailable
import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum.Companion.isPresent
import java.time.ZonedDateTime

data class VehicleSearchParamModel(
	var isVisible: Boolean? = null,
	var isAvailable: Boolean? = null,
	var isPresent: Boolean? = null,
	var dateTime: ZonedDateTime? = null,
) {
	var query: String? = null

	constructor(
		query: String? = null,
		isVisible: Boolean? = null,
		status: PresenceStatusEnum? = null,
		dateTime: ZonedDateTime?,
	): this(
		isVisible = isVisible,
		isAvailable = status.isAvailable(),
		isPresent = status.isPresent(),
		dateTime = dateTime,
	) {
		this.query = if (query.isNullOrBlank()) null else query
	}
}
