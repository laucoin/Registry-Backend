package fr.laucoin.registry.backend.domain.model

import java.time.ZonedDateTime

data class CommunicationSearchParamModel(
	var visibilitySearched: Boolean? = null,
	var startDateTimeSearched: ZonedDateTime? = null,
	var endDateTimeSearched: ZonedDateTime? = null,
) {
	var textSearched: String? = null

	constructor(
		textSearched: String? = null,
		visibilitySearched: Boolean? = null,
		startDateTimeSearched: ZonedDateTime? = null,
		endDateTimeSearched: ZonedDateTime? = null,
	): this(visibilitySearched, startDateTimeSearched, endDateTimeSearched) {
		this.textSearched = if (textSearched.isNullOrBlank()) null else textSearched
	}
}
