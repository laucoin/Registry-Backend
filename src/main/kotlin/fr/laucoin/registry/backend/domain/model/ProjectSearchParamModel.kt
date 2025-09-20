package fr.laucoin.registry.backend.domain.model

import java.time.ZonedDateTime

data class ProjectSearchParamModel(
	var visibilitySearched: Boolean? = null,
	var dateTimeSearched: ZonedDateTime? = null,
) {
	var textSearched: String? = null

	constructor(
		textSearched: String? = null,
		visibilitySearched: Boolean? = null,
		dateTimeSearched: ZonedDateTime? = null,
	): this(visibilitySearched, dateTimeSearched) {
		this.textSearched = if (textSearched.isNullOrBlank()) null else textSearched
	}
}

