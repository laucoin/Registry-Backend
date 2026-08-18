package fr.laucoin.registry.backend.domain.model

import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum
import java.time.ZonedDateTime

data class VehicleSearchParamModel(
	var visibilitySearched: Boolean? = null,
	var availabilitySearched: Boolean? = null,
	var presenceStatusSearched: PresenceStatusEnum? = null,
	var warnedSearched: Boolean? = null,
	var dateTimeSearched: ZonedDateTime? = null,
) {
	var textSearched: String? = null

	constructor(
		textSearched: String? = null,
		visibilitySearched: Boolean? = null,
		statusSearched: PresenceStatusEnum? = null,
		dateTimeSearched: ZonedDateTime?,
		warnedSearched: Boolean? = null,
	) : this(
		visibilitySearched = visibilitySearched,
		presenceStatusSearched = statusSearched,
		warnedSearched = warnedSearched,
		dateTimeSearched = dateTimeSearched,
	) {
		this.textSearched = if (textSearched.isNullOrBlank()) null else textSearched
	}
}
