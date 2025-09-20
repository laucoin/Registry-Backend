package fr.laucoin.registry.backend.domain.model

import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum
import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum.Companion.isAvailable
import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum.Companion.isPresent
import java.time.ZonedDateTime

data class VehicleSearchParamModel(
	var visibilitySearched: Boolean? = null,
	var availabilitySearched: Boolean? = null,
	var presenceSearched: Boolean? = null,
	var dateTimeSearched: ZonedDateTime? = null,
) {
	var textSearched: String? = null

	constructor(
		textSearched: String? = null,
		visibilitySearched: Boolean? = null,
		statusSearched: PresenceStatusEnum? = null,
		dateTimeSearched: ZonedDateTime?,
	): this(
		visibilitySearched = visibilitySearched,
		availabilitySearched = statusSearched.isAvailable(),
		presenceSearched = statusSearched.isPresent(),
		dateTimeSearched = dateTimeSearched,
	) {
		this.textSearched = if (textSearched.isNullOrBlank()) null else textSearched
	}
}
